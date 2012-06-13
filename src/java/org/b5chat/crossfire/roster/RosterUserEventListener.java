package org.b5chat.crossfire.roster;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;


import org.b5chat.crossfire.JIDFactory;
import org.b5chat.crossfire.event.UserEventListener;
import org.b5chat.crossfire.group.Group;
import org.b5chat.crossfire.user.User;
import org.b5chat.crossfire.user.UserManager;
import org.b5chat.crossfire.user.UserNotFoundException;
import org.xmpp.packet.JID;

public class RosterUserEventListener implements UserEventListener {
	private IRosterManager rosterManager;
	private JIDFactory jidFactory;
	
	public RosterUserEventListener(IRosterManager rosterManager, JIDFactory jidFactory) {
		this.rosterManager = rosterManager;
		this.jidFactory = jidFactory;
	}

    /**
     * A new user has been created so members of public shared groups need to have
     * their rosters updated. Members of public shared groups need to have a roster
     * item with subscription FROM for the new user since the new user can see them.
     *
     * @param newUser the newly created user.
     * @param params event parameters.
     */
    public void userCreated(User newUser, Map<String,Object> params) {
        JID newUserJID = jidFactory.createJID(newUser.getUsername(), null);
        // Shared public groups that are public should have a presence subscription
        // of type FROM for the new user
        for (Group group : rosterManager.getPublicSharedGroups()) {
            // Get group members of public group
            Collection<JID> users = new HashSet<JID>(group.getMembers());
            users.addAll(group.getAdmins());
            // Update the roster of each group member to include a subscription of type FROM
            for (JID userToUpdate : users) {
                // Get the roster to update
                IRoster roster = null;
                if (jidFactory.isLocal(userToUpdate)) {
                    // Check that the user exists, if not then continue with the next user
                    try {
                        UserManager.getInstance().getUser(userToUpdate.getNode());
                    }
                    catch (UserNotFoundException e) {
                        continue;
                    }
                    roster = rosterManager.getRoster(userToUpdate.getNode());
                }
                // Only update rosters in memory
                if (roster != null) {
                    roster.addSharedUser(group, newUserJID);
                }
                if (!jidFactory.isLocal(userToUpdate)) {
                    // Susbcribe to the presence of the remote user. This is only necessary for
                    // remote users and may only work with remote users that **automatically**
                    // accept presence subscription requests
                    rosterManager.sendSubscribeRequest(newUserJID, userToUpdate, true);
                }
            }
        }
    }

    public void userDeleting(User user, Map<String,Object> params) {
        // Shared public groups that have a presence subscription of type FROM
        // for the deleted user should no longer have a reference to the deleted user
        JID userJID = jidFactory.createJID(user.getUsername(), null);
        // Shared public groups that are public should have a presence subscription
        // of type FROM for the new user
        for (Group group : rosterManager.getPublicSharedGroups()) {
            // Get group members of public group
            Collection<JID> users = new HashSet<JID>(group.getMembers());
            users.addAll(group.getAdmins());
            // Update the roster of each group member to include a subscription of type FROM
            for (JID userToUpdate : users) {
                // Get the roster to update
                IRoster roster = null;
                if (jidFactory.isLocal(userToUpdate)) {
                    // Check that the user exists, if not then continue with the next user
                    try {
                        UserManager.getInstance().getUser(userToUpdate.getNode());
                    }
                    catch (UserNotFoundException e) {
                        continue;
                    }
                    roster = rosterManager.getRoster(userToUpdate.getNode());
                }
                // Only update rosters in memory
                if (roster != null) {
                    roster.deleteSharedUser(group, userJID);
                }
                if (!jidFactory.isLocal(userToUpdate)) {
                    // Unsusbcribe from the presence of the remote user. This is only necessary for
                    // remote users and may only work with remote users that **automatically**
                    // accept presence subscription requests
                    rosterManager.sendSubscribeRequest(userJID, userToUpdate, false);
                }
            }
        }

        rosterManager.deleteRoster(userJID);
    }

    public void userModified(User user, Map<String,Object> params) {
        //Do nothing
    }

}
