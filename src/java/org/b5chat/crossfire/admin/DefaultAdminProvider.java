/**
 * $Revision$
 * $Date$
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
package org.b5chat.crossfire.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


import org.b5chat.crossfire.core.property.PropertyEventDispatcher;
import org.b5chat.crossfire.core.property.PropertyEventListener;
import org.b5chat.crossfire.server.XmppServer;
import org.b5chat.util.Globals;
import org.b5chat.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

/**
 * Handles default management of admin users, which stores the list if accounts as a system property.
 *
 * @author Daniel Henninger
 */
public class DefaultAdminProvider implements IAdminProvider {

	private static final Logger Log = LoggerFactory.getLogger(DefaultAdminProvider.class);

    /**
     * Constructs a new DefaultAdminProvider
     */
    public DefaultAdminProvider() {

        // Convert old crossfire.xml style to new provider style, if necessary.
        Log.debug("DefaultAdminProvider: Convert XML to provider.");
        convertXMLToProvider();
    }

    /**
     * The default provider retrieves the comma separated list from the system property
     * <tt>admin.authorizedJIDs</tt>
     * @see org.b5chat.crossfire.admin.IAdminProvider#getAdmins()
     */
    public List<JID> getAdmins() {
        List<JID> adminList = new ArrayList<JID>();

        // Add bare JIDs of users that are admins (may include remote users), primarily used to override/add to list of admin users
        String jids = Globals.getProperty("admin.authorizedJIDs");
        jids = (jids == null || jids.trim().length() == 0) ? "" : jids;
        StringTokenizer tokenizer = new StringTokenizer(jids, ",");
        while (tokenizer.hasMoreTokens()) {
            String jid = tokenizer.nextToken().toLowerCase().trim();
            try {
                adminList.add(new JID(jid));
            }
            catch (IllegalArgumentException e) {
                Log.warn("Invalid JID found in admin.authorizedJIDs system property: " + jid, e);
            }
        }

        if (adminList.isEmpty()) {
            // Add default admin account when none was specified
            adminList.add(new JID("admin", XmppServer.getInstance().getServerInfo().getXMPPDomain(), null, true));
        }

        return adminList;
    }

    /**
     * The default provider sets a comma separated list as the system property
     * <tt>admin.authorizedJIDs</tt>
     * @see org.b5chat.crossfire.admin.IAdminProvider#setAdmins(java.util.List)
     */
    public void setAdmins(List<JID> admins) {
        Collection<String> adminList = new ArrayList<String>();
        for (JID admin : admins) {
            adminList.add(admin.toBareJID());
        }
        Globals.setProperty("admin.authorizedJIDs", StringUtils.collectionToString(adminList));
    }

    /**
     * The default provider is not read only
     * @see org.b5chat.crossfire.admin.IAdminProvider#isReadOnly()
     */
    public boolean isReadOnly() {
        return false;
    }

    /**
     * Converts the old crossfire.xml style admin list to use the new provider mechanism.
     */
    private void convertXMLToProvider() {
        if (Globals.getXMLProperty("admin.authorizedJIDs") == null &&
                Globals.getXMLProperty("admin.authorizedUsernames") == null &&
                Globals.getXMLProperty("adminConsole.authorizedUsernames") == null) {
            // No settings in crossfire.xml.
            return;
        }

        List<JID> adminList = new ArrayList<JID>();

        // Add bare JIDs of users that are admins (may include remote users), primarily used to override/add to list of admin users
        String jids = Globals.getXMLProperty("admin.authorizedJIDs");
        jids = (jids == null || jids.trim().length() == 0) ? "" : jids;
        StringTokenizer tokenizer = new StringTokenizer(jids, ",");
        while (tokenizer.hasMoreTokens()) {
            String jid = tokenizer.nextToken().toLowerCase().trim();
            try {
                adminList.add(new JID(jid));
            }
            catch (IllegalArgumentException e) {
                Log.warn("Invalid JID found in authorizedJIDs at crossfire.xml: " + jid, e);
            }
        }

        // Add the JIDs of the local users that are admins, primarily used to override/add to list of admin users
        String usernames = Globals.getXMLProperty("admin.authorizedUsernames");
        if (usernames == null) {
            // Fall back to old method for defining admins (i.e. using adminConsole prefix
            usernames = Globals.getXMLProperty("adminConsole.authorizedUsernames");
        }
        // Add default of admin user if no other users were listed as admins.
        usernames = (usernames == null || usernames.trim().length() == 0) ? (adminList.size() == 0 ? "admin" : "") : usernames;
        tokenizer = new StringTokenizer(usernames, ",");
        while (tokenizer.hasMoreTokens()) {
            String username = tokenizer.nextToken();
            try {
                adminList.add(XmppServer.getInstance().createJID(username.toLowerCase().trim(), null));
            }
            catch (IllegalArgumentException e) {
                // Ignore usernames that when appended @server.com result in an invalid JID
                Log.warn("Invalid username found in authorizedUsernames at crossfire.xml: " +
                        username, e);
            }
        }
        setAdmins(adminList);

        // Clear out old XML property settings
        Globals.deleteXMLProperty("admin.authorizedJIDs");
        Globals.deleteXMLProperty("admin.authorizedUsernames");
        Globals.deleteXMLProperty("adminConsole.authorizedUsernames");
    }

}
