/**
 * $RCSfile: PresenceManagerImpl.java,v $
 * $Revision: 3128 $
 * $Date: 2005-11-30 15:31:54 -0300 (Wed, 30 Nov 2005) $
 *
 * Copyright (C) 2004-2008 B5Chat Community. All rights reserved.
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

package org.b5chat.crossfire.xmpp.presence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.b5chat.crossfire.core.container.BasicModule;
import org.b5chat.crossfire.core.util.LocaleUtils;
import org.b5chat.crossfire.core.util.StringUtils;
import org.b5chat.crossfire.core.util.cache.Cache;
import org.b5chat.crossfire.core.util.cache.CacheFactory;
import org.b5chat.crossfire.database.DbConnectionManager;
import org.b5chat.crossfire.xmpp.auth.UnauthorizedException;
import org.b5chat.crossfire.xmpp.privacy.PrivacyList;
import org.b5chat.crossfire.xmpp.privacy.PrivacyListManager;
import org.b5chat.crossfire.xmpp.roster.Roster;
import org.b5chat.crossfire.xmpp.roster.RosterItem;
import org.b5chat.crossfire.xmpp.roster.RosterManager;
import org.b5chat.crossfire.xmpp.route.IPacketDeliverer;
import org.b5chat.crossfire.xmpp.server.XmppServer;
import org.b5chat.crossfire.xmpp.session.IClientSession;
import org.b5chat.crossfire.xmpp.session.SessionManager;
import org.b5chat.crossfire.xmpp.user.IUserEventListener;
import org.b5chat.crossfire.xmpp.user.User;
import org.b5chat.crossfire.xmpp.user.UserEventDispatcher;
import org.b5chat.crossfire.xmpp.user.UserManager;
import org.b5chat.crossfire.xmpp.user.UserNotFoundException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;
import org.xmpp.packet.Presence;

/**
 * Simple in memory implementation of the IPresenceManager interface.
 *
 * @author Iain Shigeoka
 */
public class PresenceManagerImpl extends BasicModule implements IPresenceManager, IUserEventListener {

	private static final Logger Log = LoggerFactory.getLogger(PresenceManagerImpl.class);

    private static final String LOAD_OFFLINE_PRESENCE =
            "SELECT offlinePresence, offlineDate FROM ofPresence WHERE username=?";
    private static final String INSERT_OFFLINE_PRESENCE =
            "INSERT INTO ofPresence(username, offlinePresence, offlineDate) VALUES(?,?,?)";
    private static final String DELETE_OFFLINE_PRESENCE =
            "DELETE FROM ofPresence WHERE username=?";

    private static final String NULL_STRING = "NULL";
    private static final long NULL_LONG = -1L;

    private SessionManager sessionManager;
    private UserManager userManager;
    private RosterManager rosterManager;
    private XmppServer server;
    private IPacketDeliverer deliverer;
    private PresenceUpdateHandler presenceUpdateHandler;

    private Cache<String, Long> lastActivityCache;
    private Cache<String, String> offlinePresenceCache;

    public PresenceManagerImpl() {
        super("Presence manager");
    }

    public boolean isAvailable(User user) {
        return sessionManager.getActiveSessionCount(user.getUsername()) > 0;
    }

    public Presence getPresence(User user) {
        if (user == null) {
            return null;
        }
        Presence presence = null;

        for (IClientSession session : sessionManager.getSessions(user.getUsername())) {
            if (presence == null) {
                presence = session.getPresence();
            }
            else {
                // Get the ordinals of the presences to compare. If no ordinal is available then
                // assume a value of -1
                int o1 = presence.getShow() != null ? presence.getShow().ordinal() : -1;
                int o2 = session.getPresence().getShow() != null ?
                        session.getPresence().getShow().ordinal() : -1;
                // Compare the presences' show ordinals
                if (o1 > o2) {
                    presence = session.getPresence();
                }
            }
        }
        return presence;
    }

    public Collection<Presence> getPresences(String username) {
        if (username == null) {
            return null;
        }
        List<Presence> presences = new ArrayList<Presence>();

        for (IClientSession session : sessionManager.getSessions(username)) {
            presences.add(session.getPresence());
        }
        return Collections.unmodifiableCollection(presences);
    }

    public String getLastPresenceStatus(User user) {
        String username = user.getUsername();
        String presenceStatus = null;
        String presenceXML = offlinePresenceCache.get(username);
        if (presenceXML == null) {
            loadOfflinePresence(username);
        }
        presenceXML = offlinePresenceCache.get(username);
        if (presenceXML != null) {
            // If the cached answer is no data, return null.
            if (presenceXML.equals(NULL_STRING)) {
                return null;
            }
            // Otherwise, parse out the status from the XML.
            try {
                // Parse the element
                Document element = DocumentHelper.parseText(presenceXML);
                presenceStatus = element.getRootElement().elementTextTrim("status");
            }
            catch (DocumentException e) {
                Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
            }
        }
        return presenceStatus;
    }

    public long getLastActivity(User user) {
        String username = user.getUsername();
        long lastActivity = NULL_LONG;
        Long offlineDate = lastActivityCache.get(username);
        if (offlineDate == null) {
            loadOfflinePresence(username);
        }
        offlineDate = lastActivityCache.get(username);
        if (offlineDate != null) {
            // If the cached answer is no data, return -1.
            if (offlineDate == NULL_LONG) {
                return NULL_LONG;
            }
            else {
                try {
                    lastActivity = (System.currentTimeMillis() - offlineDate);
                }
                catch (NumberFormatException e) {
                    Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
                }
            }
        }
        return lastActivity;
    }

    public void userAvailable(Presence presence) {
        // Delete the last unavailable presence of this user since the user is now
        // available. Only perform this operation if this is an available presence sent to
        // THE SERVER and the presence belongs to a local user.
        if (presence.getTo() == null && server.isLocal(presence.getFrom())) {
            String username = presence.getFrom().getNode();
            if (username == null || !userManager.isRegisteredUser(username)) {
                // Ignore anonymous users
                return;
            }

            // Optimization: only delete the unavailable presence information if this
            // is the first session created on the server.
            if (sessionManager.getSessionCount(username) > 1) {
                return;
            }

            deleteOfflinePresenceFromDB(username);

            // Remove data from cache.
            offlinePresenceCache.remove(username);
            lastActivityCache.remove(username);
        }
    }

    private void deleteOfflinePresenceFromDB(String username) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(DELETE_OFFLINE_PRESENCE);
            pstmt.setString(1, username);
            pstmt.execute();
        }
        catch (SQLException sqle) {
            Log.error(sqle.getMessage(), sqle);
        }
        finally {
            DbConnectionManager.closeConnection(pstmt, con);
        }
    }

    public void userUnavailable(Presence presence) {
        // Only save the last presence status and keep track of the time when the user went
        // offline if this is an unavailable presence sent to THE SERVER and the presence belongs
        // to a local user.
        if (presence.getTo() == null && server.isLocal(presence.getFrom())) {
            String username = presence.getFrom().getNode();
            if (username == null || !userManager.isRegisteredUser(username)) {
                // Ignore anonymous users
                return;
            }

            // If the user has any remaining sessions, don't record the offline info.
            if (sessionManager.getActiveSessionCount(username) > 0) {
                return;
            }

            String offlinePresence = null;
            // Save the last unavailable presence of this user if the presence contains any
            // child element such as <status>.
            if (!presence.getElement().elements().isEmpty()) {
                offlinePresence = presence.toXML();
            }
            // Keep track of the time when the user went offline
            java.util.Date offlinePresenceDate = new java.util.Date();

            boolean addedToCache;
            if (offlinePresence == null) {
                addedToCache = !NULL_STRING.equals(offlinePresenceCache.put(username, NULL_STRING));
            }
            else {
                addedToCache = !offlinePresence.equals(offlinePresenceCache.put(username, offlinePresence));
            }
            if (!addedToCache) {
                return;
            }
            lastActivityCache.put(username, offlinePresenceDate.getTime());

            // delete existing offline presence (if any)
            deleteOfflinePresenceFromDB(username);
            
            // Insert data into the database.
            Connection con = null;
            PreparedStatement pstmt = null;
            try {
                con = DbConnectionManager.getConnection();
                pstmt = con.prepareStatement(INSERT_OFFLINE_PRESENCE);
                pstmt.setString(1, username);
                if (offlinePresence != null) {
                    DbConnectionManager.setLargeTextField(pstmt, 2, offlinePresence);
                }
                else {
                    pstmt.setNull(2, Types.VARCHAR);
                }
                pstmt.setString(3, StringUtils.dateToMillis(offlinePresenceDate));
                pstmt.execute();
            }
            catch (SQLException sqle) {
                Log.error("Error storing offline presence of user: " + username, sqle);
            }
            finally {
                DbConnectionManager.closeConnection(pstmt, con);
            }
        }
    }

    public void handleProbe(Presence packet) throws UnauthorizedException {
        String username = packet.getTo().getNode();
        try {
            Roster roster = rosterManager.getRoster(username);
            RosterItem item = roster.getRosterItem(packet.getFrom());
            if (item.getSubStatus() == RosterItem.SUB_FROM
                    || item.getSubStatus() == RosterItem.SUB_BOTH) {
                probePresence(packet.getFrom(),  packet.getTo());
            }
            else {
                PacketError.Condition error = PacketError.Condition.not_authorized;
                if ((item.getSubStatus() == RosterItem.SUB_NONE &&
                        item.getRecvStatus() != RosterItem.RECV_SUBSCRIBE) ||
                        (item.getSubStatus() == RosterItem.SUB_TO &&
                        item.getRecvStatus() != RosterItem.RECV_SUBSCRIBE)) {
                    error = PacketError.Condition.forbidden;
                }
                Presence presenceToSend = new Presence();
                presenceToSend.setError(error);
                presenceToSend.setTo(packet.getFrom());
                presenceToSend.setFrom(packet.getTo());
                deliverer.deliver(presenceToSend);
            }
        }
        catch (UserNotFoundException e) {
            Presence presenceToSend = new Presence();
            presenceToSend.setError(PacketError.Condition.forbidden);
            presenceToSend.setTo(packet.getFrom());
            presenceToSend.setFrom(packet.getTo());
            deliverer.deliver(presenceToSend);
        }
    }

    public boolean canProbePresence(JID prober, String probee) throws UserNotFoundException {
        RosterItem item = rosterManager.getRoster(probee).getRosterItem(prober);
        return item.getSubStatus() == RosterItem.SUB_FROM
                || item.getSubStatus() == RosterItem.SUB_BOTH;
    }

    public void probePresence(JID prober, JID probee) {
        try {
            if (server.isLocal(probee)) {
                // Local probers should receive presences of probee in all connected resources
                Collection<JID> proberFullJIDs = new ArrayList<JID>();
                if (prober.getResource() == null && server.isLocal(prober)) {
                    for (IClientSession session : sessionManager.getSessions(prober.getNode())) {
                        proberFullJIDs.add(session.getAddress());
                    }
                }
                else {
                    proberFullJIDs.add(prober);
                }
                // If the probee is a local user then don't send a probe to the contact's server.
                // But instead just send the contact's presence to the prober
                Collection<IClientSession> sessions = sessionManager.getSessions(probee.getNode());
                if (sessions.isEmpty()) {
                    // If the probee is not online then try to retrieve his last unavailable
                    // presence which may contain particular information and send it to the
                    // prober
                    String presenceXML = offlinePresenceCache.get(probee.getNode());
                    if (presenceXML == null) {
                        loadOfflinePresence(probee.getNode());
                    }
                    presenceXML = offlinePresenceCache.get(probee.getNode());
                    if (presenceXML != null && !NULL_STRING.equals(presenceXML)) {
                        try {
                            // Parse the element
                            Document element = DocumentHelper.parseText(presenceXML);
                            // Create the presence from the parsed element
                            Presence presencePacket = new Presence(element.getRootElement());
                            presencePacket.setFrom(probee.toBareJID());
                            // Check if default privacy list of the probee blocks the
                            // outgoing presence
                            PrivacyList list = PrivacyListManager.getInstance()
                                    .getDefaultPrivacyList(probee.getNode());
                            // Send presence to all prober's resources
                            for (JID receipient : proberFullJIDs) {
                                presencePacket.setTo(receipient);
                                if (list == null || !list.shouldBlockPacket(presencePacket)) {
                                    // Send the presence to the prober
                                    deliverer.deliver(presencePacket);
                                }
                            }
                        }
                        catch (Exception e) {
                            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
                        }
                    }
                }
                else {
                    // The contact is online so send to the prober all the resources where the
                    // probee is connected
                    for (IClientSession session : sessions) {
                        // Create presence to send from probee to prober
                        Presence presencePacket = session.getPresence().createCopy();
                        presencePacket.setFrom(session.getAddress());
                        // Check if a privacy list of the probee blocks the outgoing presence
                        PrivacyList list = session.getActiveList();
                        list = list == null ? session.getDefaultList() : list;
                        // Send presence to all prober's resources
                        for (JID receipient : proberFullJIDs) {
                            presencePacket.setTo(receipient);
                            if (list != null) {
                                if (list.shouldBlockPacket(presencePacket)) {
                                    // Default list blocked outgoing presence so skip this session
                                    continue;
                                }
                            }
                            try {
                                deliverer.deliver(presencePacket);
                            }
                            catch (Exception e) {
                                Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
        }
    }

    public void sendUnavailableFromSessions(JID recipientJID, JID userJID) {
        if (XmppServer.getInstance().isLocal(userJID) && userManager.isRegisteredUser(userJID.getNode())) {
            for (IClientSession session : sessionManager.getSessions(userJID.getNode())) {
                // Do not send an unavailable presence if the user sent a direct available presence
                if (presenceUpdateHandler.hasDirectPresence(session.getAddress(), recipientJID)) {
                    continue;
                }
                Presence presencePacket = new Presence();
                presencePacket.setType(Presence.Type.unavailable);
                presencePacket.setFrom(session.getAddress());
                // Ensure that unavailable presence is sent to all receipient's resources
                Collection<JID> recipientFullJIDs = new ArrayList<JID>();
                if (server.isLocal(recipientJID)) {
                    for (IClientSession targetSession : sessionManager
                            .getSessions(recipientJID.getNode())) {
                        recipientFullJIDs.add(targetSession.getAddress());
                    }
                }
                else {
                    recipientFullJIDs.add(recipientJID);
                }
                for (JID jid : recipientFullJIDs) {
                    presencePacket.setTo(jid);
                    try {
                        deliverer.deliver(presencePacket);
                    }
                    catch (Exception e) {
                        Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
                    }
                }
            }
        }
    }

    public void userCreated(User user, Map<String, Object> params) {
        // Do nothing
    }

    public void userDeleting(User user, Map<String, Object> params) {
        // Delete user information
        deleteOfflinePresenceFromDB(user.getUsername());
    }

    public void userModified(User user, Map<String, Object> params) {
        // Do nothing
    }

// #####################################################################
    // IModule management
    // #####################################################################

    @Override
	public void initialize(XmppServer server) {
        super.initialize(server);
        this.server = server;

        offlinePresenceCache = CacheFactory.createCache("Offline Presence Cache");
        lastActivityCache = CacheFactory.createCache("Last Activity Cache");

        deliverer = server.getPacketDeliverer();
        sessionManager = server.getSessionManager();
        userManager = server.getUserManager();
        presenceUpdateHandler = server.getPresenceUpdateHandler();
        rosterManager = server.getRosterManager();
    }

    @Override
	public void start() throws IllegalStateException {
        super.start();
        // Listen for user deletion events
        UserEventDispatcher.addListener(this);

    }

    @Override
	public void stop() {
        // Clear the caches when stopping the module.
        offlinePresenceCache.clear();
        lastActivityCache.clear();
        // Stop listening for user deletion events
        UserEventDispatcher.removeListener(this);
    }

    /**
     * Loads offline presence data for the user into cache.
     *
     * @param username the username.
     */
    private void loadOfflinePresence(String username) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Lock lock = CacheFactory.getLock(username, offlinePresenceCache);
        try {
            lock.lock();
            if (!offlinePresenceCache.containsKey(username) || !lastActivityCache.containsKey(username)) {
                con = DbConnectionManager.getConnection();
                pstmt = con.prepareStatement(LOAD_OFFLINE_PRESENCE);
                pstmt.setString(1, username);
                rs = pstmt.executeQuery();
                if (rs.next()) {
                    String offlinePresence = DbConnectionManager.getLargeTextField(rs, 1);
                    if (rs.wasNull()) {
                        offlinePresence = NULL_STRING;
                    }
                    long offlineDate = Long.parseLong(rs.getString(2).trim());
                    offlinePresenceCache.put(username, offlinePresence);
                    lastActivityCache.put(username, offlineDate);
                }
                else {
                    offlinePresenceCache.put(username, NULL_STRING);
                    lastActivityCache.put(username, NULL_LONG);
                }
            }
        }
        catch (SQLException sqle) {
            Log.error(sqle.getMessage(), sqle);
        }
        finally {
            DbConnectionManager.closeConnection(rs, pstmt, con);
            lock.unlock();
        }
    }
}
