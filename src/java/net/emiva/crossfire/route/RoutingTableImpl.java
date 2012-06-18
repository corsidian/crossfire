/**
 * $RCSfile: RoutingTableImpl.java,v $
 * $Revision: 3138 $
 * $Date: 2005-12-01 02:13:26 -0300 (Thu, 01 Dec 2005) $
 *
 * Copyright (C) 2005-2008 EMIVA Community. All rights reserved.
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

package net.emiva.crossfire.route;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import net.emiva.crossfire.PacketException;
import net.emiva.crossfire.auth.UnauthorizedException;
import net.emiva.crossfire.core.cluster.IClusterEventListener;
import net.emiva.crossfire.core.cluster.ClusterManager;
import net.emiva.crossfire.core.container.BasicModule;
import net.emiva.crossfire.presence.PresenceRouter;
import net.emiva.crossfire.presence.PresenceUpdateHandler;
import net.emiva.crossfire.server.XmppServer;
import net.emiva.crossfire.session.IClientSession;
import net.emiva.crossfire.session.LocalClientSession;
import net.emiva.crossfire.session.IRemoteSessionLocator;
import net.emiva.util.ConcurrentHashSet;
import net.emiva.util.Globals;
import net.emiva.util.cache.Cache;
import net.emiva.util.cache.CacheFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

/**
 * Routing table that stores routes to client sessions, outgoing server sessions
 * and components. As soon as a user authenticates with the server its client session
 * will be added to the routing table. Whenever the client session becomes available
 * or unavailable the routing table will be updated too.<p>
 *
 * When running inside of a cluster the routing table will also keep references to routes
 * hosted in other cluster nodes. A {@link IRemotePacketRouter} will be use to route packets
 * to routes hosted in other cluster nodes.<p>
 *
 * Failure to route a packet will end up sending {@link IQRouter#routingFailed(JID, Packet)},
 * {@link MessageRouter#routingFailed(JID, Packet)} or {@link PresenceRouter#routingFailed(JID, Packet)}
 * depending on the packet type that tried to be sent.
 *
 * @author Gaston Dombiak
 */
public class RoutingTableImpl extends BasicModule implements IRoutingTable, IClusterEventListener {

	private static final Logger Log = LoggerFactory.getLogger(RoutingTableImpl.class);
	
    public static final String C2S_CACHE_NAME = "Routing Users Cache";
    public static final String ANONYMOUS_C2S_CACHE_NAME = "Routing AnonymousUsers Cache";
    public static final String COMPONENT_CACHE_NAME = "Routing Components Cache";

    /**
     * Cache (unlimited, never expire) that holds sessions of user that have authenticated with the server.
     * Key: full JID, Value: {nodeID, available/unavailable}
     */
    private Cache<String, ClientRoute> usersCache;
    /**
     * Cache (unlimited, never expire) that holds sessions of anonymous user that have authenticated with the server.
     * Key: full JID, Value: {nodeID, available/unavailable}
     */
    private Cache<String, ClientRoute> anonymousUsersCache;
    /**
     * Cache (unlimited, never expire) that holds list of connected resources of authenticated users
     * (includes anonymous).
     * Key: bare JID, Value: list of full JIDs of the user
     */
    private Cache<String, Collection<String>> usersSessions;

    private String serverName;
    private XmppServer server;
    private LocalRoutingTable localRoutingTable;
    private IRemotePacketRouter remotePacketRouter;
    private IQRouter iqRouter;
    private MessageRouter messageRouter;
    private PresenceRouter presenceRouter;
    private PresenceUpdateHandler presenceUpdateHandler;

    public RoutingTableImpl() {
        super("Routing table");
        usersCache = CacheFactory.createCache(C2S_CACHE_NAME);
        anonymousUsersCache = CacheFactory.createCache(ANONYMOUS_C2S_CACHE_NAME);
        usersSessions = CacheFactory.createCache("Routing User Sessions");
        localRoutingTable = new LocalRoutingTable();
    }

    public boolean addClientRoute(JID route, LocalClientSession destination) {
        boolean added;
        boolean available = destination.getPresence().isAvailable();
        localRoutingTable.addRoute(route.toString(), destination);
        if (destination.getAuthToken().isAnonymous()) {
            Lock lockAn = CacheFactory.getLock(route.toString(), anonymousUsersCache);
            try {
                lockAn.lock();
                added = anonymousUsersCache.put(route.toString(), new ClientRoute(server.getNodeID(), available)) ==
                        null;
            }
            finally {
                lockAn.unlock();
            }
            // Add the session to the list of user sessions
            if (route.getResource() != null && (!available || added)) {
                Lock lock = CacheFactory.getLock(route.toBareJID(), usersSessions);
                try {
                    lock.lock();
                    usersSessions.put(route.toBareJID(), Arrays.asList(route.toString()));
                }
                finally {
                    lock.unlock();
                }
            }
        }
        else {
            Lock lockU = CacheFactory.getLock(route.toString(), usersCache);
            try {
                lockU.lock();
                added = usersCache.put(route.toString(), new ClientRoute(server.getNodeID(), available)) == null;
            }
            finally {
                lockU.unlock();
            }
            // Add the session to the list of user sessions
            if (route.getResource() != null && (!available || added)) {
                Lock lock = CacheFactory.getLock(route.toBareJID(), usersSessions);
                try {
                    lock.lock();
                    Collection<String> jids = usersSessions.get(route.toBareJID());
                    if (jids == null) {
                        // Optimization - use different class depending on current setup
                        if (ClusterManager.isClusteringStarted()) {
                            jids = new HashSet<String>();
                        }
                        else {
                            jids = new ConcurrentHashSet<String>();
                        }
                    }
                    jids.add(route.toString());
                    usersSessions.put(route.toBareJID(), jids);
                }
                finally {
                    lock.unlock();
                }
            }
        }
        return added;
    }

    public void broadcastPacket(Message packet, boolean onlyLocal) {
        // Send the message to client sessions connected to this JVM
        for(IClientSession session : localRoutingTable.getClientRoutes()) {
            session.process(packet);
        }

        // Check if we need to broadcast the message to client sessions connected to remote cluter nodes
        if (!onlyLocal && remotePacketRouter != null) {
            remotePacketRouter.broadcastPacket(packet);
        }
    }

    /*
     * (non-Javadoc)
     * @see net.emiva.crossfire.RoutingTable#routePacket(org.xmpp.packet.JID, org.xmpp.packet.Packet, boolean)
     * 
     * @param jid the recipient of the packet to route.
     * @param packet the packet to route.
     * @param fromServer true if the packet was created by the server. This packets should
     *        always be delivered
     * @throws PacketException thrown if the packet is malformed (results in the sender's
     *      session being shutdown).
     */
    public void routePacket(JID jid, Packet packet, boolean fromServer) throws PacketException {
        boolean routed = false;
        if (serverName.equals(jid.getDomain())) {
        	// Packet sent to our domain.
            routed = routeToLocalDomain(jid, packet, fromServer);
        }

        if (!routed) {
            if (Log.isDebugEnabled()) {
                Log.debug("RoutingTableImpl: Failed to route packet to JID: {} packet: {}", jid, packet.toXML());
            }
            if (packet instanceof IQ) {
                iqRouter.routingFailed(jid, packet);
            }
            else if (packet instanceof Message) {
                messageRouter.routingFailed(jid, packet);
            }
            else if (packet instanceof Presence) {
                presenceRouter.routingFailed(jid, packet);
            }
        }
    }

	/**
	 * Routes packets that are sent to the XMPP domain itself (excluding subdomains).
	 * 
	 * @param jid
	 *            the recipient of the packet to route.
	 * @param packet
	 *            the packet to route.
	 * @param fromServer
	 *            true if the packet was created by the server. This packets
	 *            should always be delivered
	 * @throws PacketException
	 *             thrown if the packet is malformed (results in the sender's
	 *             session being shutdown).
	 * @return <tt>true</tt> if the packet was routed successfully,
	 *         <tt>false</tt> otherwise.
	 */
	private boolean routeToLocalDomain(JID jid, Packet packet,
			boolean fromServer) {
		boolean routed = false;
		if (jid.getResource() == null) {
		    // Packet sent to a bare JID of a user
		    if (packet instanceof Message) {
		        // Find best route of local user
		        routed = routeToBareJID(jid, (Message) packet);
		    }
		    else {
		        throw new PacketException("Cannot route packet of type IQ or Presence to bare JID: " + packet.toXML());
		    }
		}
		else {
		    // Packet sent to local user (full JID)
		    ClientRoute clientRoute = usersCache.get(jid.toString());
		    if (clientRoute == null) {
		        clientRoute = anonymousUsersCache.get(jid.toString());
		    }
		    if (clientRoute != null) {
		        if (!clientRoute.isAvailable() && routeOnlyAvailable(packet, fromServer) &&
		                !presenceUpdateHandler.hasDirectPresence(packet.getTo(), packet.getFrom())) {
		        	Log.debug("Unable to route packet. Packet should only be sent to available sessions and the route is not available. {} ", packet.toXML());
		            routed = false;
		        }
		        else {
		            if (server.getNodeID().equals(clientRoute.getNodeID())) {
		                // This is a route to a local user hosted in this node
		                try {
		                    localRoutingTable.getRoute(jid.toString()).process(packet);
		                    routed = true;
		                } catch (UnauthorizedException e) {
		                    Log.error("Unable to route packet " + packet.toXML(), e);
		                }
		            }
		            else {
		                // This is a route to a local user hosted in other node
		                if (remotePacketRouter != null) {
		                    routed = remotePacketRouter
		                            .routePacket(clientRoute.getNodeID().toByteArray(), jid, packet);
		                }
		            }
		        }
		    }
		}
		return routed;
	}
	
    /**
     * Returns true if the specified packet must only be route to available client sessions.
     *
     * @param packet the packet to route.
     * @param fromServer true if the packet was created by the server.
     * @return true if the specified packet must only be route to available client sessions.
     */
    private boolean routeOnlyAvailable(Packet packet, boolean fromServer) {
        if (fromServer) {
            // Packets created by the server (no matter their FROM value) must always be delivered no
            // matter the available presence of the user
            return false;
        }
        boolean onlyAvailable = true;
        JID from = packet.getFrom();
        boolean hasSender = from != null;
        if (packet instanceof IQ) {
            onlyAvailable = hasSender && !(serverName.equals(from.getDomain()) && from.getResource() == null);
        }
        else if (packet instanceof Message || packet instanceof Presence) {
            onlyAvailable = !hasSender ||
                    (!serverName.equals(from.toString()));
        }
        return onlyAvailable;
    }

    /**
     * Deliver the message sent to the bare JID of a local user to the best connected resource. If the
     * target user is not online then messages will be stored offline according to the offline strategy.
     * However, if the user is connected from only one resource then the message will be delivered to
     * that resource. In the case that the user is connected from many resources the logic will be the
     * following:
     * <ol>
     *  <li>Select resources with highest priority</li>
     *  <li>Select resources with highest show value (chat, available, away, xa, dnd)</li>
     *  <li>Select resource with most recent activity</li>
     * </ol>
     *
     * Admins can override the above logic and just send the message to all connected resources
     * with highest priority by setting the system property <tt>route.all-resources</tt> to
     * <tt>true</tt>.
     *
     * @param recipientJID the bare JID of the target local user.
     * @param packet the message to send.
     * @return true if at least one target session was found
     */
    private boolean routeToBareJID(JID recipientJID, Message packet) {
        List<IClientSession> sessions = new ArrayList<IClientSession>();
        // Get existing AVAILABLE sessions of this user or AVAILABLE to the sender of the packet
        for (JID address : getRoutes(recipientJID, packet.getFrom())) {
            IClientSession session = getClientRoute(address);
            if (session != null) {
                sessions.add(session);
            }
        }
        sessions = getHighestPrioritySessions(sessions);
        if (sessions.isEmpty()) {
            // No session is available so store offline
        	Log.debug("Unable to route packet. No session is available so store offline. {} ", packet.toXML());
            return false;
        }
        else if (sessions.size() == 1) {
            // Found only one session so deliver message
            sessions.get(0).process(packet);
        }
        else {
            // Many sessions have the highest priority (be smart now) :)
            if (!Globals.getBooleanProperty("route.all-resources", false)) {
                // Sort sessions by show value (e.g. away, xa)
                Collections.sort(sessions, new Comparator<IClientSession>() {

                    public int compare(IClientSession o1, IClientSession o2) {
                        int thisVal = getShowValue(o1);
                        int anotherVal = getShowValue(o2);
                        return (thisVal<anotherVal ? -1 : (thisVal==anotherVal ? 0 : 1));
                    }

                    /**
                     * Priorities are: chat, available, away, xa, dnd.
                     */
                    private int getShowValue(IClientSession session) {
                        Presence.Show show = session.getPresence().getShow();
                        if (show == Presence.Show.chat) {
                            return 1;
                        }
                        else if (show == null) {
                            return 2;
                        }
                        else if (show == Presence.Show.away) {
                            return 3;
                        }
                        else if (show == Presence.Show.xa) {
                            return 4;
                        }
                        else {
                            return 5;
                        }
                    }
                });

                // Get same sessions with same max show value
                List<IClientSession> targets = new ArrayList<IClientSession>();
                Presence.Show showFilter = sessions.get(0).getPresence().getShow();
                for (IClientSession session : sessions) {
                    if (session.getPresence().getShow() == showFilter) {
                        targets.add(session);
                    }
                    else {
                        break;
                    }
                }

                // Get session with most recent activity (and highest show value)
                Collections.sort(targets, new Comparator<IClientSession>() {
                    public int compare(IClientSession o1, IClientSession o2) {
                        return o2.getLastActiveDate().compareTo(o1.getLastActiveDate());
                    }
                });
                // Deliver stanza to session with highest priority, highest show value and most recent activity
                targets.get(0).process(packet);
            }
            else {
                // Deliver stanza to all connected resources with highest priority
                for (IClientSession session : sessions) {
                    session.process(packet);
                }
            }
        }
        return true;
    }

    /**
     * Returns the sessions that had the highest presence priority greater than zero.
     *
     * @param sessions the list of user sessions that filter and get the ones with highest priority.
     * @return the sessions that had the highest presence priority greater than zero or empty collection
     *         if all were negative.
     */
    private List<IClientSession> getHighestPrioritySessions(List<IClientSession> sessions) {
        int highest = Integer.MIN_VALUE;
        // Get the highest priority amongst the sessions
        for (IClientSession session : sessions) {
            int priority = session.getPresence().getPriority();
            if (priority >= 0 && priority > highest) {
                highest = priority;
            }
        }
        // Answer an empty collection if all have negative priority
        if (highest == Integer.MIN_VALUE) {
            return Collections.emptyList();
        }
        // Get sessions that have the highest priority
        List<IClientSession> answer = new ArrayList<IClientSession>(sessions.size());
        for (IClientSession session : sessions) {
            if (session.getPresence().getPriority() == highest) {
                answer.add(session);
            }
        }
        return answer;
    }

    public IClientSession getClientRoute(JID jid) {
        // Check if this session is hosted by this cluster node
        IClientSession session = (IClientSession) localRoutingTable.getRoute(jid.toString());
        if (session == null) {
            // The session is not in this JVM so assume remote
            IRemoteSessionLocator locator = server.getRemoteSessionLocator();
            if (locator != null) {
                // Check if the session is hosted by other cluster node
                ClientRoute route = usersCache.get(jid.toString());
                if (route == null) {
                    route = anonymousUsersCache.get(jid.toString());
                }
                if (route != null) {
                    session = locator.getClientSession(route.getNodeID().toByteArray(), jid);
                }
            }
        }
        return session;
    }

    public Collection<IClientSession> getClientsRoutes(boolean onlyLocal) {
        // Add sessions hosted by this cluster node
        Collection<IClientSession> sessions = new ArrayList<IClientSession>(localRoutingTable.getClientRoutes());
        if (!onlyLocal) {
            // Add sessions not hosted by this JVM
            IRemoteSessionLocator locator = server.getRemoteSessionLocator();
            if (locator != null) {
                // Add sessions of non-anonymous users hosted by other cluster nodes
                for (Map.Entry<String, ClientRoute> entry : usersCache.entrySet()) {
                    ClientRoute route = entry.getValue();
                    if (!server.getNodeID().equals(route.getNodeID())) {
                        sessions.add(locator.getClientSession(route.getNodeID().toByteArray(), new JID(entry.getKey())));
                    }
                }
                // Add sessions of anonymous users hosted by other cluster nodes
                for (Map.Entry<String, ClientRoute> entry : anonymousUsersCache.entrySet()) {
                    ClientRoute route = entry.getValue();
                    if (!server.getNodeID().equals(route.getNodeID())) {
                        sessions.add(locator.getClientSession(route.getNodeID().toByteArray(), new JID(entry.getKey())));
                    }
                }
            }
        }
        return sessions;
    }

    public boolean hasClientRoute(JID jid) {
        return usersCache.containsKey(jid.toString()) || isAnonymousRoute(jid);
    }

    public boolean isAnonymousRoute(JID jid) {
        return anonymousUsersCache.containsKey(jid.toString());
    }

    public boolean isLocalRoute(JID jid) {
        return localRoutingTable.isLocalRoute(jid);
    }

    public List<JID> getRoutes(JID route, JID requester) {
        List<JID> jids = new ArrayList<JID>();
        if (serverName.equals(route.getDomain())) {
            // Address belongs to local user
            if (route.getResource() != null) {
                // Address is a full JID of a user
                ClientRoute clientRoute = usersCache.get(route.toString());
                if (clientRoute == null) {
                    clientRoute = anonymousUsersCache.get(route.toString());
                }
                if (clientRoute != null &&
                        (clientRoute.isAvailable() || presenceUpdateHandler.hasDirectPresence(route, requester))) {
                    jids.add(route);
                }
            }
            else {
                // Address is a bare JID so return all AVAILABLE resources of user
                Collection<String> sessions = usersSessions.get(route.toBareJID());
                if (sessions != null) {
                    // Select only available sessions
                    for (String jid : sessions) {
                        ClientRoute clientRoute = usersCache.get(jid);
                        if (clientRoute == null) {
                            clientRoute = anonymousUsersCache.get(jid);
                        }
                        if (clientRoute != null && (clientRoute.isAvailable() ||
                                presenceUpdateHandler.hasDirectPresence(new JID(jid), requester))) {
                            jids.add(new JID(jid));
                        }
                    }
                }
            }
        }
        else {
            // Packet sent to remote server
            jids.add(route);
        }
        return jids;
    }

    public boolean removeClientRoute(JID route) {
        boolean anonymous = false;
        String address = route.toString();
        ClientRoute clientRoute = null;
        Lock lockU = CacheFactory.getLock(address, usersCache);
        try {
            lockU.lock();
            clientRoute = usersCache.remove(address);
        }
        finally {
            lockU.unlock();
        }
        if (clientRoute == null) {
            Lock lockA = CacheFactory.getLock(address, anonymousUsersCache);
            try {
                lockA.lock();
                clientRoute = anonymousUsersCache.remove(address);
                anonymous = true;
            }
            finally {
                lockA.unlock();
            }
        }
        if (clientRoute != null && route.getResource() != null) {
            Lock lock = CacheFactory.getLock(route.toBareJID(), usersSessions);
            try {
                lock.lock();
                if (anonymous) {
                    usersSessions.remove(route.toBareJID());
                }
                else {
                    Collection<String> jids = usersSessions.get(route.toBareJID());
                    if (jids != null) {
                        jids.remove(route.toString());
                        if (!jids.isEmpty()) {
                            usersSessions.put(route.toBareJID(), jids);
                        }
                        else {
                            usersSessions.remove(route.toBareJID());
                        }
                    }
                }
            }
            finally {
                lock.unlock();
            }
        }
        localRoutingTable.removeRoute(address);
        return clientRoute != null;
    }

    public void setRemotePacketRouter(IRemotePacketRouter remotePacketRouter) {
        this.remotePacketRouter = remotePacketRouter;
    }

    public IRemotePacketRouter getRemotePacketRouter() {
        return remotePacketRouter;
    }

    @Override
	public void initialize(XmppServer server) {
        super.initialize(server);
        this.server = server;
        serverName = server.getServerInfo().getXMPPDomain();
        iqRouter = server.getIQRouter();
        messageRouter = server.getMessageRouter();
        presenceRouter = server.getPresenceRouter();
        presenceUpdateHandler = server.getPresenceUpdateHandler();
        // Listen to cluster events
        ClusterManager.addListener(this);
    }

    @Override
	public void start() throws IllegalStateException {
        super.start();
        localRoutingTable.start();
    }

    @Override
	public void stop() {
        super.stop();
        localRoutingTable.stop();
    }

    public void joinedCluster() {
        restoreCacheContent();

        // Broadcast presence of local sessions to remote sessions when subscribed to presence
        // Probe presences of remote sessions when subscribed to presence of local session
        // Send pending subscription requests to local sessions from remote sessions
        // Deliver offline messages sent to local sessions that were unavailable in other nodes
        // Send available presences of local sessions to other resources of the same user
        PresenceUpdateHandler presenceUpdateHandler = XmppServer.getInstance().getPresenceUpdateHandler();
        for (LocalClientSession session : localRoutingTable.getClientRoutes()) {
            // Simulate that the local session has just became available
            session.setInitialized(false);
            // Simulate that current session presence has just been received
            presenceUpdateHandler.process(session.getPresence());
        }
    }

    public void joinedCluster(byte[] nodeID) {
        // Do nothing
    }

    public void leftCluster() {
        if (!XmppServer.getInstance().isShuttingDown()) {
            // Add local sessions to caches
            restoreCacheContent();
        }
    }

    public void leftCluster(byte[] nodeID) {
        // Do nothing
    }

    public void markedAsSeniorClusterMember() {
        // Do nothing
    }

    private void restoreCacheContent() {

        // Add client sessions hosted locally to the cache (using new nodeID)
        for (LocalClientSession session : localRoutingTable.getClientRoutes()) {
            addClientRoute(session.getAddress(), session);
        }
    }

}
