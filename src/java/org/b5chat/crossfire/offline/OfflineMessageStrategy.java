/**
 * $RCSfile: OfflineMessageStrategy.java,v $
 * $Revision: 3114 $
 * $Date: 2005-11-23 18:12:54 -0300 (Wed, 23 Nov 2005) $
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

package org.b5chat.crossfire.offline;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


import org.b5chat.crossfire.core.container.BasicModule;
import org.b5chat.crossfire.core.property.Globals;
import org.b5chat.crossfire.privacy.PrivacyList;
import org.b5chat.crossfire.privacy.PrivacyListManager;
import org.b5chat.crossfire.route.IPacketRouter;
import org.b5chat.crossfire.server.XmppServer;
import org.b5chat.crossfire.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError;

/**
 * Controls what is done with offline messages.
 *
 * @author Iain Shigeoka
 */
public class OfflineMessageStrategy extends BasicModule {

	private static final Logger Log = LoggerFactory.getLogger(OfflineMessageStrategy.class);

    private static int quota = 100*1024; // Default to 100 K.
    private static Type type = Type.store_and_bounce;

    private static List<IOfflineMessageListener> listeners = new CopyOnWriteArrayList<IOfflineMessageListener>();

    private OfflineMessageStore messageStore;
    private JID serverAddress;
    private IPacketRouter router;

    public OfflineMessageStrategy() {
        super("Offline Message Strategy");
    }

    public int getQuota() {
        return quota;
    }

    public void setQuota(int quota) {
        OfflineMessageStrategy.quota = quota;
        Globals.setProperty("xmpp.offline.quota", Integer.toString(quota));
    }

    public OfflineMessageStrategy.Type getType() {
        return type;
    }

    public void setType(OfflineMessageStrategy.Type type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }
        OfflineMessageStrategy.type = type;
        Globals.setProperty("xmpp.offline.type", type.toString());
    }

    public void storeOffline(Message message) {
        if (message != null) {
            // Do nothing if the message was sent to the server itself, an anonymous user or a non-existent user
            JID recipientJID = message.getTo();
            if (recipientJID == null || serverAddress.equals(recipientJID) ||
                    recipientJID.getNode() == null ||
                    !UserManager.getInstance().isRegisteredUser(recipientJID.getNode())) {
                return;
            }
            // Do not store messages of type groupchat, error or headline as specified in JEP-160
            if (Message.Type.groupchat == message.getType() ||
                    Message.Type.error == message.getType() ||
                    Message.Type.headline == message.getType()) {
                return;
            }
            // Do not store messages if communication is blocked
            PrivacyList list =
                    PrivacyListManager.getInstance().getDefaultPrivacyList(recipientJID.getNode());
            if (list != null && list.shouldBlockPacket(message)) {
                return;
            }

            if (type == Type.bounce) {
                bounce(message);
            }
            else if (type == Type.store) {
                store(message);
            }
            else if (type == Type.store_and_bounce) {
                if (underQuota(message)) {
                    store(message);
                }
                else {
                    bounce(message);
                }
            }
            else if (type == Type.store_and_drop) {
                if (underQuota(message)) {
                    store(message);
                }
            }
        }
    }

    /**
     * Registers a listener to receive events.
     *
     * @param listener the listener.
     */
    public static void addListener(IOfflineMessageListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        listeners.add(listener);
    }

    /**
     * Unregisters a listener to receive events.
     *
     * @param listener the listener.
     */
    public static void removeListener(IOfflineMessageListener listener) {
        listeners.remove(listener);
    }

    private boolean underQuota(Message message) {
        return quota > messageStore.getSize(message.getTo().getNode()) + message.toXML().length();
    }

    private void store(Message message) {
        messageStore.addMessage(message);
        // Inform listeners that an offline message was stored
        if (!listeners.isEmpty()) {
            for (IOfflineMessageListener listener : listeners) {
                listener.messageStored(message);
            }
        }
    }

    private void bounce(Message message) {
        // Do nothing if the sender was the server itself
        if (message.getFrom() == null) {
            return;
        }
        try {
            // Generate a rejection response to the sender
            Message errorResponse = message.createCopy();
            errorResponse.setError(new PacketError(PacketError.Condition.item_not_found,
                    PacketError.Type.continue_processing));
            errorResponse.setFrom(message.getTo());
            errorResponse.setTo(message.getFrom());
            // Send the response
            router.route(errorResponse);
            // Inform listeners that an offline message was bounced
            if (!listeners.isEmpty()) {
                for (IOfflineMessageListener listener : listeners) {
                    listener.messageBounced(message);
                }
            }
        }
        catch (Exception e) {
            Log.error(e.getMessage(), e);
        }
    }

    @Override
	public void initialize(XmppServer server) {
        super.initialize(server);
        messageStore = server.getOfflineMessageStore();
        router = server.getPacketRouter();
        serverAddress = new JID(server.getServerInfo().getXMPPDomain());

        String quota = Globals.getProperty("xmpp.offline.quota");
        if (quota != null && quota.length() > 0) {
            OfflineMessageStrategy.quota = Integer.parseInt(quota);
        }
        String type = Globals.getProperty("xmpp.offline.type");
        if (type != null && type.length() > 0) {
            OfflineMessageStrategy.type = Type.valueOf(type);
        }
    }

    /**
     * Strategy types.
     */
    public enum Type {

        /**
         * All messages are bounced to the sender.
         */
        bounce,

        /**
         * All messages are silently dropped.
         */
        drop,

        /**
         * All messages are stored.
         */
        store,

        /**
         * Messages are stored up to the storage limit, and then bounced.
         */
        store_and_bounce,

        /**
         * Messages are stored up to the storage limit, and then silently dropped.
         */
        store_and_drop
    }
}