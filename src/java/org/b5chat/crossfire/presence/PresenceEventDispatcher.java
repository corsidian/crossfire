/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
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

package org.b5chat.crossfire.presence;


import org.b5chat.crossfire.session.IClientSession;
import org.xmpp.packet.JID;
import org.xmpp.packet.Presence;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dispatches presence events. The following events are supported:
 * <ul>
 * <li><b>availableSession</b> --> A session is now available to receive communication.</li>
 * <li><b>unavailableSession</b> --> A session is no longer available.</li>
 * <li><b>presencePriorityChanged</b> --> The priority of a resource has changed.</li>
 * <li><b>presenceChanged</b> --> The show or status value of an available session has changed.</li>
 * </ul>
 * Use {@link #addListener(IPresenceEventListener)} and
 * {@link #removeListener(IPresenceEventListener)} to add or remove {@link IPresenceEventListener}.
 *
 * @author Gaston Dombiak
 */
public class PresenceEventDispatcher {

    private static List<IPresenceEventListener> listeners =
            new CopyOnWriteArrayList<IPresenceEventListener>();

    /**
     * Registers a listener to receive events.
     *
     * @param listener the listener.
     */
    public static void addListener(IPresenceEventListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        listeners.add(listener);
    }

    /**
     * Unregisters a listener to receive events.
     *
     * @param listener the listener.
     */
    public static void removeListener(IPresenceEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notification message indicating that a session that was not available is now
     * available. A session becomes available when an available presence is received.
     * Sessions that are available will have a route in the routing table thus becoming
     * eligible for receiving messages (in particular messages sent to the user bare JID).
     *
     * @param session the session that is now available.
     * @param presence the received available presence.
     */
    public static void availableSession(IClientSession session, Presence presence) {
        if (!listeners.isEmpty()) {
            for (IPresenceEventListener listener : listeners) {
                listener.availableSession(session, presence);
            }
        }
    }

    /**
     * Notification message indicating that a session that was available is no longer
     * available. A session becomes unavailable when an unavailable presence is received.
     * The entity may still be connected to the server and may send an available presence
     * later to indicate that communication can proceed.
     *
     * @param session the session that is no longer available.
     * @param presence the received unavailable presence.
     */
    public static void unavailableSession(IClientSession session, Presence presence) {
        if (!listeners.isEmpty()) {
            for (IPresenceEventListener listener : listeners) {
                listener.unavailableSession(session, presence);
            }
        }
    }


    /**
     * Notification message indicating that an available session has changed its
     * presence. This is the case when the user presence changed the show value
     * (e.g. away, dnd, etc.) or the presence status message.
     *
     * @param session the affected session.
     * @param presence the received available presence with the new information.
     */
    public static void presenceChanged(IClientSession session, Presence presence) {
        if (!listeners.isEmpty()) {
            for (IPresenceEventListener listener : listeners) {
                listener.presenceChanged(session, presence);
            }
        }
    }

    /**
     * Notification message indicating that a user has successfully subscribed
     * to the presence of another user.
     * 
     * @param subscriberJID the user that initiated the subscription.
     * @param authorizerJID the user that authorized the subscription.
     */
    public static void subscribedToPresence(JID subscriberJID, JID authorizerJID) {
        if (!listeners.isEmpty()) {
            for (IPresenceEventListener listener : listeners) {
                listener.subscribedToPresence(subscriberJID, authorizerJID);
            }
        }
    }
    
    /**
     * Notification message indicating that a user has unsubscribed
     * to the presence of another user.
     * 
     * @param unsubscriberJID the user that initiated the unsubscribe request.
     * @param recipientJID    the recipient user of the unsubscribe request.
     */
    public static void unsubscribedToPresence(JID unsubscriberJID, JID recipientJID) {
        if (!listeners.isEmpty()) {
            for (IPresenceEventListener listener : listeners) {
                listener.unsubscribedToPresence(unsubscriberJID, recipientJID);
            }
        }
    }
}