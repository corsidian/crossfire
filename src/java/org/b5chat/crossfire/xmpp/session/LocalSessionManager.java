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

package org.b5chat.crossfire.xmpp.session;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A LocalSessionManager keeps track of sessions that are connected to this JVM and for
 * which there is no route. That is, sessions that are added to the routing table are
 * not going to be stored by this manager.<p>
 *
 * For external component sessions, incoming server sessions and connection manager
 * sessions there is never going to be a route so they are only kept here. Client
 * sessions before they authenticate are kept in this manager but once authenticated
 * they are removed since a new route is created for authenticated client sessions.<p>
 *
 * Sessions stored in this manager are not accessible from other cluster nodes. However,
 * sessions for which there is a route in the routing table can be accessed from other
 * cluster nodes. The only exception to this rule are the sessions of external components.
 * External component sessions are kept in this manager but all components (internal and
 * external) create a route in the routing table for the service they provide. That means
 * that services of components are accessible from other cluster nodes and when the
 * component is an external component then its session will be used to deliver packets
 * through the external component's session. 
 *
 * @author Gaston Dombiak
 */
class LocalSessionManager {
    /**
     * Map that holds sessions that has been created but haven't been authenticated yet. The Map
     * will hold client sessions.
     */
    private Map<String, LocalClientSession> preAuthenticatedSessions = new ConcurrentHashMap<String, LocalClientSession>();

    public Map<String, LocalClientSession> getPreAuthenticatedSessions() {
        return preAuthenticatedSessions;
    }

    public void start() {

    }

    public void stop() {
        try {
            // Send the close stream header to all connected connections
            Set<LocalSession> sessions = new HashSet<LocalSession>();
            sessions.addAll(preAuthenticatedSessions.values());

            for (LocalSession session : sessions) {
                try {
                    // Notify connected client that the server is being shut down
                    session.getConnection().systemShutdown();
                }
                catch (Throwable t) {
                    // Ignore.
                }
            }
        }
        catch (Exception e) {
            // Ignore.
        }
    }
}
