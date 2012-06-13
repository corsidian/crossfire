/**
 * Copyright (C) 2004-2009 B5Chat Community. All rights reserved.
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

package org.b5chat.crossfire.handler;


import org.b5chat.crossfire.IQHandlerInfo;
import org.b5chat.crossfire.XMPPServer;
import org.b5chat.crossfire.auth.UnauthorizedException;
import org.b5chat.crossfire.group.Group;
import org.b5chat.crossfire.roster.IRosterManager;
import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import java.util.Collection;

/**
 * Handler of IQ packets whose child element is "sharedgroup" with namespace
 * "http://www.b5chat.org/protocol/sharedgroup". This handler will return the list of
 * shared groups where the user sending the request belongs.
 *
 * @author Gaston Dombiak
 */
public class IQSharedGroupHandler extends IQHandler {

    private IQHandlerInfo info;
    private String serverName;
    private IRosterManager rosterManager;

    public IQSharedGroupHandler() {
        super("Shared Groups Handler");
        info = new IQHandlerInfo("sharedgroup", "http://www.b5chat.org/protocol/sharedgroup");
    }

    @Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
        IQ result = IQ.createResultIQ(packet);
        String username = packet.getFrom().getNode();
        if (!serverName.equals(packet.getFrom().getDomain()) || username == null) {
            // Users of remote servers are not allowed to get their "shared groups". Users of
            // remote servers cannot have shared groups in this server.
            // Besides, anonymous users do not belong to shared groups so answer an error
            result.setChildElement(packet.getChildElement().createCopy());
            result.setError(PacketError.Condition.not_allowed);
            return result;
        }

        Collection<Group> groups = rosterManager.getSharedGroups(username);
        Element sharedGroups = result.setChildElement("sharedgroup",
                "http://www.b5chat.org/protocol/sharedgroup");
        for (Group sharedGroup : groups) {
            String displayName = sharedGroup.getProperties().get("sharedRoster.displayName");
            if (displayName != null) {
                sharedGroups.addElement("group").setText(displayName);
            }
        }
        return result;
    }

    @Override
	public IQHandlerInfo getInfo() {
        return info;
    }

    @Override
	public void initialize(XMPPServer server) {
        super.initialize(server);
        serverName = server.getServerInfo().getXMPPDomain();
        rosterManager = server.getRosterManager();
    }
}
