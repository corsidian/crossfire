/**
 * $RCSfile$
 * $Revision: 1761 $
 * $Date: 2005-08-09 19:34:09 -0300 (Tue, 09 Aug 2005) $
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

package org.b5chat.crossfire.handler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;


import org.b5chat.crossfire.IQHandlerInfo;
import org.b5chat.crossfire.OfflineMessage;
import org.b5chat.crossfire.OfflineMessageStore;
import org.b5chat.crossfire.RoutingTable;
import org.b5chat.crossfire.XMPPServer;
import org.b5chat.crossfire.auth.UnauthorizedException;
import org.b5chat.crossfire.disco.DiscoInfoProvider;
import org.b5chat.crossfire.disco.DiscoItem;
import org.b5chat.crossfire.disco.DiscoItemsProvider;
import org.b5chat.crossfire.disco.IQDiscoInfoHandler;
import org.b5chat.crossfire.disco.IQDiscoItemsHandler;
import org.b5chat.crossfire.disco.ServerFeaturesProvider;
import org.b5chat.crossfire.session.LocalClientSession;
import org.b5chat.crossfire.user.UserManager;
import org.b5chat.util.GlobalConstants;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.forms.DataForm;
import org.xmpp.forms.FormField;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

/**
 * Implements JEP-0013: Flexible Offline Message Retrieval. Allows users to request number of
 * messages, request message headers, retrieve specific messages, remove specific messages,
 * retrieve all messages and remove all messages.
 *
 * @author Gaston Dombiak
 */
public class IQOfflineMessagesHandler extends IQHandler implements ServerFeaturesProvider,
        DiscoInfoProvider, DiscoItemsProvider {

	private static final Logger Log = LoggerFactory.getLogger(IQOfflineMessagesHandler.class);

    private static final String NAMESPACE = "http://jabber.org/protocol/offline";

    final private SimpleDateFormat dateFormat =
            new SimpleDateFormat(GlobalConstants.XMPP_DATETIME_FORMAT);
    private IQHandlerInfo info;
    private IQDiscoInfoHandler infoHandler;
    private IQDiscoItemsHandler itemsHandler;

    private RoutingTable routingTable;
    private UserManager userManager;
    private OfflineMessageStore messageStore;

    public IQOfflineMessagesHandler() {
        super("Flexible Offline Message Retrieval Handler");
        info = new IQHandlerInfo("offline", NAMESPACE);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
        IQ reply = IQ.createResultIQ(packet);
        Element offlineRequest = packet.getChildElement();

        JID from = packet.getFrom();
        if (offlineRequest.element("purge") != null) {
            // User requested to delete all offline messages
            messageStore.deleteMessages(from.getNode());
        }
        else if (offlineRequest.element("fetch") != null) {
            // Mark that offline messages shouldn't be sent when the user becomes available
            stopOfflineFlooding(from);
            // User requested to receive all offline messages
            for (OfflineMessage offlineMessage : messageStore.getMessages(from.getNode(), false)) {
                sendOfflineMessage(from, offlineMessage);
            }
        }
        else {
            for (Iterator it = offlineRequest.elementIterator("item"); it.hasNext();) {
                Element item = (Element) it.next();
                Date creationDate = null;
                synchronized (dateFormat) {
                    try {
                        creationDate = dateFormat.parse(item.attributeValue("node"));
                    }
                    catch (ParseException e) {
                        Log.error("Error parsing date", e);
                    }
                }
                if ("view".equals(item.attributeValue("action"))) {
                    // User requested to receive specific message
                    OfflineMessage offlineMsg = messageStore.getMessage(from.getNode(), creationDate);
                    if (offlineMsg != null) {
                        sendOfflineMessage(from, offlineMsg);
                    }
                }
                else if ("remove".equals(item.attributeValue("action"))) {
                    // User requested to delete specific message
                    messageStore.deleteMessage(from.getNode(), creationDate);
                }
            }
        }
        return reply;
    }

    private void sendOfflineMessage(JID receipient, OfflineMessage offlineMessage) {
        Element offlineInfo = offlineMessage.addChildElement("offline", NAMESPACE);
        synchronized (dateFormat) {
            offlineInfo.addElement("item").addAttribute("node",
                    dateFormat.format(offlineMessage.getCreationDate()));
        }
        routingTable.routePacket(receipient, offlineMessage, true);
    }

    @Override
	public IQHandlerInfo getInfo() {
        return info;
    }

    public Iterator<String> getFeatures() {
        ArrayList<String> features = new ArrayList<String>();
        features.add(NAMESPACE);
        return features.iterator();
    }

    public Iterator<Element> getIdentities(String name, String node, JID senderJID) {
        ArrayList<Element> identities = new ArrayList<Element>();
        Element identity = DocumentHelper.createElement("identity");
        identity.addAttribute("category", "automation");
        identity.addAttribute("type", "message-list");
        identities.add(identity);
        return identities.iterator();
    }

    public Iterator<String> getFeatures(String name, String node, JID senderJID) {
        return Arrays.asList(NAMESPACE).iterator();
    }

    public DataForm getExtendedInfo(String name, String node, JID senderJID) {
        // Mark that offline messages shouldn't be sent when the user becomes available
        stopOfflineFlooding(senderJID);

        final DataForm dataForm = new DataForm(DataForm.Type.result);

        final FormField field1 = dataForm.addField();
        field1.setVariable("FORM_TYPE");
        field1.setType(FormField.Type.hidden);
        field1.addValue(NAMESPACE);

        final FormField field2 = dataForm.addField();
        field2.setVariable("number_of_messages");
        field2.addValue(String.valueOf(messageStore.getMessages(senderJID.getNode(), false).size()));

        return dataForm;
    }

    public boolean hasInfo(String name, String node, JID senderJID) {
        return NAMESPACE.equals(node) && userManager.isRegisteredUser(senderJID.getNode());
    }

    public Iterator<DiscoItem> getItems(String name, String node, JID senderJID) {
        // Mark that offline messages shouldn't be sent when the user becomes available
        stopOfflineFlooding(senderJID);
        List<DiscoItem> answer = new ArrayList<DiscoItem>();
        for (OfflineMessage offlineMessage : messageStore.getMessages(senderJID.getNode(), false)) {
            synchronized (dateFormat) {
                answer.add(new DiscoItem(new JID(senderJID.toBareJID()), offlineMessage.getFrom().toString(), dateFormat.format(offlineMessage.getCreationDate()), null));
            }
        }

        return answer.iterator();
    }

    @Override
	public void initialize(XMPPServer server) {
        super.initialize(server);
        infoHandler = server.getIQDiscoInfoHandler();
        itemsHandler = server.getIQDiscoItemsHandler();
        messageStore = server.getOfflineMessageStore();
        userManager = server.getUserManager();
        routingTable = server.getRoutingTable();
    }

    @Override
	public void start() throws IllegalStateException {
        super.start();
        infoHandler.setServerNodeInfoProvider(NAMESPACE, this);
        itemsHandler.setServerNodeInfoProvider(NAMESPACE, this);
    }

    @Override
	public void stop() {
        super.stop();
        infoHandler.removeServerNodeInfoProvider(NAMESPACE);
        itemsHandler.removeServerNodeInfoProvider(NAMESPACE);
    }

    private void stopOfflineFlooding(JID senderJID) {
        LocalClientSession session = (LocalClientSession) sessionManager.getSession(senderJID);
        if (session != null) {
            session.setOfflineFloodStopped(true);
        }
    }
}