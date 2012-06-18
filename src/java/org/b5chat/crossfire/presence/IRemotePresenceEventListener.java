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

/**
 * Interface to listen for presence events of remote users. Use the
 * {@link RemotePresenceEventDispatcher#addListener(IRemotePresenceEventListener)}
 * method to register for events.
 *
 * @author Armando Jagucki
 */
public interface IRemotePresenceEventListener {

    /**
     * Notification message indicating that a remote user is now available or has changed
     * his available presence. This event is triggered when an available presence is received
     * by <tt>PresenceRouter</tt>.
     *
     * @param presence the received available presence.
     */
    public void remoteUserAvailable(Presence presence);

    /**
     * Notification message indicating that a remote user is no longer available.
     * A remote user becomes unavailable when an unavailable presence is received.
     * by <tt>PresenceRouter</tt>.
     *
     * @param presence the received unavailable presence.
     */
    public void remoteUserUnavailable(Presence presence);

}
