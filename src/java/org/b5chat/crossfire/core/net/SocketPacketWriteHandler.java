/**
 * $RCSfile: SocketPacketWriteHandler.java,v $
 * $Revision: 3137 $
 * $Date: 2005-12-01 02:11:05 -0300 (Thu, 01 Dec 2005) $
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

package org.b5chat.crossfire.core.net;


import org.b5chat.crossfire.IChannelHandler;
import org.b5chat.crossfire.PacketException;
import org.b5chat.crossfire.auth.UnauthorizedException;
import org.b5chat.crossfire.route.IPacketRouter;
import org.b5chat.crossfire.route.IRoutingTable;
import org.b5chat.crossfire.server.XmppServer;
import org.b5chat.util.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

/**
 * This IChannelHandler writes packet data to connections.
 *
 * @author Iain Shigeoka
 * @see IPacketRouter
 */
public class SocketPacketWriteHandler implements IChannelHandler {

	private static final Logger Log = LoggerFactory.getLogger(SocketPacketWriteHandler.class);

    private XmppServer server;
    private IRoutingTable routingTable;

    public SocketPacketWriteHandler(IRoutingTable routingTable) {
        this.routingTable = routingTable;
        this.server = XmppServer.getInstance();
    }

     public void process(Packet packet) throws UnauthorizedException, PacketException {
        try {
            JID recipient = packet.getTo();
            // The target domain belongs to the local server
            if (recipient == null || (recipient.getNode() == null && recipient.getResource() == null)) {
                // no TO was found so send back the packet to the sender
                routingTable.routePacket(packet.getFrom(), packet, false);
            }
            else if (recipient.getResource() != null || !(packet instanceof Presence)) {
                // JID is of the form <user@domain/resource>
                routingTable.routePacket(recipient, packet, false);
            }
            else {
                // JID is of the form <user@domain>
                for (JID route : routingTable.getRoutes(recipient, null)) {
                    routingTable.routePacket(route, packet, false);
                }
            }
        }
        catch (Exception e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error.deliver") + "\n" + packet.toString(), e);
        }
    }
}
