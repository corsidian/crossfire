/**
 * $RCSfile: $
 * $Revision: $
 * $Date: $
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.emiva.crossfire.session.LocalClientSession;
import net.emiva.crossfire.session.LocalSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

/**
 * Internal component used by the IRoutingTable to keep references to routes hosted by this JVM. When
 * running in a cluster each cluster member will have its own IRoutingTable containing an instance of
 * this class. Each LocalRoutingTable is responsible for storing routes to components, client sessions
 * and outgoing server sessions hosted by local cluster node.
 *
 * @author Gaston Dombiak
 */
class LocalRoutingTable {
	
	private static final Logger Log = LoggerFactory.getLogger(LocalRoutingTable.class);

    Map<String, IRoutableChannelHandler> routes = new ConcurrentHashMap<String, IRoutableChannelHandler>();

    /**
     * Adds a route of a local {@link IRoutableChannelHandler}
     *
     * @param address the string representation of the JID associated to the route.
     * @param route the route hosted by this node.
     * @return true if the element was added or false if was already present.
     */
    boolean addRoute(String address, IRoutableChannelHandler route) {
        return routes.put(address, route) != route;
    }

    /**
     * Returns the route hosted by this node that is associated to the specified address.
     *
     * @param address the string representation of the JID associated to the route.
     * @return the route hosted by this node that is associated to the specified address.
     */
    IRoutableChannelHandler getRoute(String address) {
        return routes.get(address);
    }

    /**
     * Returns the client sessions that are connected to this JVM.
     *
     * @return the client sessions that are connected to this JVM.
     */
    Collection<LocalClientSession> getClientRoutes() {
        List<LocalClientSession> sessions = new ArrayList<LocalClientSession>();
        for (IRoutableChannelHandler route : routes.values()) {
            if (route instanceof LocalClientSession) {
                sessions.add((LocalClientSession) route);
            }
        }
        return sessions;
    }

    /**
     * Removes a route of a local {@link IRoutableChannelHandler}
     *
     * @param address the string representation of the JID associated to the route.
     */
    void removeRoute(String address) {
        routes.remove(address);
    }

    public void start() {
    }

    public void stop() {
        try {
            // Send the close stream header to all connected connections
            for (IRoutableChannelHandler route : routes.values()) {
                if (route instanceof LocalSession) {
                    LocalSession session = (LocalSession) route;
                    try {
                        // Notify connected client that the server is being shut down
                        session.getConnection().systemShutdown();
                    }
                    catch (Throwable t) {
                        // Ignore.
                    }
                }
            }
        }
        catch (Exception e) {
            // Ignore.
        }
    }

    public boolean isLocalRoute(JID jid) {
        return routes.containsKey(jid.toString());
    }
}
