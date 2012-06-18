/**
 * $RCSfile: PacketDelivererImpl.java,v $
 * $Revision: 2715 $
 * $Date: 2005-08-23 22:16:45 -0300 (Tue, 23 Aug 2005) $
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

package org.b5chat.crossfire.route;


import org.b5chat.crossfire.PacketException;
import org.b5chat.crossfire.auth.UnauthorizedException;
import org.b5chat.crossfire.core.container.BasicModule;
import org.b5chat.crossfire.core.net.SocketPacketWriteHandler;
import org.b5chat.crossfire.server.XmppServer;
import org.xmpp.packet.Packet;

/**
 * In-memory implementation of the packet deliverer service
 *
 * @author Iain Shigeoka
 */
public class PacketDelivererImpl extends BasicModule implements IPacketDeliverer {

    /**
     * The handler that does the actual delivery (could be a channel instead)
     */
    protected SocketPacketWriteHandler deliverHandler;

    public PacketDelivererImpl() {
        super("Packet Delivery");
    }

    public void deliver(Packet packet) throws UnauthorizedException, PacketException {
        if (packet == null) {
            throw new PacketException("Packet was null");
        }
        if (deliverHandler == null) {
            throw new PacketException("Could not send packet - no route" + packet.toString());
        }
        // Let the SocketPacketWriteHandler process the packet. SocketPacketWriteHandler may send
        // it over the socket or store it when user is offline or drop it.
        deliverHandler.process(packet);
    }

    @Override
	public void start() throws IllegalStateException {
        super.start();
        deliverHandler = new SocketPacketWriteHandler(XmppServer.getInstance().getRoutingTable());
    }

    @Override
	public void stop() {
        super.stop();
        deliverHandler = null;
    }
}
