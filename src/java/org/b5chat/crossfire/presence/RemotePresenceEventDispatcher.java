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

import org.xmpp.packet.Presence;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dispatches presence events of remote users. The following events are supported:
 * <ul>
 * <li><b>remoteUserAvailable</b> --> A remote user is now available.</li>
 * <li><b>remoteUserUnavailable</b> --> A remote user is no longer available.</li>
 * </ul>
 * Use {@link #addListener(IRemotePresenceEventListener)} and
 * {@link #removeListener(IRemotePresenceEventListener)} to add or remove {@link IRemotePresenceEventListener}.
 *
 * @author Armando Jagucki
 */
public class RemotePresenceEventDispatcher {

    private static List<IRemotePresenceEventListener> listeners =
            new CopyOnWriteArrayList<IRemotePresenceEventListener>();

    /**
     * Registers a listener to receive events.
     *
     * @param listener the listener.
     */
    public static void addListener(IRemotePresenceEventListener listener) {
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
    public static void removeListener(IRemotePresenceEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notification message indicating that a remote user is now available or has changed
     * his available presence. This event is triggered when an available presence is received
     * by <tt>PresenceRouter</tt>.
     *
     * @param presence the received available presence.
     */
    public static void remoteUserAvailable(Presence presence) {
        if (!listeners.isEmpty()) {
            for (IRemotePresenceEventListener listener : listeners) {
                listener.remoteUserAvailable(presence);
            }
        }
    }

    /**
     * Notification message indicating that a remote user that was available is no longer
     * available. A remote user becomes unavailable when an unavailable presence is received.
     * by <tt>PresenceRouter</tt>.
     *
     * @param presence the received unavailable presence.
     */
    public static void remoteUserUnavailable(Presence presence) {
        if (!listeners.isEmpty()) {
            for (IRemotePresenceEventListener listener : listeners) {
                listener.remoteUserUnavailable(presence);
            }
        }
    }

}
