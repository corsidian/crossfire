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


import java.util.HashSet;
import java.util.Set;

import org.b5chat.crossfire.server.XmppServer;
import org.xmpp.packet.JID;

/**
 * Represents a directed presence sent from a session hosted in a cluster node
 * to another entity (e.g. user or MUC service) hosted in some other cluster
 * node.<p>
 *
 * This information needs to be shared by all cluster nodes so that if a
 * cluster node goes down then directed presences can be correctly cleaned
 * up.<p>
 *
 * Note that an instance of this class will be created and kept in the clustered
 * cache only when entities hosted by different cluster nodes are involved.
 *
 * @author Gaston Dombiak
 */
public class DirectedPresence {
    /**
     * ID of the node that received the request to send a directed presence. This is the
     * node ID that hosts the sender.
     */
    private byte[] nodeID;
    /**
     * Full JID of the entity that received the directed presence.
     * E.g.: paul@js.com/Spark or conference.js.com
     */
    private JID handler;
    /**
     * List of JIDs with the TO value of the directed presences.
     * E.g.: paul@js.com or room1@conference.js.com
     */
    private Set<String> receivers = new HashSet<String>();

    public DirectedPresence() {
    }

    public DirectedPresence(JID handlerJID) {
        this.handler = handlerJID;
        this.nodeID = XmppServer.getInstance().getNodeID().toByteArray();
    }

    public byte[] getNodeID() {
        return nodeID;
    }

    public JID getHandler() {
        return handler;
    }

    public Set<String> getReceivers() {
        return receivers;
    }

    public void addReceiver(String receiver) {
        receivers.add(receiver);
    }

    public void removeReceiver(String receiver) {
        receivers.remove(receiver);
    }

    public boolean isEmpty() {
        return receivers.isEmpty();
    }
}
