/**
 * $RCSfile: RosterManager.java,v $
 * $Revision: 3138 $
 * $Date: 2005-12-01 02:13:26 -0300 (Thu, 01 Dec 2005) $
 *
 * Copyright (C) 2004-2008 B5Chat Community. All rights reserved.
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

package org.b5chat.crossfire.roster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;


import org.b5chat.crossfire.JIDFactory;
import org.b5chat.crossfire.PacketRouter;
import org.b5chat.crossfire.PresenceManager;
import org.b5chat.crossfire.RoutingTable;
import org.b5chat.crossfire.SessionManager;
import org.b5chat.crossfire.SharedGroupException;
import org.b5chat.crossfire.group.Group;
import org.b5chat.crossfire.group.GroupManager;
import org.b5chat.crossfire.group.GroupNotFoundException;
import org.b5chat.crossfire.user.UserManager;
import org.b5chat.crossfire.user.UserNameManager;
import org.b5chat.database.DbConnectionManager;
import org.b5chat.util.cache.Cache;
import org.b5chat.util.cache.CacheFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Presence;

/**
 * A simple service that allows components to retrieve a roster based solely on the ID
 * of the owner. Users have convenience methods for obtaining a roster associated with
 * the owner. However there are many components that need to retrieve the roster
 * based solely on the generic ID owner key. This interface defines a service that can
 * do that. This allows classes that generically manage resource for resource owners
 * (such as presence updates) to generically offer their services without knowing or
 * caring if the roster owner is a user, chatbot, etc.
 *
 * @author Iain Shigeoka
 */
public class RosterManager implements IRosterManager {	

	private Cache<String, IRoster> rosterCache;

	private JIDFactory jidFactory;
    private RoutingTable routingTable;
    private RosterItemProvider rosterItemProvider;
    private RosterEventDispatcher rosterEventDispatcher;
    private PresenceManager presenceManager;
    private SessionManager sessionManager;
    private PacketRouter packetRouter;
    private GroupManager groupManager;
    private UserNameManager userNameManager;
    private UserManager userManager;
    
    public RosterManager(JIDFactory jidFactory, RoutingTable routingTable, 
    		PresenceManager presenceManager, SessionManager sessionManager, 
    		PacketRouter packetRouter, GroupManager groupManager, 
    		UserManager userManager, UserNameManager userNameManager,
    		DbConnectionManager dbConnectionManager) {
    
        rosterCache = CacheFactory.createCache("Roster");

    	this.jidFactory = jidFactory;
    	this.routingTable = routingTable;
    	this.packetRouter = packetRouter;
    	this.sessionManager = sessionManager;
    	this.presenceManager = presenceManager;
    	this.userNameManager = userNameManager;
    	this.groupManager = groupManager;
    	this.userManager = userManager;
    
    	rosterItemProvider = new RosterItemProvider(userNameManager, groupManager, dbConnectionManager);
    	rosterEventDispatcher = new RosterEventDispatcher();
    }

    // temp until refactor to move roster item provider under manager
    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterManager#getRosterItemProvider()
	 */
    @Override
	public RosterItemProvider getRosterItemProvider() {
    	return rosterItemProvider;
    }
    
    // temp until refactor
    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterManager#getRosterEventDispatcher()
	 */
    @Override
	public RosterEventDispatcher getRosterEventDispatcher() {
    	return rosterEventDispatcher;
    }
    
    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterManager#getRoster(java.lang.String)
	 */
    @Override
	public IRoster getRoster(String username)  {
        IRoster roster = rosterCache.get(username);
        if (roster == null) {
            // Synchronize using a unique key so that other threads loading the User
            // and not the Roster cannot produce a deadlock
            synchronized ((username + " ro").intern()) {
                roster = rosterCache.get(username);
                if (roster == null) {
                    // Not in cache so load a new one:
                    roster = new Roster(username, presenceManager, this, 
                    		sessionManager, routingTable, 
                    		jidFactory, packetRouter, groupManager, userNameManager);
                    rosterCache.put(username, roster);
                }
            }
        }
        return roster;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterManager#deleteRoster(org.xmpp.packet.JID)
	 */
    @Override
	public void deleteRoster(JID user) {
        if (!jidFactory.isLocal(user)) {
            // Ignore request if user is not a local user
            return;
        }
        try {
            String username = user.getNode();
            // Get the roster of the deleted user
            IRoster rosterImpl = getRoster(username);
            // Remove each roster item from the user's roster
            for (IRosterItem item : rosterImpl.getRosterItems()) {
                try {
                    rosterImpl.deleteRosterItem(item.getJid(), false);
                }
                catch (SharedGroupException e) {
                    // Do nothing. We shouldn't have this exception since we disabled the checkings
                }
            }
            // Remove the cached roster from memory
            rosterCache.remove(username);

            // Get the rosters that have a reference to the deleted user
            Iterator<String> usernames = rosterItemProvider.getUsernames(user.toBareJID());
            while (usernames.hasNext()) {
                username = usernames.next();
                try {
                    // Get the roster that has a reference to the deleted user
                    rosterImpl = getRoster(username);
                    // Remove the deleted user reference from this roster
                    rosterImpl.deleteRosterItem(user, false);
                }
                catch (SharedGroupException e) {
                    // Do nothing. We shouldn't have this exception since we disabled the checkings
                }
            }
        }
        catch (UnsupportedOperationException e) {
            // Do nothing
        }
    }
    
    // Used by clustering code.. shouldn't write to database on each node of cluster
    // this needs to be rewritten to local vs remote rosters
    public void addRoster(IRoster roster) {
        rosterCache.put(roster.getUsername(), roster);
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterManager#getSharedGroups(java.lang.String)
	 */
    @Override
	public Collection<Group> getSharedGroups(String username) {
        Collection<Group> answer = new HashSet<Group>();
        Collection<Group> groups = groupManager.getSharedGroups();
        for (Group group : groups) {
            String showInRoster = group.getProperties().get("sharedRoster.showInRoster");
            if ("onlyGroup".equals(showInRoster)) {
                if (group.isUser(username)) {
                    // The user belongs to the group so add the group to the answer
                    answer.add(group);
                }
                else {
                    // Check if the user belongs to a group that may see this group
                    Collection<Group> groupList = parseGroups(group.getProperties().get("sharedRoster.groupList"));
                    for (Group groupInList : groupList) {
                        if (groupInList.isUser(username)) {
                            answer.add(group);
                        }
                    }
                }
            }
            else if ("everybody".equals(showInRoster)) {
                // Anyone can see this group so add the group to the answer
                answer.add(group);
            }
        }
        return answer;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterManager#getPublicSharedGroups()
	 */
    @Override
	public Collection<Group> getPublicSharedGroups() {
        Collection<Group> answer = new HashSet<Group>();
        Collection<Group> groups = groupManager.getSharedGroups();
        for (Group group : groups) {
            String showInRoster = group.getProperties().get("sharedRoster.showInRoster");
            if ("everybody".equals(showInRoster)) {
                // Anyone can see this group so add the group to the answer
                answer.add(group);
            }
        }
        return answer;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterManager#parseGroups(java.lang.String)
	 */
    @Override
	public Collection<Group> parseGroups(String groupNames) {
        Collection<Group> answer = new HashSet<Group>();
        for (String groupName : parseGroupNames(groupNames)) {
            try {
                answer.add(groupManager.getGroup(groupName));
            }
            catch (GroupNotFoundException e) {
                // Do nothing. Silently ignore the invalid reference to the group
            }
        }
        return answer;
    }

    /**
     * Returns a collection of Groups obtained by parsing a comma delimited String with the name
     * of groups.
     *
     * @param groupNames a comma delimited string with group names.
     * @return a collection of Groups obtained by parsing a comma delimited String with the name
     *         of groups.
     */
    private static Collection<String> parseGroupNames(String groupNames) {
        Collection<String> answer = new HashSet<String>();
        if (groupNames != null) {
            StringTokenizer tokenizer = new StringTokenizer(groupNames, ",");
            while (tokenizer.hasMoreTokens()) {
                answer.add(tokenizer.nextToken());
            }
        }
        return answer;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterManager#isSharedGroup(org.b5chat.crossfire.group.Group)
	 */
    @Override
	public boolean isSharedGroup(Group group) {
        String showInRoster = group.getProperties().get("sharedRoster.showInRoster");
        if ("onlyGroup".equals(showInRoster) || "everybody".equals(showInRoster)) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if the specified Group may be seen by all users in the system. The decision
     * is made based on the group properties that are configurable through the Admin Console.
     *
     * @param group the group to check if it may be seen by all users in the system.
     * @return true if the specified Group may be seen by all users in the system.
     */
    public static boolean isPublicSharedGroup(Group group) {
        String showInRoster = group.getProperties().get("sharedRoster.showInRoster");
        if ("everybody".equals(showInRoster)) {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterManager#sendSubscribeRequest(org.xmpp.packet.JID, org.xmpp.packet.JID, boolean)
	 */
    @Override
	public void sendSubscribeRequest(JID sender, JID recipient, boolean isSubscribe) {
        Presence presence = new Presence();
        presence.setFrom(sender);
        presence.setTo(recipient);
        if (isSubscribe) {
            presence.setType(Presence.Type.subscribe);
        }
        else {
            presence.setType(Presence.Type.unsubscribe);
        }
        routingTable.routePacket(recipient, presence, false);
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterManager#getVisibleGroups(org.b5chat.crossfire.group.Group)
	 */
    @Override
	public Collection<Group> getVisibleGroups(Group groupToCheck) {
        Collection<Group> answer = new HashSet<Group>();
        Collection<Group> groups = groupManager.getSharedGroups();
        for (Group group : groups) {
            if (group.equals(groupToCheck)) {
                continue;
            }
            String showInRoster = group.getProperties().get("sharedRoster.showInRoster");
            if ("onlyGroup".equals(showInRoster)) {
                // Check if the user belongs to a group that may see this group
                Collection<String> groupList =
                        parseGroupNames(group.getProperties().get("sharedRoster.groupList"));
                if (groupList.contains(groupToCheck.getName())) {
                    answer.add(group);
                }
            }
            else if ("everybody".equals(showInRoster)) {
                answer.add(group);
            }
        }
        return answer;
    }

    /**
     * Returns true if a given group is visible to a given user. That means, if the user can
     * see the group in his roster.
     *
     * @param group the group to check if the user can see.
     * @param user the JID of the user to check if he may see the group.
     * @return true if a given group is visible to a given user.
     */
    public boolean isGroupVisible(Group group, JID user) {
        String showInRoster = group.getProperties().get("sharedRoster.showInRoster");
        if ("everybody".equals(showInRoster)) {
            return true;
        }
        else if ("onlyGroup".equals(showInRoster)) {
            if (group.isUser(user)) {
                 return true;
            }
            // Check if the user belongs to a group that may see this group
            Collection<Group> groupList = parseGroups(group.getProperties().get(
                    "sharedRoster.groupList"));
            for (Group groupInList : groupList) {
                if (groupInList.isUser(user)) {
                    return true;
                }
            }
        }
        return false;
    }

    
    public Collection<JID> getSharedUsersForRoster(Group group, IRoster roster) {
        String showInRoster = group.getProperties().get("sharedRoster.showInRoster");
        String groupNames = group.getProperties().get("sharedRoster.groupList");
        
        // Answer an empty collection if the group is not being shown in users' rosters
        if (!"onlyGroup".equals(showInRoster) && !"everybody".equals(showInRoster)) {
            return new ArrayList<JID>();
        }
        
        // Add the users of the group
        Collection<JID> users = new HashSet<JID>(group.getMembers());
        users.addAll(group.getAdmins());
        
        // If the user of the roster belongs to the shared group then we should return
        // users that need to be in the roster with subscription "from"
        if (group.isUser(roster.getUsername())) {
            // Check if anyone can see this shared group
            if ("everybody".equals(showInRoster)) {
                // Add all users in the system
                for (String username : userManager.getUsernames()) {
                    users.add(jidFactory.createJID(username, null, true));
                }
            }
            else {
                // Add the users that may see the group
                Collection<Group> groupList = parseGroups(groupNames);
                for (Group groupInList : groupList) {
                    users.addAll(groupInList.getMembers());
                    users.addAll(groupInList.getAdmins());
                }
            }
        }
        return users;
    }

    /**
     * Returns true if a group in the first collection may mutually see a group of the
     * second collection. More precisely, return true if both collections contain a public
     * group (i.e. anybody can see the group) or if both collection have a group that may see
     * each other and the users are members of those groups or if one group is public and the
     * other group allowed the public group to see it.
     *
     * @param user the name of the user associated to the first collection of groups. This is always a local user.
     * @param groups a collection of groups to check against the other collection of groups.
     * @param otherUser the JID of the user associated to the second collection of groups.
     * @param otherGroups the other collection of groups to check against the first collection.
     * @return true if a group in the first collection may mutually see a group of the
     *         second collection.
     */
    public boolean hasMutualVisibility(String user, Collection<Group> groups, JID otherUser,
            Collection<Group> otherGroups) {
        for (Group group : groups) {
            for (Group otherGroup : otherGroups) {
                // Skip this groups if the users are not group users of the groups
                if (!group.isUser(user) || !otherGroup.isUser(otherUser)) {
                    continue;
                }
                if (group.equals(otherGroup)) {
                     return true;
                }
                String showInRoster = group.getProperties().get("sharedRoster.showInRoster");
                String otherShowInRoster = otherGroup.getProperties().get("sharedRoster.showInRoster");
                // Return true if both groups are public groups (i.e. anybody can see them)
                if ("everybody".equals(showInRoster) && "everybody".equals(otherShowInRoster)) {
                    return true;
                }
                else if ("onlyGroup".equals(showInRoster) && "onlyGroup".equals(otherShowInRoster)) {
                    String groupNames = group.getProperties().get("sharedRoster.groupList");
                    String otherGroupNames = otherGroup.getProperties().get("sharedRoster.groupList");
                    // Return true if each group may see the other group
                    if (groupNames != null && otherGroupNames != null) {
                        if (groupNames.contains(otherGroup.getName()) &&
                                otherGroupNames.contains(group.getName())) {
                            return true;
                        }
                        // Check if each shared group can be seen by a group where each user belongs
                        Collection<Group> groupList = parseGroups(groupNames);
                        Collection<Group> otherGroupList = parseGroups(otherGroupNames);
                        for (Group groupName : groupList) {
                            if (groupName.isUser(otherUser)) {
                                for (Group otherGroupName : otherGroupList) {
                                    if (otherGroupName.isUser(user)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
                else if ("everybody".equals(showInRoster) && "onlyGroup".equals(otherShowInRoster)) {
                    // Return true if one group is public and the other group allowed the public
                    // group to see him
                    String otherGroupNames = otherGroup.getProperties().get("sharedRoster.groupList");
                    if (otherGroupNames != null && otherGroupNames.contains(group.getName())) {
                            return true;
                    }
                }
                else if ("onlyGroup".equals(showInRoster) && "everybody".equals(otherShowInRoster)) {
                    // Return true if one group is public and the other group allowed the public
                    // group to see him
                    String groupNames = group.getProperties().get("sharedRoster.groupList");
                    // Return true if each group may see the other group
                    if (groupNames != null && groupNames.contains(otherGroup.getName())) {
                            return true;
                    }
                }
            }
        }
        return false;
    }

}
