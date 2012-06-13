package org.b5chat.crossfire.roster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;


import org.b5chat.crossfire.JIDFactory;
import org.b5chat.crossfire.event.GroupEventListener;
import org.b5chat.crossfire.group.Group;
import org.b5chat.crossfire.group.GroupManager;
import org.b5chat.crossfire.user.UserManager;
import org.b5chat.crossfire.user.UserNotFoundException;
import org.xmpp.packet.JID;

public class RosterGroupEventListener implements GroupEventListener {
	private IRosterManager rosterManager;
	private JIDFactory jidFactory;
	private UserManager userManager;
	private GroupManager groupManager;
	
	public RosterGroupEventListener(IRosterManager rosterManager, JIDFactory jidFactory, 
			GroupManager groupManager, UserManager userManager) {
		this.rosterManager = rosterManager;
		this.jidFactory = jidFactory;
		this.groupManager = groupManager;
		this.userManager = userManager;
	}

	public void groupCreated(Group group, Map params) {
        //Do nothing
    }

    public void groupDeleting(Group group, Map params) {
        // Get group members
        Collection<JID> users = new HashSet<JID>(group.getMembers());
        users.addAll(group.getAdmins());
        // Get users whose roster will be updated
        Collection<JID> affectedUsers = getAffectedUsers(group);
        // Iterate on group members and update rosters of affected users
        for (JID deletedUser : users) {
            groupUserDeleted(group, affectedUsers, deletedUser);
        }
    }

    public void groupModified(Group group, Map params) {
        // Do nothing if no group property has been modified
        if ("propertyDeleted".equals(params.get("type"))) {
             return;
        }
        String keyChanged = (String) params.get("propertyKey");
        String originalValue = (String) params.get("originalValue");


        if ("sharedRoster.showInRoster".equals(keyChanged)) {
            String currentValue = group.getProperties().get("sharedRoster.showInRoster");
            // Nothing has changed so do nothing.
            if (currentValue.equals(originalValue)) {
                return;
            }
            // Get the users of the group
            Collection<JID> users = new HashSet<JID>(group.getMembers());
            users.addAll(group.getAdmins());
            // Get the users whose roster will be affected
            Collection<JID> affectedUsers = getAffectedUsers(group, originalValue,
                    group.getProperties().get("sharedRoster.groupList"));
            // Remove the group members from the affected rosters
            for (JID deletedUser : users) {
                groupUserDeleted(group, affectedUsers, deletedUser);
            }

            // Simulate that the group users has been added to the group. This will cause to push
            // roster items to the "affected" users for the group users
            for (JID user : users) {
                groupUserAdded(group, user);
            }
        }
        else if ("sharedRoster.groupList".equals(keyChanged)) {
            String currentValue = group.getProperties().get("sharedRoster.groupList");
            // Nothing has changed so do nothing.
            if (currentValue.equals(originalValue)) {
                return;
            }
            // Get the users of the group
            Collection<JID> users = new HashSet<JID>(group.getMembers());
            users.addAll(group.getAdmins());
            // Get the users whose roster will be affected
            Collection<JID> affectedUsers = getAffectedUsers(group,
                    group.getProperties().get("sharedRoster.showInRoster"), originalValue);
            // Remove the group members from the affected rosters
            for (JID deletedUser : users) {
                groupUserDeleted(group, affectedUsers, deletedUser);
            }

            // Simulate that the group users has been added to the group. This will cause to push
            // roster items to the "affected" users for the group users
            for (JID user : users) {
                groupUserAdded(group, user);
            }
        }
        else if ("sharedRoster.displayName".equals(keyChanged)) {
            String currentValue = group.getProperties().get("sharedRoster.displayName");
            // Nothing has changed so do nothing.
            if (currentValue.equals(originalValue)) {
                return;
            }
            // Do nothing if the group is not being shown in users' rosters
            if (!rosterManager.isSharedGroup(group)) {
                return;
            }
            // Get all the affected users
            Collection<JID> users = getAffectedUsers(group);
            // Iterate on all the affected users and update their rosters
            for (JID updatedUser : users) {
                // Get the roster to update.
                IRoster roster = null;
                if (jidFactory.isLocal(updatedUser)) {
                	roster = rosterManager.getRoster(updatedUser.getNode());
                }
                if (roster != null) {
                    // Update the roster with the new group display name
                    roster.shareGroupRenamed(users);
                }
            }
        }
    }

    public void memberAdded(Group group, Map params) {
        JID addedUser = new JID((String) params.get("member"));
        // Do nothing if the user was an admin that became a member
        if (group.getAdmins().contains(addedUser)) {
            return;
        }
        if (!rosterManager.isSharedGroup(group)) {
            for (Group visibleGroup : rosterManager.getVisibleGroups(group)) {
                // Get the list of affected users
                Collection<JID> users = new HashSet<JID>(visibleGroup.getMembers());
                users.addAll(visibleGroup.getAdmins());
                groupUserAdded(visibleGroup, users, addedUser);
            }
        }
        else {
            groupUserAdded(group, addedUser);
        }
    }

    public void memberRemoved(Group group, Map params) {
        String member = (String) params.get("member");
        if (member == null) {
            return;
        }
        JID deletedUser = new JID(member);
        // Do nothing if the user is still an admin
        if (group.getAdmins().contains(deletedUser)) {
            return;
        }
        if (!rosterManager.isSharedGroup(group)) {
            for (Group visibleGroup : rosterManager.getVisibleGroups(group)) {
                // Get the list of affected users
                Collection<JID> users = new HashSet<JID>(visibleGroup.getMembers());
                users.addAll(visibleGroup.getAdmins());
                groupUserDeleted(visibleGroup, users, deletedUser);
            }
        }
        else {
            groupUserDeleted(group, deletedUser);
        }
    }

    public void adminAdded(Group group, Map params) {
        JID addedUser = new JID((String) params.get("admin"));
        // Do nothing if the user was a member that became an admin
        if (group.getMembers().contains(addedUser)) {
            return;
        }
        if (!rosterManager.isSharedGroup(group)) {
            for (Group visibleGroup : rosterManager.getVisibleGroups(group)) {
                // Get the list of affected users
                Collection<JID> users = new HashSet<JID>(visibleGroup.getMembers());
                users.addAll(visibleGroup.getAdmins());
                groupUserAdded(visibleGroup, users, addedUser);
            }
        }
        else {
            groupUserAdded(group, addedUser);
        }
    }

    public void adminRemoved(Group group, Map params) {
        JID deletedUser = new JID((String) params.get("admin"));
        // Do nothing if the user is still a member
        if (group.getMembers().contains(deletedUser)) {
            return;
        }
        // Do nothing if the group is not being shown in group members' rosters
        if (!rosterManager.isSharedGroup(group)) {
            for (Group visibleGroup : rosterManager.getVisibleGroups(group)) {
                // Get the list of affected users
                Collection<JID> users = new HashSet<JID>(visibleGroup.getMembers());
                users.addAll(visibleGroup.getAdmins());
                groupUserDeleted(visibleGroup, users, deletedUser);
            }
        }
        else {
            groupUserDeleted(group, deletedUser);
        }
    }

    /**
     * Notification that a Group user has been added. Update the group users' roster accordingly.
     *
     * @param group the group where the user was added.
     * @param addedUser the username of the user that has been added to the group.
     */
    private void groupUserAdded(Group group, JID addedUser) {
        groupUserAdded(group, getAffectedUsers(group), addedUser);
    }

    /**
     * Notification that a Group user has been added. Update the group users' roster accordingly.
     *
     * @param group the group where the user was added.
     * @param users the users to update their rosters
     * @param addedUser the username of the user that has been added to the group.
     */
    private void groupUserAdded(Group group, Collection<JID> users, JID addedUser) {
        // Get the roster of the added user.
        IRoster addedUserRoster = null;
        if (jidFactory.isLocal(addedUser)) {
            addedUserRoster = rosterManager.getRoster(addedUser.getNode());
        }

        // Iterate on all the affected users and update their rosters
        for (JID userToUpdate : users) {
            if (!addedUser.equals(userToUpdate)) {
                // Get the roster to update
                IRoster roster = null;
                if (jidFactory.isLocal(userToUpdate)) {
                    // Check that the user exists, if not then continue with the next user
                    try {
                        userManager.getUser(userToUpdate.getNode());
                    }
                    catch (UserNotFoundException e) {
                        continue;
                    }
                    roster = rosterManager.getRoster(userToUpdate.getNode());
                }
                // Only update rosters in memory
                if (roster != null) {
                    roster.addSharedUser(group, addedUser);
                }
                // Check if the roster is still not in memory
                if (addedUserRoster == null && jidFactory.isLocal(addedUser)) {
                    addedUserRoster = rosterManager.getRoster(addedUser.getNode());
                }
                // Update the roster of the newly added group user.
                if (addedUserRoster != null) {
                    Collection<Group> groups = groupManager.getGroups(userToUpdate);
                    addedUserRoster.addSharedUser(userToUpdate, groups, group);
                }
                if (!jidFactory.isLocal(addedUser)) {
                    // Susbcribe to the presence of the remote user. This is only necessary for
                    // remote users and may only work with remote users that **automatically**
                    // accept presence subscription requests
                    rosterManager.sendSubscribeRequest(userToUpdate, addedUser, true);
                }
                if (!jidFactory.isLocal(userToUpdate)) {
                    // Susbcribe to the presence of the remote user. This is only necessary for
                    // remote users and may only work with remote users that **automatically**
                    // accept presence subscription requests
                    rosterManager.sendSubscribeRequest(addedUser, userToUpdate, true);
                }
            }
        }
    }

    /**
     * Notification that a Group user has been deleted. Update the group users' roster accordingly.
     *
     * @param group the group from where the user was deleted.
     * @param deletedUser the username of the user that has been deleted from the group.
     */
    private void groupUserDeleted(Group group, JID deletedUser) {
        groupUserDeleted(group, getAffectedUsers(group), deletedUser);
    }

    /**
     * Notification that a Group user has been deleted. Update the group users' roster accordingly.
     *
     * @param group the group from where the user was deleted.
     * @param users the users to update their rosters
     * @param deletedUser the username of the user that has been deleted from the group.
     */
    private void groupUserDeleted(Group group, Collection<JID> users, JID deletedUser) {
        // Get the roster of the deleted user.
        IRoster deletedUserRoster = null;
        if (jidFactory.isLocal(deletedUser)) {
        	deletedUserRoster = rosterManager.getRoster(deletedUser.getNode());
        }

        // Iterate on all the affected users and update their rosters
        for (JID userToUpdate : users) {
            // Get the roster to update
            IRoster roster = null;
            if (jidFactory.isLocal(userToUpdate)) {
                // Check that the user exists, if not then continue with the next user
                try {
                    userManager.getUser(userToUpdate.getNode());
                }
                catch (UserNotFoundException e) {
                    continue;
                }
                roster = rosterManager.getRoster(userToUpdate.getNode());
            }
            // Only update rosters in memory
            if (roster != null) {
                roster.deleteSharedUser(group, deletedUser);
            }
            // Check if the roster is still not in memory
            if (deletedUserRoster == null && jidFactory.isLocal(deletedUser)) {
                deletedUserRoster = rosterManager.getRoster(deletedUser.getNode());
            }
            // Update the roster of the newly deleted group user.
            if (deletedUserRoster != null) {
                deletedUserRoster.deleteSharedUser(userToUpdate, group);
            }
            if (!jidFactory.isLocal(deletedUser)) {
                // Unsusbcribe from the presence of the remote user. This is only necessary for
                // remote users and may only work with remote users that **automatically**
                // accept presence subscription requests
                rosterManager.sendSubscribeRequest(userToUpdate, deletedUser, false);
            }
            if (!jidFactory.isLocal(userToUpdate)) {
                // Unsusbcribe from the presence of the remote user. This is only necessary for
                // remote users and may only work with remote users that **automatically**
                // accept presence subscription requests
                rosterManager.sendSubscribeRequest(deletedUser, userToUpdate, false);
            }
        }
    }

    /**
     * Returns all the users that are related to a shared group. This is the logic that we are
     * using: 1) If the group visiblity is configured as "Everybody" then all users in the system or
     * all logged users in the system will be returned (configurable thorugh the "filterOffline"
     * flag), 2) if the group visiblity is configured as "onlyGroup" then all the group users will
     * be included in the answer and 3) if the group visiblity is configured as "onlyGroup" and
     * the group allows other groups to include the group in the groups users' roster then all
     * the users of the allowed groups will be included in the answer.
     */
    private Collection<JID> getAffectedUsers(Group group) {
        return getAffectedUsers(group, group.getProperties().get("sharedRoster.showInRoster"),
                group.getProperties().get("sharedRoster.groupList"));
    }

    /**
     * This method is similar to {@link #getAffectedUsers(Group)} except that it receives
     * some group properties. The group properties are passed as parameters since the called of this
     * method may want to obtain the related users of the group based in some properties values.
     *
     * This is useful when the group is being edited and some properties has changed and we need to
     * obtain the related users of the group based on the previous group state.
     */ 
    private Collection<JID> getAffectedUsers(Group group, String showInRoster, String groupNames) {
        // Answer an empty collection if the group is not being shown in users' rosters
        if (!"onlyGroup".equals(showInRoster) && !"everybody".equals(showInRoster)) {
            return new ArrayList<JID>();
        }
        // Add the users of the group
        Collection<JID> users = new HashSet<JID>(group.getMembers());
        users.addAll(group.getAdmins());
        // Check if anyone can see this shared group
        if ("everybody".equals(showInRoster)) {
            // Add all users in the system
            for (String username : userManager.getUsernames()) {
                users.add(jidFactory.createJID(username, null, true));
            }
            // Add all logged users. We don't need to add all users in the system since only the
            // logged ones will be affected.
            //users.addAll(SessionManager.getInstance().getSessionUsers());
        }
        else {
            // Add the users that may see the group
            Collection<Group> groupList = rosterManager.parseGroups(groupNames);
            for (Group groupInList : groupList) {
                users.addAll(groupInList.getMembers());
                users.addAll(groupInList.getAdmins());
            }
        }
        return users;
    }

}
