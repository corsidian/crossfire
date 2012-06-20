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

package org.b5chat.crossfire.session;


import org.xmpp.packet.Presence;

/**
 * Client session information to be used when running in a cluster. The session
 * information is shared between cluster nodes and is meant to be used by remote
 * sessions to avoid invocation remote calls and instead use cached information.
 * This optimization should give an important boost to the application specifically
 * while users are logging in.<p>
 *
 * ISession information is stored after a user authenticated and bound a resource.
 *
 * @author Gaston Dombiak
 */
public class ClientSessionInfo {
    private Presence presence;
    private String defaultList;
    private String activeList;
    private boolean offlineFloodStopped;

    public ClientSessionInfo() {
    }

    public ClientSessionInfo(LocalClientSession session) {
        presence = session.getPresence();
        defaultList = session.getDefaultList() != null ? session.getDefaultList().getName() : null;
        activeList = session.getActiveList() != null ? session.getActiveList().getName() : null;
        offlineFloodStopped = session.isOfflineFloodStopped();
    }

    public Presence getPresence() {
        return presence;
    }

    public String getDefaultList() {
        return defaultList;
    }

    public String getActiveList() {
        return activeList;
    }

    public boolean isOfflineFloodStopped() {
        return offlineFloodStopped;
    }
}
