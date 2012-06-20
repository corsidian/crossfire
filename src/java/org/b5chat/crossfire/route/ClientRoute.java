/**
 * $RCSfile: $
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

package org.b5chat.crossfire.route;


import org.b5chat.crossfire.server.NodeID;
import org.b5chat.util.cache.CacheSizes;
import org.b5chat.util.cache.Cacheable;

/**
 * Internal object used by RoutingTableImpl to keep track of the node that own a IClientSession
 * and whether the session is available or not.
 *
 * @author Gaston Dombiak
 */
@SuppressWarnings("serial")
public class ClientRoute implements Cacheable {

    private NodeID nodeID;
    private boolean available;

    public ClientRoute() {
    }


    public NodeID getNodeID() {
        return nodeID;
    }


    public boolean isAvailable() {
        return available;
    }

    public ClientRoute(NodeID nodeID, boolean available) {
        this.nodeID = nodeID;
        this.available = available;
    }

    public int getCachedSize() {
        // Approximate the size of the object in bytes by calculating the size
        // of each field.
        int size = 0;
        size += CacheSizes.sizeOfObject();      // overhead of object
        size += nodeID.toByteArray().length;                  // Node ID
        size += CacheSizes.sizeOfBoolean();     // available
        return size;
    }

}
