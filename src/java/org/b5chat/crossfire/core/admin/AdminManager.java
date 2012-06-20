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
package org.b5chat.crossfire.core.admin;

import java.util.ArrayList;
import java.util.List;

import org.b5chat.crossfire.core.property.Globals;
import org.b5chat.crossfire.core.property.PropertyEventDispatcher;
import org.b5chat.crossfire.core.property.PropertyEventListener;
import org.b5chat.crossfire.core.util.ClassUtils;
import org.b5chat.crossfire.xmpp.server.XmppServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

/**
 * The AdminManager manages the IAdminProvider configured for this server, caches knowledge of
 * accounts with admin permissions, and provides a single point of entry for handling
 * getting and setting administrative accounts.
 *
 * The provider can be specified using the system property:
 *
 * <ul>
 * <li><tt>provider.admin.className = my.admin.provider</tt></li>
 * </ul>
 *
 * @author Daniel Henninger
 */
public class AdminManager implements IAdminManager {

	private static final Logger logger = LoggerFactory.getLogger(AdminManager.class);

    /* Cache of admin accounts */
    private List<JID> adminList;
    private IAdminProvider provider;

    /**
     * Constructs a AdminManager, propery listener, and setting up the provider.
     */
    public AdminManager() {
        // Load an admin provider.
        initProvider();

        // Detect when a new admin provider class is set
        PropertyEventListener propListener = new AdminManagerPropertyEventListener(this);
        PropertyEventDispatcher.addListener(propListener);
    }

    /**
     * Initializes the server's admin provider, based on configuration and defaults to
     * DefaultAdminProvider if the specified provider is not valid or not specified.
     */
    protected void initProvider() {
        // Convert XML based provider setup to Database based
        Globals.migrateProperty("provider.admin.className");

        String className = Globals.getProperty("provider.admin.className",
                "org.b5chat.crossfire.core.admin.DefaultAdminProvider");
        // Check if we need to reset the provider class
        if (provider == null || !className.equals(provider.getClass().getName())) {
            try {
                @SuppressWarnings("unchecked")
				Class<IAdminProvider> c = ClassUtils.forName(className);
                provider = c.newInstance();
            }
            catch (Exception e) {
                logger.error("Error loading admin provider: " + className, e);
                provider = new DefaultAdminProvider();
            }
        }
    }

    /**
     * Reads the admin list from the provider and sets up the cache.
     */
    private void loadAdminList() {
        adminList = provider.getAdmins();
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.core.admin.IAdminManager#refreshAdminAccounts()
	 */
    @Override
	public void refreshAdminAccounts() {
        loadAdminList();
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.core.admin.IAdminManager#getAdminAccounts()
	 */
    @Override
	public List<JID> getAdminAccounts() {
        if (adminList == null) {
            loadAdminList();
        }
        return adminList;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.core.admin.IAdminManager#addAdminAccount(java.lang.String)
	 */
    @Override
	public void addAdminAccount(String username) {
        if (adminList == null) {
            loadAdminList();
        }
        JID userJID = XmppServer.getInstance().createJID(username, null);
        if (adminList.contains(userJID)) {
            // Already have them.
            return;
        }
        // Add new admin to cache.
        adminList.add(userJID);
        // Store updated list of admins with provider.
        provider.setAdmins(adminList);
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.core.admin.IAdminManager#addAdminAccount(org.xmpp.packet.JID)
	 */
    @Override
	public void addAdminAccount(JID jid) {
        if (adminList == null) {
            loadAdminList();
        }
        JID bareJID = new JID(jid.toBareJID());
        if (adminList.contains(bareJID)) {
            // Already have them.
            return;
        }
        // Add new admin to cache.
        adminList.add(bareJID);
        // Store updated list of admins with provider.
        provider.setAdmins(adminList);
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.core.admin.IAdminManager#removeAdminAccount(java.lang.String)
	 */
    @Override
	public void removeAdminAccount(String username) {
        if (adminList == null) {
            loadAdminList();
        }
        JID userJID = XmppServer.getInstance().createJID(username, null);
        if (!adminList.contains(userJID)) {
            return;
        }
        // Remove user from admin list cache.
        adminList.remove(userJID);
        // Store updated list of admins with provider.
        provider.setAdmins(adminList);
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.core.admin.IAdminManager#removeAdminAccount(org.xmpp.packet.JID)
	 */
    @Override
	public void removeAdminAccount(JID jid) {
        if (adminList == null) {
            loadAdminList();
        }
        
        JID bareJID = new JID(jid.toBareJID());
        if (!adminList.contains(bareJID)) {
            return;
        }
        // Remove user from admin list cache.
        adminList.remove(bareJID);
        // Store updated list of admins with provider.
        provider.setAdmins(adminList);
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.core.admin.IAdminManager#isUserAdmin(java.lang.String, boolean)
	 */
    @Override
	public boolean isUserAdmin(String username, boolean allowAdminIfEmpty) {
        if (adminList == null) {
            loadAdminList();
        }
        if (allowAdminIfEmpty && adminList.isEmpty()) {
            return "admin".equals(username);
        }
        JID userJID = XmppServer.getInstance().createJID(username, null);
        return adminList.contains(userJID);
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.core.admin.IAdminManager#isUserAdmin(org.xmpp.packet.JID, boolean)
	 */
    @Override
	public boolean isUserAdmin(JID jid, boolean allowAdminIfEmpty) {
        if (adminList == null) {
            loadAdminList();
        }
        if (allowAdminIfEmpty && adminList.isEmpty()) {
            return "admin".equals(jid.getNode());
        }
        JID bareJID = new JID(jid.toBareJID());
        return adminList.contains(bareJID);
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.core.admin.IAdminManager#clearAdminUsers()
	 */
    @Override
	public void clearAdminUsers() {
        // Clear the admin list cache.
        if (adminList == null) {
            adminList = new ArrayList<JID>();
        }
        else {
            adminList.clear();
        }
        // Store empty list of admins with provider.
        provider.setAdmins(adminList);
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.core.admin.IAdminManager#setAdminUsers(java.util.List)
	 */
    @Override
	public void setAdminUsers(List<String> usernames) {
        if (adminList == null) {
            adminList = new ArrayList<JID>();
        }
        else {
            adminList.clear();
        }
        List<JID> admins = new ArrayList<JID>();
        for (String username : usernames) {
            admins.add(XmppServer.getInstance().createJID(username, null));
        }
        adminList.addAll(admins);
        provider.setAdmins(admins);
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.core.admin.IAdminManager#setAdminJIDs(java.util.List)
	 */
    @Override
	public void setAdminJIDs(List<JID> jids) {
        if (adminList == null) {
            adminList = new ArrayList<JID>();
        }
        else {
            adminList.clear();
        }

        List<JID> admins = new ArrayList<JID>();
        for (JID jid : jids)
		{
        	admins.add(new JID(jid.toBareJID()));
		}
        adminList.addAll(admins);
        provider.setAdmins(admins);
    }
}
