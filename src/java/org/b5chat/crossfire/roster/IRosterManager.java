package org.b5chat.crossfire.roster;

import java.util.Collection;


import org.b5chat.crossfire.group.Group;
import org.xmpp.packet.JID;

public interface IRosterManager {
	
	// temp until refactor to move roster item provider under manager
	public abstract RosterItemProvider getRosterItemProvider();

	// temp until refactor
	public abstract RosterEventDispatcher getRosterEventDispatcher();

	/**
	 * Returns the roster for the given username.
	 *
	 * @param username the username to search for.
	 * @return the roster associated with the ID.
	 */
	public abstract IRoster getRoster(String username);

	/**
	 * Removes the entire roster of a given user. This is necessary when a user
	 * account is being deleted from the server.
	 *
	 * @param user the user.
	 */
	public abstract void deleteRoster(JID user);
    
    // Used by clustering code.. shouldn't write to database on each node of cluster
    // this needs to be rewritten to local vs remote rosters
    public abstract void addRoster(IRoster roster);
    
    public abstract Collection<JID> getSharedUsersForRoster(Group group, IRoster roster);
    
    public abstract boolean isGroupVisible(Group group, JID user) ;
    
	/**
	 * Returns a collection with all the groups that the user may include in his roster. The
	 * following criteria will be used to select the groups: 1) Groups that are configured so that
	 * everybody can include in his roster, 2) Groups that are configured so that its users may
	 * include the group in their rosters and the user is a group user of the group and 3) User
	 * belongs to a Group that may see a Group that whose members may include the Group in their
	 * rosters.
	 *
	 * @param username the username of the user to return his shared groups.
	 * @return a collection with all the groups that the user may include in his roster.
	 */
	public abstract Collection<Group> getSharedGroups(String username);

	/**
	 * Returns the list of shared groups whose visibility is public.
	 *
	 * @return the list of shared groups whose visibility is public.
	 */
	public abstract Collection<Group> getPublicSharedGroups();

	/**
	 * Returns a collection of Groups obtained by parsing a comma delimited String with the name
	 * of groups.
	 *
	 * @param groupNames a comma delimited string with group names.
	 * @return a collection of Groups obtained by parsing a comma delimited String with the name
	 *         of groups.
	 */
	public abstract Collection<Group> parseGroups(String groupNames);

	/**
	 * Returns true if the specified Group may be included in a user roster. The decision is made
	 * based on the group properties that are configurable through the Admin Console.
	 *
	 * @param group the group to check if it may be considered a shared group.
	 * @return true if the specified Group may be included in a user roster.
	 */
	public abstract boolean isSharedGroup(Group group);

	public abstract void sendSubscribeRequest(JID sender, JID recipient,
			boolean isSubscribe);

	public abstract Collection<Group> getVisibleGroups(Group groupToCheck);

	public boolean hasMutualVisibility(String user, Collection<Group> groups, JID otherUser,
            Collection<Group> otherGroups);
}