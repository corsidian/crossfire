/**
 * $Revision: 1727 $
 * $Date: 2005-07-29 19:55:59 -0300 (Fri, 29 Jul 2005) $
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

package org.b5chat.crossfire.core.server;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.b5chat.crossfire.xmpp.server.XmppServer;

/**
 * An ServerContextListener starts an XmppServer when a ServletContext is initialized and stops
 * the xmpp server when the servlet context is destroyed.
 *
 * @author evrim ulu
 * @author Gaston Dombiak
 */
public class ServerContextListener implements ServletContextListener {

    protected String XMPP_KEY = "XMPP_SERVER";

    public void contextInitialized(ServletContextEvent event) {
    	XmppServer server = (XmppServer) event.getServletContext().getAttribute(XMPP_KEY);
    	if (server != null) {
    		return;
    	}
    	server = new XmppServer();
        event.getServletContext().setAttribute(XMPP_KEY, server);
    }

    public void contextDestroyed(ServletContextEvent event) {
        XmppServer server = (XmppServer) event.getServletContext().getAttribute(XMPP_KEY);
        if (null != server) {
            server.stop();
        }
    }

}
