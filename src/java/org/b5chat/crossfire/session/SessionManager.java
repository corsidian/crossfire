/**
 * $RCSfile$
 * $Revision: 3170 $
 * $Date: 2005-12-07 14:00:58 -0300 (Wed, 07 Dec 2005) $
 *
 * Copyright (C) 2005-2008 B5Chat Community. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.b5chat.crossfire.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


import org.b5chat.crossfire.BasicStreamIDFactory;
import org.b5chat.crossfire.IConnection;
import org.b5chat.crossfire.IConnectionCloseListener;
import org.b5chat.crossfire.IStreamId;
import org.b5chat.crossfire.IStreamIdFactory;
import org.b5chat.crossfire.PacketException;
import org.b5chat.crossfire.auth.AuthToken;
import org.b5chat.crossfire.core.container.BasicModule;
import org.b5chat.crossfire.offline.OfflineMessage;
import org.b5chat.crossfire.offline.OfflineMessageStore;
import org.b5chat.crossfire.route.IPacketRouter;
import org.b5chat.crossfire.route.IRoutingTable;
import org.b5chat.crossfire.server.XmppServer;
import org.b5chat.crossfire.user.UserManager;
import org.b5chat.util.Globals;
import org.b5chat.util.LocaleUtils;
import org.b5chat.util.cache.Cache;
import org.b5chat.util.cache.CacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

/**
 * Manages the sessions associated with an account. The information
 * maintained by the ISession manager is entirely transient and does
 * not need to be preserved between server restarts.
 *
 * @author Derek DeMoro
 */
public class SessionManager extends BasicModule {

	private static final Logger Log = LoggerFactory.getLogger(SessionManager.class);

    public static final String C2S_INFO_CACHE_NAME = "Client ISession Info Cache";

    public static final int NEVER_KICK = -1;

    private XmppServer server;
    private IPacketRouter router;
    private String serverName;
    private JID serverAddress;
    private UserManager userManager;
    private int conflictLimit;

    /**
     * Counter of user connections. A connection is counted just after it was created and not
     * after the user became available. This counter only considers sessions local to this JVM.
     * That means that when running inside of a cluster you will need to add up this counter
     * for each cluster node. 
     */
    private final AtomicInteger connectionsCounter = new AtomicInteger(0);

    /**
     * Cache (unlimited, never expire) that holds information about client sessions (as soon as
     * a resource has been bound). The cache is used by Remote sessions to avoid generating big
     * number of remote calls.
     * Key: full JID, Value: ClientSessionInfo
     */
    private Cache<String, ClientSessionInfo> sessionInfoCache;

    private ClientSessionListener clientSessionListener = new ClientSessionListener();

    /**
     * Local session manager responsible for keeping sessions connected to this JVM that are not
     * present in the routing table. 
     */
    private LocalSessionManager localSessionManager;
    /**
     * <p>ISession manager must maintain the routing table as sessions are added and
     * removed.</p>
     */
    private IRoutingTable routingTable;

    private IStreamIdFactory streamIDFactory;

    /**
     * Returns the instance of <CODE>SessionManagerImpl</CODE> being used by the XmppServer.
     *
     * @return the instance of <CODE>SessionManagerImpl</CODE> being used by the XmppServer.
     */
    public static SessionManager getInstance() {
        return XmppServer.getInstance().getSessionManager();
    }

    public SessionManager() {
        super("ISession Manager");
        streamIDFactory = new BasicStreamIDFactory();
        localSessionManager = new LocalSessionManager();
        conflictLimit = Globals.getIntProperty("xmpp.session.conflict-limit", 0);
    }

    /**
     * Returns a randomly created ID to be used in a stream element.
     *
     * @return a randomly created ID to be used in a stream element.
     */
    public IStreamId nextStreamID() {
        return streamIDFactory.createStreamID();
    }

    /**
     * Creates a new <tt>IClientSession</tt>. The new Client session will have a newly created
     * stream ID.
     *
     * @param conn the connection to create the session from.
     * @return a newly created session.
     */
    public LocalClientSession createClientSession(IConnection conn) {
        return createClientSession(conn, nextStreamID());
    }

    /**
     * Creates a new <tt>IClientSession</tt> with the specified streamID.
     *
     * @param conn the connection to create the session from.
     * @param id the streamID to use for the new session.
     * @return a newly created session.
     */
    public LocalClientSession createClientSession(IConnection conn, IStreamId id) {
        if (serverName == null) {
            throw new IllegalStateException("Server not initialized");
        }
        LocalClientSession session = new LocalClientSession(serverName, conn, id);
        conn.init(session);
        // Register to receive close notification on this session so we can
        // remove  and also send an unavailable presence if it wasn't
        // sent before
        conn.registerCloseListener(clientSessionListener, session);

        // Add to pre-authenticated sessions.
        localSessionManager.getPreAuthenticatedSessions().put(session.getAddress().getResource(), session);
        // Increment the counter of user sessions
        connectionsCounter.incrementAndGet();
        return session;
    }

    /**
     * Add a new session to be managed. The session has been authenticated and resource
     * binding has been done.
     *
     * @param session the session that was authenticated.
     */
    public void addSession(LocalClientSession session) {
        // Remove the pre-Authenticated session but remember to use the temporary ID as the key
        localSessionManager.getPreAuthenticatedSessions().remove(session.getStreamID().toString());
        // Add session to the routing table (routing table will know session is not available yet)
        routingTable.addClientRoute(session.getAddress(), session);
        SessionEventDispatcher.EventType event = session.getAuthToken().isAnonymous() ?
                SessionEventDispatcher.EventType.anonymous_session_created :
                SessionEventDispatcher.EventType.session_created;
        // Fire session created event.
        SessionEventDispatcher.dispatchEvent(session, event);
    }

    /**
     * Notification message sent when a client sent an available presence for the session. Making
     * the session available means that the session is now eligible for receiving messages from
     * other clients. Sessions whose presence is not available may only receive packets (IQ packets)
     * from the server. Therefore, an unavailable session remains invisible to other clients.
     *
     * @param session the session that receieved an available presence.
     */
    public void sessionAvailable(LocalClientSession session) {
        if (session.getAuthToken().isAnonymous()) {
            // Anonymous session always have resources so we only need to add one route. That is
            // the route to the anonymous session
            routingTable.addClientRoute(session.getAddress(), session);
        }
        else {
            // A non-anonymous session is now available
            // Add route to the new session
            routingTable.addClientRoute(session.getAddress(), session);
            // Broadcast presence between the user's resources
            broadcastPresenceOfOtherResource(session);
        }
    }

    /**
     * Sends the presences of other connected resources to the resource that just connected.
     * 
     * @param session the newly created session.
     */
    private void broadcastPresenceOfOtherResource(LocalClientSession session) {
        Presence presence;
        // Get list of sessions of the same user
        JID searchJID = new JID(session.getAddress().getNode(), session.getAddress().getDomain(), null);
        List<JID> addresses = routingTable.getRoutes(searchJID, null);
        for (JID address : addresses) {
            if (address.equals(session.getAddress())) {
                continue;
            }
            // Send the presence of an existing session to the session that has just changed
            // the presence
            IClientSession userSession = routingTable.getClientRoute(address);
            presence = userSession.getPresence().createCopy();
            presence.setTo(session.getAddress());
            session.process(presence);
        }
    }

    /**
     * Broadcasts presence updates from the originating user's resource to any of the user's
     * existing available resources (if any).
     *
     * @param originatingResource the full JID of the session that sent the presence update.
     * @param presence the presence.
     */
    public void broadcastPresenceToOtherResources(JID originatingResource, Presence presence) {
        // Get list of sessions of the same user
        JID searchJID = new JID(originatingResource.getNode(), originatingResource.getDomain(), null);
        List<JID> addresses = routingTable.getRoutes(searchJID, null);
        for (JID address : addresses) {
            if (address.equals(originatingResource)) {
                continue;
            }
            // Send the presence of the session whose presence has changed to
            // this other user's session
            presence.setTo(address);
            routingTable.routePacket(address, presence, false);
        }
    }

    /**
     * Notification message sent when a client sent an unavailable presence for the session. Making
     * the session unavailable means that the session is not eligible for receiving messages from
     * other clients.
     *
     * @param session the session that received an unavailable presence.
     */
    public void sessionUnavailable(LocalClientSession session) {
        if (session.getAddress() != null && routingTable != null &&
                session.getAddress().toBareJID().trim().length() != 0) {
            // Update route to unavailable session (anonymous or not)
            routingTable.addClientRoute(session.getAddress(), session);
        }
    }

    /**
     * Change the priority of a session, that was already available, associated with the sender.
     *
     * @param session   The session whose presence priority has been modified
     * @param oldPriority The old priority for the session
     */
    public void changePriority(LocalClientSession session, int oldPriority) {
        if (session.getAuthToken().isAnonymous()) {
            // Do nothing if the session belongs to an anonymous user
            return;
        }
        int newPriority = session.getPresence().getPriority();
        if (newPriority < 0 || oldPriority >= 0) {
            // Do nothing if new presence priority is not positive and old presence negative
            return;
        }

        // Check presence's priority of other available resources
        JID searchJID = new JID(session.getAddress().toBareJID());
        for (JID address : routingTable.getRoutes(searchJID, null)) {
            if (address.equals(session.getAddress())) {
                continue;
            }
            IClientSession otherSession = routingTable.getClientRoute(address);
            if (otherSession.getPresence().getPriority() >= 0) {
                return;
            }
        }

        // User sessions had negative presence before this change so deliver messages
        if (session.canFloodOfflineMessages()) {
            OfflineMessageStore messageStore = server.getOfflineMessageStore();
            Collection<OfflineMessage> messages = messageStore.getMessages(session.getAuthToken().getUsername(), true);
            for (Message message : messages) {
                session.process(message);
            }
        }
    }

    public boolean isAnonymousRoute(String username) {
        // JID's node and resource are the same for anonymous sessions
        return isAnonymousRoute(new JID(username, serverName, username, true));
    }

    public boolean isAnonymousRoute(JID address) {
        // JID's node and resource are the same for anonymous sessions
        return routingTable.isAnonymousRoute(address);
    }

    public boolean isActiveRoute(String username, String resource) {
        boolean hasRoute = false;
        ISession session = routingTable.getClientRoute(new JID(username, serverName, resource));
        // Makes sure the session is still active
        if (session != null && !session.isClosed()) {
            hasRoute = session.validate();
        }

        return hasRoute;
    }

    /**
     * Returns the session responsible for this JID data. The returned ISession may have never sent
     * an available presence (thus not have a route) or could be a ISession that hasn't
     * authenticated yet (i.e. preAuthenticatedSessions).
     *
     * @param from the sender of the packet.
     * @return the <code>ISession</code> associated with the JID.
     */
    public IClientSession getSession(JID from) {
        // Return null if the JID is null or belongs to a foreign server. If the server is
        // shutting down then serverName will be null so answer null too in this case.
        if (from == null || serverName == null || !serverName.equals(from.getDomain())) {
            return null;
        }

        // Initially Check preAuthenticated Sessions
        if (from.getResource() != null) {
            IClientSession session = localSessionManager.getPreAuthenticatedSessions().get(from.getResource());
            if (session != null) {
                return session;
            }
        }

        if (from.getResource() == null || from.getNode() == null) {
            return null;
        }

        return routingTable.getClientRoute(from);
    }

    /**
     * Returns a list that contains all authenticated client sessions connected to the server.
     * The list contains sessions of anonymous and non-anonymous users.
     *
     * @return a list that contains all client sessions connected to the server.
     */
    public Collection<IClientSession> getSessions() {
        return routingTable.getClientsRoutes(false);
    }


    public Collection<IClientSession> getSessions(SessionResultFilter filter) {
        List<IClientSession> results = new ArrayList<IClientSession>();
        if (filter != null) {
            // Grab all the matching sessions
            results.addAll(getSessions());

            // Now we have a copy of the references so we can spend some time
            // doing the rest of the filtering without locking out session access
            // so let's iterate and filter each session one by one
            List<IClientSession> filteredResults = new ArrayList<IClientSession>();
            for (IClientSession session : results) {
                // Now filter on creation date if needed
                filteredResults.add(session);
            }

            // Sort list.
            Collections.sort(filteredResults, filter.getSortComparator());

            int maxResults = filter.getNumResults();
            if (maxResults == SessionResultFilter.NO_RESULT_LIMIT) {
                maxResults = filteredResults.size();
            }

            // Now generate the final list. I believe it's faster to to build up a new
            // list than it is to remove items from head and tail of the sorted tree
            List<IClientSession> finalResults = new ArrayList<IClientSession>();
            int startIndex = filter.getStartIndex();
            Iterator<IClientSession> sortedIter = filteredResults.iterator();
            for (int i = 0; sortedIter.hasNext() && finalResults.size() < maxResults; i++) {
                IClientSession result = sortedIter.next();
                if (i >= startIndex) {
                    finalResults.add(result);
                }
            }
            return finalResults;
        }
        return results;
    }

    public Collection<IClientSession> getSessions(String username) {
        List<IClientSession> sessionList = new ArrayList<IClientSession>();
        if (username != null) {
            List<JID> addresses = routingTable.getRoutes(new JID(username, serverName, null, true), null);
            for (JID address : addresses) {
                sessionList.add(routingTable.getClientRoute(address));
            }
        }
        return sessionList;
    }

    /**
     * Returns number of client sessions that are connected to the server. Sessions that
     * are authenticated and not authenticated will be included
     *
     * @param onlyLocal true if only sessions connected to this JVM will be considered. Otherwise count cluster wise.
     * @return number of client sessions that are connected to the server.
     */
    public int getConnectionsCount(boolean onlyLocal) {
        int total = connectionsCounter.get();
        return total;
    }

    /**
     * Returns number of client sessions that are authenticated with the server. This includes
     * anonymous and non-anoymous users.
     *
     * @param onlyLocal true if only sessions connected to this JVM will be considered. Otherwise count cluster wise.
     * @return number of client sessions that are authenticated with the server.
     */
    public int getUserSessionsCount(boolean onlyLocal) {
        int total = routingTable.getClientsRoutes(true).size();
        return total;
    }

    /**
     * Returns the number of sessions for a user that are available. For the count
     * of all sessions for the user, including sessions that are just starting
     * or closed.
     *
     * @see #getConnectionsCount(boolean)
     * @param username the user.
     * @return number of available sessions for a user.
     */
    public int getActiveSessionCount(String username) {
        return routingTable.getRoutes(new JID(username, serverName, null, true), null).size();
    }

    public int getSessionCount(String username) {
        // TODO Count ALL sessions not only available
        return routingTable.getRoutes(new JID(username, serverName, null, true), null).size();
    }

    /**
     * Broadcasts the given data to all connected sessions. Excellent
     * for server administration messages.
     *
     * @param packet the packet to be broadcast.
     */
    public void broadcast(Message packet) {
        routingTable.broadcastPacket(packet, false);
    }

    /**
     * Broadcasts the given data to all connected sessions for a particular
     * user. Excellent for updating all connected resources for users such as
     * roster pushes.
     *
     * @param username the user to send the boradcast to.
     * @param packet the packet to be broadcast.
     * @throws PacketException if a packet exception occurs.
     */
    public void userBroadcast(String username, Packet packet) throws PacketException {
        // TODO broadcast to ALL sessions of the user and not only available
        for (JID address : routingTable.getRoutes(new JID(username, serverName, null), null)) {
            packet.setTo(address);
            routingTable.routePacket(address, packet, true);
        }
    }

    /**
     * Removes a session.
     *
     * @param session the session.
     * @return true if the requested session was successfully removed.
     */
    public boolean removeSession(LocalClientSession session) {
        // Do nothing if session is null or if the server is shutting down. Note: When the server
        // is shutting down the serverName will be null.
        if (session == null || serverName == null) {
            return false;
        }

        AuthToken authToken = session.getAuthToken();
        // Consider session anonymous (for this matter) if we are closing a session that never authenticated
        boolean anonymous = authToken == null || authToken.isAnonymous();
        return removeSession(session, session.getAddress(), anonymous, false);
    }

    /**
     * Removes a session.
     *
     * @param session the session or null when session is derived from fullJID.
     * @param fullJID the address of the session.
     * @param anonymous true if the authenticated user is anonymous.
     * @param forceUnavailable true if an unavailable presence must be created and routed.
     * @return true if the requested session was successfully removed.
     */
    public boolean removeSession(IClientSession session, JID fullJID, boolean anonymous, boolean forceUnavailable) {
        // Do nothing if server is shutting down. Note: When the server
        // is shutting down the serverName will be null.
        if (serverName == null) {
            return false;
        }

        if (session == null) {
            session = getSession(fullJID);
        }

        // Remove route to the removed session (anonymous or not)
        boolean removed = routingTable.removeClientRoute(fullJID);

        if (removed) {
            // Fire session event.
            if (anonymous) {
                SessionEventDispatcher
                        .dispatchEvent(session, SessionEventDispatcher.EventType.anonymous_session_destroyed);
            }
            else {
                SessionEventDispatcher.dispatchEvent(session, SessionEventDispatcher.EventType.session_destroyed);

            }
        }

        // Remove the session from the pre-Authenticated sessions list (if present)
        boolean preauth_removed =
                localSessionManager.getPreAuthenticatedSessions().remove(fullJID.getResource()) != null;
        // If the user is still available then send an unavailable presence
        if (forceUnavailable || session.getPresence().isAvailable()) {
            Presence offline = new Presence();
            offline.setFrom(fullJID);
            offline.setTo(new JID(null, serverName, null, true));
            offline.setType(Presence.Type.unavailable);
            router.route(offline);
        }

        // Stop tracking information about the session and share it with other cluster nodes
        sessionInfoCache.remove(fullJID.toString());

        if (removed || preauth_removed) {
            // Decrement the counter of user sessions
            connectionsCounter.decrementAndGet();
            return true;
        }
        return false;
    }

    public int getConflictKickLimit() {
        return conflictLimit;
    }

    /**
     * Returns the temporary keys used by the sessions that has not been authenticated yet. This
     * is an utility method useful for debugging situations.
     *
     * @return the temporary keys used by the sessions that has not been authenticated yet.
     */
    public Collection<String> getPreAuthenticatedKeys() {
        return localSessionManager.getPreAuthenticatedSessions().keySet();
    }

    /**
     * Returns true if the specified address belongs to a preauthenticated session. Preauthenticated
     * sessions are only available to the local cluster node when running inside of a cluster.
     *
     * @param address the address of the session.
     * @return true if the specified address belongs to a preauthenticated session.
     */
    public boolean isPreAuthenticatedSession(JID address) {
        return serverName.equals(address.getDomain()) &&
                localSessionManager.getPreAuthenticatedSessions().containsKey(address.getResource());
    }

    public void setConflictKickLimit(int limit) {
        conflictLimit = limit;
        Globals.setProperty("xmpp.session.conflict-limit", Integer.toString(conflictLimit));
    }

    private class ClientSessionListener implements IConnectionCloseListener {
        /**
         * Handle a session that just closed.
         *
         * @param handback The session that just closed
         */
        public void onConnectionClose(Object handback) {
            try {
                LocalClientSession session = (LocalClientSession) handback;
                try {
                    if ((session.getPresence().isAvailable() || !session.wasAvailable()) &&
                            routingTable.hasClientRoute(session.getAddress())) {
                        // Send an unavailable presence to the user's subscribers
                        // Note: This gives us a chance to send an unavailable presence to the
                        // entities that the user sent directed presences
                        Presence presence = new Presence();
                        presence.setType(Presence.Type.unavailable);
                        presence.setFrom(session.getAddress());
                        router.route(presence);
                    }
                }
                finally {
                    // Remove the session
                    removeSession(session);
                }
            }
            catch (Exception e) {
                // Can't do anything about this problem...
                Log.error(LocaleUtils.getLocalizedString("admin.error.close"), e);
            }
        }
    }

    @Override
	public void initialize(XmppServer server) {
        super.initialize(server);
        this.server = server;
        router = server.getPacketRouter();
        userManager = server.getUserManager();
        routingTable = server.getRoutingTable();
        serverName = server.getServerInfo().getXMPPDomain();
        serverAddress = new JID(serverName);

        streamIDFactory = new BasicStreamIDFactory();

        String conflictLimitProp = Globals.getProperty("xmpp.session.conflict-limit");
        if (conflictLimitProp == null) {
            conflictLimit = 0;
            Globals.setProperty("xmpp.session.conflict-limit", Integer.toString(conflictLimit));
        }
        else {
            try {
                conflictLimit = Integer.parseInt(conflictLimitProp);
            }
            catch (NumberFormatException e) {
                conflictLimit = 0;
                Globals.setProperty("xmpp.session.conflict-limit", Integer.toString(conflictLimit));
            }
        }

        // Initialize caches.
        sessionInfoCache = CacheFactory.createCache(C2S_INFO_CACHE_NAME);
    }


    /**
     * Sends a message with a given subject and body to all the active user sessions in the server.
     *
     * @param subject the subject to broadcast.
     * @param body    the body to broadcast.
     */
    public void sendServerMessage(String subject, String body) {
        sendServerMessage(null, subject, body);
    }

    /**
     * Sends a message with a given subject and body to one or more user sessions related to the
     * specified address. If address is null or the address's node is null then the message will be
     * sent to all the user sessions. But if the address includes a node but no resource then
     * the message will be sent to all the user sessions of the requeted user (defined by the node).
     * Finally, if the address is a full JID then the message will be sent to the session associated
     * to the full JID. If no session is found then the message is not sent.
     *
     * @param address the address that defines the sessions that will receive the message.
     * @param subject the subject to broadcast.
     * @param body    the body to broadcast.
     */
    public void sendServerMessage(JID address, String subject, String body) {
        Message packet = createServerMessage(subject, body);
        if (address == null || address.getNode() == null || !userManager.isRegisteredUser(address)) {
            broadcast(packet);
        }
        else if (address.getResource() == null || address.getResource().length() < 1) {
            userBroadcast(address.getNode(), packet);
        }
        else {
            routingTable.routePacket(address, packet, true);
        }
    }

    private Message createServerMessage(String subject, String body) {
        Message message = new Message();
        message.setFrom(serverAddress);
        if (subject != null) {
            message.setSubject(subject);
        }
        message.setBody(body);
        return message;
    }

    @Override
	public void start() throws IllegalStateException {
        super.start();
        localSessionManager.start();
    }

    @Override
	public void stop() {
        Log.debug("SessionManager: Stopping server");
        // Stop threads that are sending packets to remote servers
        if (Globals.getBooleanProperty("shutdownMessage.enabled")) {
            sendServerMessage(null, LocaleUtils.getLocalizedString("admin.shutdown.now"));
        }
        localSessionManager.stop();
        serverName = null;
    }

    /******************************************************
     * Clean up code
     *****************************************************/

    public Cache<String, ClientSessionInfo> getSessionInfoCache() {
        return sessionInfoCache;
    }
}
