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
package org.b5chat.crossfire.xmpp.route;


import org.b5chat.crossfire.core.net.sasl.SASLAuthentication;
import org.b5chat.crossfire.xmpp.UnknownStanzaException;
import org.b5chat.crossfire.xmpp.server.XmppServer;
import org.b5chat.crossfire.xmpp.session.LocalClientSession;
import org.dom4j.Element;
import org.xmpp.packet.*;

import java.io.UnsupportedEncodingException;

/**
 * Handles the routing of packets to a particular session. It will invoke all of the appropriate
 * interceptors, before and after having the server process the message.
 *
 * @author Alexander Wenckus
 */
public class SessionPacketRouter implements IPacketRouter {

    private LocalClientSession session;
    private IPacketRouter router;
    private boolean skipJIDValidation = false;

    public SessionPacketRouter(LocalClientSession session) {
        this.session = session;
        router = XmppServer.getInstance().getPacketRouter();
    }

    /**
     * Sets if TO addresses of Elements being routed should be validated. Doing stringprep operations
     * is very expensive and sometimes we already validated the TO address so there is no need to
     * validate again the address. For instance, when using IConnection Managers the validation
     * is done by the IConnection Manager so we can just trust the TO address. On the other hand,
     * the FROM address is set by the server so there is no need to validate it.<p>
     *
     * By default validation is enabled.
     *
     * @param skipJIDValidation true if validation of TO address is enabled.
     */
    public void setSkipJIDValidation(boolean skipJIDValidation) {
        this.skipJIDValidation = skipJIDValidation;
    }

    public void route(Element wrappedElement)
            throws UnsupportedEncodingException, UnknownStanzaException {
        String tag = wrappedElement.getName();
        if ("auth".equals(tag) || "response".equals(tag)) {
            SASLAuthentication.handle(session, wrappedElement);
        }
        else if ("iq".equals(tag)) {
            route(getIQ(wrappedElement));
        }
        else if ("message".equals(tag)) {
            route(new Message(wrappedElement, skipJIDValidation));
        }
        else if ("presence".equals(tag)) {
            route(new Presence(wrappedElement, skipJIDValidation));
        }
        else {
            throw new UnknownStanzaException();
        }
    }

    private IQ getIQ(Element doc) {
        Element query = doc.element("query");
        if (query != null && "jabber:iq:roster".equals(query.getNamespaceURI())) {
            return new Roster(doc);
        }
        else {
            return new IQ(doc, skipJIDValidation);
        }
    }

    public void route(Packet packet) {
        // Security: Don't allow users to send packets on behalf of other users
        packet.setFrom(session.getAddress());
        if(packet instanceof IQ) {
            route((IQ)packet);
        }
        else if(packet instanceof Message) {
            route((Message)packet);
        }
        else if(packet instanceof Presence) {
            route((Presence)packet);
        }
    }

    public void route(IQ packet) {
        packet.setFrom(session.getAddress());
        router.route(packet);
        session.incrementClientPacketCount();
    }

    public void route(Message packet) {
        packet.setFrom(session.getAddress());
        router.route(packet);
        session.incrementClientPacketCount();
    }

    public void route(Presence packet) {
        packet.setFrom(session.getAddress());
        router.route(packet);
        session.incrementClientPacketCount();
    }
}
