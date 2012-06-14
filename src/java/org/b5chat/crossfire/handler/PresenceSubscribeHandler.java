/**
 * $RCSfile: PresenceSubscribeHandler.java,v $
 * $Revision: 3136 $
 * $Date: 2005-12-01 02:06:16 -0300 (Thu, 01 Dec 2005) $
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

package org.b5chat.crossfire.handler;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.b5chat.crossfire.ChannelHandler;
import org.b5chat.crossfire.PacketDeliverer;
import org.b5chat.crossfire.PacketException;
import org.b5chat.crossfire.PresenceManager;
import org.b5chat.crossfire.RoutingTable;
import org.b5chat.crossfire.SharedGroupException;
import org.b5chat.crossfire.XMPPServer;
import org.b5chat.crossfire.container.BasicModule;
import org.b5chat.crossfire.roster.AskType;
import org.b5chat.crossfire.roster.IRoster;
import org.b5chat.crossfire.roster.IRosterItem;
import org.b5chat.crossfire.roster.IRosterManager;
import org.b5chat.crossfire.roster.RecvType;
import org.b5chat.crossfire.roster.RosterItem;
import org.b5chat.crossfire.roster.SubType;
import org.b5chat.crossfire.user.PresenceEventDispatcher;
import org.b5chat.crossfire.user.UserAlreadyExistsException;
import org.b5chat.crossfire.user.UserManager;
import org.b5chat.crossfire.user.UserNotFoundException;
import org.b5chat.util.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;
import org.xmpp.packet.Presence;

/**
 * Implements the presence protocol. Clients use this protocol to
 * update presence and roster information.
 * <p/>
 * The handler must properly detect the presence type, update the user's roster,
 * and inform presence subscribers of the session's updated presence
 * status. Presence serves many purposes in Jabber so this handler will
 * likely be the most complex of all handlers in the server.
 * <p/>
 * There are four basic types of presence updates:
 * <ul>
 * <li>Simple presence updates - addressed to the server (or to address), these updates
 * are properly addressed by the server, and multicast to
 * interested subscribers on the user's roster. An empty, missing,
 * or "unavailable" type attribute indicates a simple update (there
 * is no "available" type although it should be accepted by the server.
 * <li>Directed presence updates - addressed to particular jabber entities,
 * these presence updates are properly addressed and directly delivered
 * to the entity without broadcast to roster subscribers. Any update type
 * is possible except those reserved for subscription requests.
 * <li>Subscription requests - these updates request presence subscription
 * status changes. Such requests always affect the roster.  The server must:
 * <ul>
 * <li>update the roster with the proper subscriber info
 * <li>push the roster changes to the user
 * <li>forward the update to the correct parties.
 * </ul>
 * The valid types include "subscribe", "subscribed", "unsubscribed",
 * and "unsubscribe".
 * <li>XMPPServer probes - Provides a mechanism for servers to query the presence
 * status of users on another server. This allows users to immediately
 * know the presence status of users when they come online rather than way
 * for a presence update broadcast from the other server or tracking them
 * as they are received.  Requires S2S capabilities.
 * </ul>
 * <p/>
 * <h2>Warning</h2>
 * There should be a way of determining whether a session has
 * authorization to access this feature. I'm not sure it is a good
 * idea to do authorization in each handler. It would be nice if
 * the framework could assert authorization policies across channels.
 *
 * @author Iain Shigeoka
 */
public class PresenceSubscribeHandler extends BasicModule implements ChannelHandler<Presence> {

	private static final Logger Log = LoggerFactory.getLogger(PresenceSubscribeHandler.class);

    private RoutingTable routingTable;
    private XMPPServer localServer;
    private String serverName;
    private PacketDeliverer deliverer;
    private PresenceManager presenceManager;
    private IRosterManager rosterManager;
    private UserManager userManager;

    public PresenceSubscribeHandler() {
        super("Presence subscription handler");
    }

    public void process(Presence presence) throws PacketException {
    	if (presence == null) {
    		throw new IllegalArgumentException("Argument 'presence' cannot be null.");
    	}

        final Presence.Type type = presence.getType();

		if (type != Presence.Type.subscribe 
				&& type != Presence.Type.unsubscribe
				&& type != Presence.Type.subscribed 
				&& type != Presence.Type.unsubscribed) {
			throw new IllegalArgumentException("Packet processed by PresenceSubscribeHandler is "
					+ "not of a subscription-related type, but: " + type);
		}

		// RFC-6121 paragraph 3: "When a server processes or generates an outbound presence stanza
		// of type "subscribe", "subscribed", "unsubscribe", or "unsubscribed", the server MUST stamp
		// the outgoing presence stanza with the bare JID <localpart@domainpart> of the sending entity,
		// not the full JID <localpart@domainpart/resourcepart>."
		presence.setFrom(presence.getFrom().toBareJID());
		
		// RFC-6121 paragraph 3.1.3: "Before processing the inbound presence subscription request, the
		// contact's server SHOULD check the syntax of the JID contained in the 'to' attribute. If the
		// JID is of the form <contact@domainpart/resourcepart> instead of <contact@domainpart>, the
		// contact's server SHOULD treat it as if the request had been directed to the contact's bare
		// JID and modify the 'to' address accordingly.
		if (presence.getTo() != null) {
			presence.setTo(presence.getTo().toBareJID());
		}

        final JID senderJID = presence.getFrom();
        final JID recipientJID = presence.getTo();

        try {            
			// Reject presence subscription requests sent to the local server itself.
            if (recipientJID == null || recipientJID.toString().equals(serverName)) {
                if (type == Presence.Type.subscribe) {
                    Presence reply = new Presence();
                    reply.setTo(senderJID);
                    reply.setFrom(recipientJID);
                    reply.setType(Presence.Type.unsubscribed);
                    deliverer.deliver(reply);
                }
                return;
            }

            try {
                IRoster senderRoster = getRoster(senderJID);
                if (senderRoster != null) {
                    manageSub(recipientJID, true, type, senderRoster);
                }
                IRoster recipientRoster = getRoster(recipientJID);
                boolean recipientSubChanged = false;
                if (recipientRoster != null) {
                    recipientSubChanged = manageSub(senderJID, false, type, recipientRoster);
                }

                // Do not forward the packet to the recipient if the presence is of type subscribed
                // and the recipient user has not changed its subscription state.
                if (!(type == Presence.Type.subscribed && recipientRoster != null && !recipientSubChanged)) {

                    // If the user is already subscribed to the *local* user's presence then do not 
                    // forward the subscription request. Also, do not send an auto-reply on behalf
                    // of the user. This presence stanza is the user's server know that it MUST no 
                	// longer send notification of the subscription state change to the user. 
                	// See http://tools.ietf.org/html/rfc3921#section-7 and/or OF-38 
                    if (type == Presence.Type.subscribe && recipientRoster != null && !recipientSubChanged) {
                        try {
                            SubType subType = recipientRoster.getRosterItem(senderJID)
                                    .getSubStatus();
                            if (subType == IRosterItem.SUB_FROM || subType == IRosterItem.SUB_BOTH) {
                                return;
                            }
                        }
                        catch (UserNotFoundException e) {
                            // Weird case: Roster item does not exist. Should never happen
                        	Log.error("User does not exist while trying to update roster item. " +
                        			"This should never happen (this indicates a programming " +
                        			"logic error). Processing stanza: " + presence.toString(), e);
                        }
                    }

                    // Try to obtain a handler for the packet based on the routes. If the handler is
                    // a module, the module will be able to handle the packet. If the handler is a
                    // Session the packet will be routed to the client. If a route cannot be found
                    // then the packet will be delivered based on its recipient and sender.
                    List<JID> jids = routingTable.getRoutes(recipientJID, null);
                    if (!jids.isEmpty()) {
                        for (JID jid : jids) {
                            Presence presenteToSend = presence.createCopy();
                            // Stamp the presence with the user's bare JID as the 'from' address,
                            // as required by section 8.2.5 of RFC 3921
                            presenteToSend.setFrom(senderJID.toBareJID());
                            routingTable.routePacket(jid, presenteToSend, false);
                        }
                    }
                    else {
                        deliverer.deliver(presence.createCopy());
                    }

                    if (type == Presence.Type.subscribed) {
                        // Send the presence of the local user to the remote user. The remote user
                        // subscribed to the presence of the local user and the local user accepted
                        JID prober = localServer.isLocal(recipientJID) ?
                                new JID(recipientJID.toBareJID()) : recipientJID;
                        presenceManager.probePresence(prober, senderJID);
                        PresenceEventDispatcher.subscribedToPresence(recipientJID, senderJID);
                    }
                }

                if (type == Presence.Type.unsubscribed) {
                    // Send unavailable presence from all of the local user's available resources
                    // to the remote user
                    presenceManager.sendUnavailableFromSessions(recipientJID, senderJID);
                    PresenceEventDispatcher.unsubscribedToPresence(senderJID, recipientJID);
                }
            }
            catch (SharedGroupException e) {
                Presence result = presence.createCopy();
                JID sender = result.getFrom();
                result.setFrom(presence.getTo());
                result.setTo(sender);
                result.setError(PacketError.Condition.not_acceptable);
                deliverer.deliver(result);
            }
        }
        catch (Exception e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
        }
    }

    /**
     * <p>Obtain the roster for the given address or null if the address doesn't have a roster.</p>
     *
     * @param address The address to check
     * @return The roster or null if the address is not managed on the server
     */
    private IRoster getRoster(JID address) {
        String username;
        IRoster roster = null;
        if (localServer.isLocal(address) && userManager.isRegisteredUser(address.getNode())) {
            username = address.getNode();
            roster = rosterManager.getRoster(username);
        }
        return roster;
    }

    /**
     * Manage the subscription request. This method updates a user's roster
     * state, storing any changes made, and updating the roster owner if changes
     * occured.
     *
     * @param target    The roster target's jid (the item's jid to be changed)
     * @param isSending True if the request is being sent by the owner
     * @param type      The subscription change type (subscribe, unsubscribe, etc.)
     * @param rosterImpl    The Roster that is updated.
     * @return <tt>true</tt> if the subscription state has changed.
     */
    private boolean manageSub(JID target, boolean isSending, Presence.Type type, IRoster rosterImpl)
            throws UserAlreadyExistsException, SharedGroupException
    {
        IRosterItem item = null;
        AskType oldAsk;
        SubType oldSub = null;
        RecvType oldRecv;
        boolean newItem = false;
        try {
            if (rosterImpl.isRosterItem(target)) {
                item = rosterImpl.getRosterItem(target);
            }
            else {
                if (Presence.Type.unsubscribed == type || Presence.Type.unsubscribe == type ||
                        Presence.Type.subscribed == type) {
                    // Do not create a roster item when processing a confirmation of
                    // an unsubscription or receiving an unsubscription request or a
                    // subscription approval from an unknown user
                    return false;
                }
                item = rosterImpl.createRosterItem(target, false, true);
                newItem = true;
            }
            // Get a snapshot of the item state
            oldAsk = item.getAskStatus();
            oldSub = item.getSubStatus();
            oldRecv = item.getRecvStatus();
            // Update the item state based in the received presence type
            updateState(item, type, isSending);
            // Update the roster IF the item state has changed
            if (oldAsk != item.getAskStatus() || oldSub != item.getSubStatus() ||
                    oldRecv != item.getRecvStatus()) {
                rosterImpl.updateRosterItem(item);
            }
            else if (newItem) {
                // Do not push items with a state of "None + Pending In"
                if (item.getSubStatus() != IRosterItem.SUB_NONE ||
                        item.getRecvStatus() != IRosterItem.RECV_SUBSCRIBE) {
                    rosterImpl.broadcast(item, false);
                }
            }
        }
        catch (UserNotFoundException e) {
            // Should be there because we just checked that it's an item
            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
        }
        return oldSub != item.getSubStatus();
    }

    /**
     * <p>The transition state table.</p>
     * <p>The root 'old state' transition table is a Map of SubType keys to match
     * to the old state of the item. Each key returns a Map containing the next
     * transition table. Transitions are defined as:</p>
     * <ul>
     * <li>'send/receive' table: Lookup whether this updates was sent or received: obtain 'action' table - key: Presence.Type subcribe action, value: Map (transition table).</li>
     * <li>'new state' table: the changed item values</li>
     * </ul>
     */
    private static Hashtable<SubType, Map<String, Map<Presence.Type, Change>>> stateTable =
            new Hashtable<SubType, Map<String, Map<Presence.Type, Change>>>();

    static {
        Hashtable<Presence.Type, Change> subrTable;
        Hashtable<Presence.Type, Change> subsTable;
        Hashtable<String,Map<Presence.Type, Change>> sr;

        sr = new Hashtable<String,Map<Presence.Type, Change>>();
        subrTable = new Hashtable<Presence.Type, Change>();
        subsTable = new Hashtable<Presence.Type, Change>();
        sr.put("recv", subrTable);
        sr.put("send", subsTable);
        stateTable.put(IRosterItem.SUB_NONE, sr);
        // Item wishes to subscribe from owner
        // Set flag and update roster if this is a new state, this is the normal way to begin
        // a roster subscription negotiation.
        subrTable.put(Presence.Type.subscribe, new Change(IRosterItem.RECV_SUBSCRIBE, null, null)); // no transition
        // Item granted subscription to owner
        // The item's state immediately goes from NONE to TO and ask is reset
        subrTable.put(Presence.Type.subscribed, new Change(null, IRosterItem.SUB_TO, IRosterItem.ASK_NONE));
        // Item wishes to unsubscribe from owner
        // This makes no sense, there is no subscription to remove
        subrTable.put(Presence.Type.unsubscribe, new Change(null, null, null));
        // Owner has subscription to item revoked
        // Valid response if item requested subscription and we're denying request
        subrTable.put(Presence.Type.unsubscribed, new Change(null, null, IRosterItem.ASK_NONE));
        // Owner asking to subscribe to item this is the normal way to begin
        // a roster subscription negotiation.
        subsTable.put(Presence.Type.subscribe, new Change(null, null, IRosterItem.ASK_SUBSCRIBE));
        // Item granted a subscription from owner
        subsTable.put(Presence.Type.subscribed, new Change(IRosterItem.RECV_NONE, IRosterItem.SUB_FROM, null));
        // Owner asking to unsubscribe to item
        // This makes no sense (there is no subscription to unsubscribe from)
        subsTable.put(Presence.Type.unsubscribe, new Change(null, null, null));
        // Item has subscription from owner revoked
        // Valid response if item requested subscription and we're denying request
        subsTable.put(Presence.Type.unsubscribed, new Change(IRosterItem.RECV_NONE, null, null));

        sr = new Hashtable<String,Map<Presence.Type, Change>>();
        subrTable = new Hashtable<Presence.Type, Change>();
        subsTable = new Hashtable<Presence.Type, Change>();
        sr.put("recv", subrTable);
        sr.put("send", subsTable);
        stateTable.put(IRosterItem.SUB_FROM, sr);
        // Owner asking to subscribe to item
        // Set flag and update roster if this is a new state, this is the normal way to begin
        // a mutual roster subscription negotiation.
        subsTable.put(Presence.Type.subscribe, new Change(null, null, IRosterItem.ASK_SUBSCRIBE));
        // Item granted a subscription from owner
        // This may be necessary if the recipient didn't get an earlier subscribed grant
        // or as a denial of an unsubscribe request
        subsTable.put(Presence.Type.subscribed, new Change(IRosterItem.RECV_NONE, null, null));
        // Owner asking to unsubscribe to item
        // This makes no sense (there is no subscription to unsubscribe from)
        subsTable.put(Presence.Type.unsubscribe, new Change(null, IRosterItem.SUB_NONE, null));
        // Item has subscription from owner revoked
        // Immediately transition to NONE state
        subsTable.put(Presence.Type.unsubscribed, new Change(IRosterItem.RECV_NONE, IRosterItem.SUB_NONE, null));
        // Item wishes to subscribe from owner
        // Item already has a subscription so only interesting if item had requested unsubscribe
        // Set flag and update roster if this is a new state, this is the normal way to begin
        // a mutual roster subscription negotiation.
        subrTable.put(Presence.Type.subscribe, new Change(IRosterItem.RECV_NONE, null, null));
        // Item granted subscription to owner
        // The item's state immediately goes from FROM to BOTH and ask is reset
        subrTable.put(Presence.Type.subscribed, new Change(null, IRosterItem.SUB_BOTH, IRosterItem.ASK_NONE));
        // Item wishes to unsubscribe from owner
        // This is the normal mechanism of removing subscription
        subrTable.put(Presence.Type.unsubscribe, new Change(IRosterItem.RECV_UNSUBSCRIBE, IRosterItem.SUB_NONE, null));
        // Owner has subscription to item revoked
        // Valid response if owner requested subscription and item is denying request
        subrTable.put(Presence.Type.unsubscribed, new Change(null, null, IRosterItem.ASK_NONE));

        sr = new Hashtable<String,Map<Presence.Type, Change>>();
        subrTable = new Hashtable<Presence.Type, Change>();
        subsTable = new Hashtable<Presence.Type, Change>();
        sr.put("recv", subrTable);
        sr.put("send", subsTable);
        stateTable.put(IRosterItem.SUB_TO, sr);
        // Owner asking to subscribe to item
        // We're already subscribed, may be trying to unset a unsub request
        subsTable.put(Presence.Type.subscribe, new Change(null, null, IRosterItem.ASK_NONE));
        // Item granted a subscription from owner
        // Sets mutual subscription
        subsTable.put(Presence.Type.subscribed, new Change(IRosterItem.RECV_NONE, IRosterItem.SUB_BOTH, null));
        // Owner asking to unsubscribe to item
        // Normal method of removing subscription
        subsTable.put(Presence.Type.unsubscribe, new Change(null, IRosterItem.SUB_NONE, IRosterItem.ASK_UNSUBSCRIBE));
        // Item has subscription from owner revoked
        // No subscription to unsub, makes sense if denying subscription request or for
        // situations where the original unsubscribed got lost
        subsTable.put(Presence.Type.unsubscribed, new Change(IRosterItem.RECV_NONE, null, null));
        // Item wishes to subscribe from owner
        // This is the normal way to negotiate a mutual subscription
        subrTable.put(Presence.Type.subscribe, new Change(IRosterItem.RECV_SUBSCRIBE, null, null));
        // Item granted subscription to owner
        // Owner already subscribed to item, could be a unsub denial or a lost packet
        subrTable.put(Presence.Type.subscribed, new Change(null, null, IRosterItem.ASK_NONE));
        // Item wishes to unsubscribe from owner
        // There is no subscription. May be trying to cancel earlier subscribe request.
        subrTable.put(Presence.Type.unsubscribe, new Change(IRosterItem.RECV_NONE, IRosterItem.SUB_NONE, null));
        // Owner has subscription to item revoked
        // Setting subscription to none
        subrTable.put(Presence.Type.unsubscribed, new Change(null, IRosterItem.SUB_NONE, IRosterItem.ASK_NONE));

        sr = new Hashtable<String,Map<Presence.Type, Change>>();
        subrTable = new Hashtable<Presence.Type, Change>();
        subsTable = new Hashtable<Presence.Type, Change>();
        sr.put("recv", subrTable);
        sr.put("send", subsTable);
        stateTable.put(IRosterItem.SUB_BOTH, sr);
        // Owner asking to subscribe to item
        // Makes sense if trying to cancel previous unsub request
        subsTable.put(Presence.Type.subscribe, new Change(null, null, IRosterItem.ASK_NONE));
        // Item granted a subscription from owner
        // This may be necessary if the recipient didn't get an earlier subscribed grant
        // or as a denial of an unsubscribe request
        subsTable.put(Presence.Type.subscribed, new Change(IRosterItem.RECV_NONE, null, null));
        // Owner asking to unsubscribe to item
        // Set flags
        subsTable.put(Presence.Type.unsubscribe, new Change(null, IRosterItem.SUB_FROM, IRosterItem.ASK_UNSUBSCRIBE));
        // Item has subscription from owner revoked
        // Immediately transition them to TO state
        subsTable.put(Presence.Type.unsubscribed, new Change(IRosterItem.RECV_NONE, IRosterItem.SUB_TO, null));
        // Item wishes to subscribe to owner
        // Item already has a subscription so only interesting if item had requested unsubscribe
        // Set flag and update roster if this is a new state, this is the normal way to begin
        // a mutual roster subscription negotiation.
        subrTable.put(Presence.Type.subscribe, new Change(IRosterItem.RECV_NONE, null, null));
        // Item granted subscription to owner
        // Redundant unless denying unsub request
        subrTable.put(Presence.Type.subscribed, new Change(null, null, IRosterItem.ASK_NONE));
        // Item wishes to unsubscribe from owner
        // This is the normal mechanism of removing subscription
        subrTable.put(Presence.Type.unsubscribe, new Change(IRosterItem.RECV_UNSUBSCRIBE, IRosterItem.SUB_TO, null));
        // Owner has subscription to item revoked
        // Immediately downgrade state to FROM
        subrTable.put(Presence.Type.unsubscribed, new Change(IRosterItem.RECV_NONE, IRosterItem.SUB_FROM, IRosterItem.ASK_NONE));
    }

    /**
     * <p>Indicate a state change.</p>
     * <p>Use nulls to indicate fields that should not be changed.</p>
     */
    private static class Change {
        public Change(RecvType recv, SubType sub, AskType ask) {
            newRecv = recv;
            newSub = sub;
            newAsk = ask;
        }

        public RecvType newRecv;
        public SubType newSub;
        public AskType newAsk;
    }

    /**
     * Determine and call the update method based on the item's subscription state.
     * The method also turns the action and sending status into an integer code
     * for easier processing (switch statements).
     * <p/>
     * Code relies on states being in numerical order without skipping.
     * In addition, the receive states must parallel the send states
     * so that (send state X) + STATE_RECV_SUBSCRIBE == (receive state X)
     * where X is subscribe, subscribed, etc.
     * </p>
     *
     * @param item      The item to be updated
     * @param action    The new state change request
     * @param isSending True if the roster owner of the item is sending the new state change request
     */
    private static void updateState(IRosterItem item, Presence.Type action, boolean isSending) {
        Map<String, Map<Presence.Type, Change>> srTable = stateTable.get(item.getSubStatus());
        Map<Presence.Type, Change> changeTable = srTable.get(isSending ? "send" : "recv");
        Change change = changeTable.get(action);
        if (change.newAsk != null && change.newAsk != item.getAskStatus()) {
            item.setAskStatus(change.newAsk);
        }
        if (change.newSub != null && change.newSub != item.getSubStatus()) {
            item.setSubStatus(change.newSub);
        }
        if (change.newRecv != null && change.newRecv != item.getRecvStatus()) {
            item.setRecvStatus(change.newRecv);
        }
    }

    @Override
	public void initialize(XMPPServer server) {
        super.initialize(server);
        localServer = server;
        serverName = server.getServerInfo().getXMPPDomain();
        routingTable = server.getRoutingTable();
        deliverer = server.getPacketDeliverer();
        presenceManager = server.getPresenceManager();
        rosterManager = server.getRosterManager();
        userManager = server.getUserManager();
    }
}