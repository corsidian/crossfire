/**
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

package org.b5chat.crossfire.nio;


import org.apache.mina.common.IoSession;
import org.b5chat.crossfire.XMPPServer;
import org.b5chat.crossfire.net.ComponentStanzaHandler;
import org.b5chat.crossfire.net.StanzaHandler;
import org.b5chat.util.Globals;

/**
 * ConnectionHandler that knows which subclass of {@link StanzaHandler} should
 * be created and how to build and configure a {@link NIOConnection}.
 *
 * @author Gaston Dombiak
 */
public class ComponentConnectionHandler extends ConnectionHandler {
    public ComponentConnectionHandler(String serverName) {
        super(serverName);
    }

    @Override
	NIOConnection createNIOConnection(IoSession session) {
        return new NIOConnection(session, XMPPServer.getInstance().getPacketDeliverer());
    }

    @Override
	StanzaHandler createStanzaHandler(NIOConnection connection) {
        return new ComponentStanzaHandler(XMPPServer.getInstance().getPacketRouter(), serverName, connection);
    }

    @Override
	int getMaxIdleTime() {
        return Globals.getIntProperty("xmpp.component.idle", 6 * 60 * 1000) / 1000;
    }
}
