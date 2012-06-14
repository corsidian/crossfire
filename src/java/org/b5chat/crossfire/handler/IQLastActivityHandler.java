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
import org.b5chat.crossfire.PresenceManager;
import org.b5chat.crossfire.XMPPServer;
import org.b5chat.crossfire.auth.UnauthorizedException;
import org.b5chat.crossfire.disco.ServerFeaturesProvider;
import org.b5chat.crossfire.roster.IRosterItem;
import org.b5chat.crossfire.roster.IRosterManager;
import org.b5chat.crossfire.user.User;
import org.b5chat.crossfire.user.UserManager;
import org.b5chat.crossfire.user.UserNotFoundException;
import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Implements the TYPE_IQ jabber:iq:last protocol (last activity). Allows users to find out
 * the number of seconds another user has been offline. This information is only available to
 * those users that already subscribed to the users presence. Otherwhise, a <tt>forbidden</tt>
 * error will be returned.
 *
 * @author Gaston Dombiak
 */
public class IQLastActivityHandler extends IQHandler implements ServerFeaturesProvider {

    private IQHandlerInfo info;
    private PresenceManager presenceManager;
    private IRosterManager rosterManager;

    public IQLastActivityHandler() {
        super("XMPP Last Activity Handler");
        info = new IQHandlerInfo("query", "jabber:iq:last");
    }

    @Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
        IQ reply = IQ.createResultIQ(packet);
        Element lastActivity = reply.setChildElement("query", "jabber:iq:last");
        String sender = packet.getFrom().getNode();
        String username = packet.getTo() == null ? null : packet.getTo().getNode();

        // Check if any of the usernames is null
        if (sender == null || username == null) {
            reply.setError(PacketError.Condition.forbidden);
            return reply;
        }

        try {
            IRosterItem item = rosterManager.getRoster(username).getRosterItem(packet.getFrom());
            // Check that the user requesting this information is subscribed to the user's presence
            if (item.getSubStatus() == IRosterItem.SUB_FROM ||
                    item.getSubStatus() == IRosterItem.SUB_BOTH) {
                if (sessionManager.getSessions(username).isEmpty()) {
                    User user = UserManager.getInstance().getUser(username);
                    // The user is offline so answer the user's "last available time and the
                    // status message of the last unavailable presence received from the user"
                    long lastActivityTime = presenceManager.getLastActivity(user);
                    if (lastActivityTime > -1) {
                        // Convert it to seconds
                        lastActivityTime = lastActivityTime / 1000;
                    }
                    lastActivity.addAttribute("seconds", String.valueOf(lastActivityTime));
                    String lastStatus = presenceManager.getLastPresenceStatus(user);
                    if (lastStatus != null && lastStatus.length() > 0) {
                        lastActivity.setText(lastStatus);
                    }
                }
                else {
                    // The user is online so answer seconds=0
                    lastActivity.addAttribute("seconds", "0");
                }
            }
            else {
                reply.setError(PacketError.Condition.forbidden);
            }
        }
        catch (UserNotFoundException e) {
            reply.setError(PacketError.Condition.forbidden);
        }
        return reply;
    }

    @Override
	public IQHandlerInfo getInfo() {
        return info;
    }

    public Iterator<String> getFeatures() {
        ArrayList<String> features = new ArrayList<String>();
        features.add("jabber:iq:last");
        return features.iterator();
    }

    @Override
	public void initialize(XMPPServer server) {
        super.initialize(server);
        presenceManager = server.getPresenceManager();
        rosterManager = server.getRosterManager();
    }
}