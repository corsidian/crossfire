package org.b5chat.crossfire.roster;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.List;


import org.b5chat.crossfire.SharedGroupException;
import org.b5chat.crossfire.group.Group;
import org.b5chat.crossfire.user.UserAlreadyExistsException;
import org.b5chat.crossfire.user.UserNotFoundException;
import org.b5chat.util.cache.CannotCalculateSizeException;
import org.xmpp.packet.JID;
import org.xmpp.packet.Presence;

public interface IRoster {

	/**
	 * Returns true if the specified user is a member of the roster, false otherwise.
	 *
	 * @param user the user object to check.
	 * @return true if the specified user is a member of the roster, false otherwise.
	 */
	public abstract boolean isRosterItem(JID user);

	public abstract void shareGroupRenamed(Collection<JID> users);

	public abstract void addSharedUser(Group group, JID addedUser); 
	
    public abstract void addSharedUser(JID addedUser, Collection<Group> groups, Group addedGroup);
    
    public abstract void deleteSharedUser(Group sharedGroup, JID deletedUser);
    
    public abstract void deleteSharedUser(JID deletedUser, Group deletedGroup);
    	
	/**
	 * Returns a collection of users in this roster.<p>
	 *
	 * Note: Roster items with subscription type FROM that exist only because of shared groups
	 * are not going to be returned.
	 *
	 * @return a collection of users in this roster.
	 */
	public abstract Collection<IRosterItem> getRosterItems();

	/**
	 * Returns the roster item that is associated with the specified JID. If no roster item
	 * was found then a UserNotFoundException will be thrown.
	 *
	 * @param user the XMPPAddress for the roster item to retrieve
	 * @return The roster item associated with the user XMPPAddress.
	 * @throws UserNotFoundException if no roster item was found for the specified JID.
	 */
	public abstract IRosterItem getRosterItem(JID user)
			throws UserNotFoundException;

	/**
	 * Create a new item to the roster. Roster items may not be created that contain the same user
	 * address as an existing item.
	 *
	 * @param user       The item to add to the roster.
	 * @param push       True if the new item must be pushed to the user.
	 * @param persistent True if the new roster item should be persisted to the DB.
	 */
	public abstract IRosterItem createRosterItem(JID user, boolean push,
			boolean persistent) throws UserAlreadyExistsException,
			SharedGroupException;

	/**
	 * Create a new item to the roster. Roster items may not be created that contain the same user
	 * address as an existing item.
	 *
	 * @param user       The item to add to the roster.
	 * @param nickname   The nickname for the roster entry (can be null).
	 * @param push       True if the new item must be push to the user.
	 * @param persistent True if the new roster item should be persisted to the DB.
	 * @param groups   The list of groups to assign this roster item to (can be null)
	 */
	public abstract IRosterItem createRosterItem(JID user, String nickname,
			List<String> groups, boolean push, boolean persistent)
			throws UserAlreadyExistsException, SharedGroupException;

	/**
	 * Create a new item to the roster based as a copy of the given item.
	 * Roster items may not be created that contain the same user address
	 * as an existing item in the roster.
	 *
	 * @param item the item to copy and add to the roster.
	 */
	public abstract void createRosterItem(org.xmpp.packet.Roster.Item item)
			throws UserAlreadyExistsException, SharedGroupException;

	/**
	 * Update an item that is already in the roster.
	 *
	 * @param item the item to update in the roster.
	 * @throws UserNotFoundException If the roster item for the given user doesn't already exist
	 */
	public abstract void updateRosterItem(IRosterItem item)
			throws UserNotFoundException;

	/**
	 * Remove a user from the roster.
	 *
	 * @param user the user to remove from the roster.
	 * @param doChecking flag that indicates if checkings should be done before deleting the user.
	 * @return The roster item being removed or null if none existed
	 * @throws SharedGroupException if the user to remove belongs to a shared group
	 */
	public abstract IRosterItem deleteRosterItem(JID user, boolean doChecking)
			throws SharedGroupException;

	/**
	 * <p>Return the username of the user or chatbot that owns this roster.</p>
	 *
	 * @return the username of the user or chatbot that owns this roster
	 */
	public abstract String getUsername();

	/**
	 * <p>Obtain a 'roster reset', a snapshot of the full cached roster as an Roster.</p>
	 *
	 * @return The roster reset (snapshot) as an Roster
	 */
	public abstract org.xmpp.packet.Roster getReset();

	/**
	 * <p>Broadcast the presence update to all subscribers of the roter.</p>
	 * <p/>
	 * <p>Any presence change typically results in a broadcast to the roster members.</p>
	 *
	 * @param packet The presence packet to broadcast
	 */
	public abstract void broadcastPresence(Presence packet);

	/**
	 * Broadcasts the RosterItem to all the connected resources of this user. Due to performance
	 * optimizations and due to some clients errors that are showing items with subscription status
	 * FROM we added a flag that indicates if a roster items that exists only because of a shared
	 * group with subscription status FROM will not be sent.
	 *
	 * @param item     the item to broadcast.
	 * @param optimize true indicates that items that exists only because of a shared
	 *                 group with subscription status FROM will not be sent
	 */
	public abstract void broadcast(IRosterItem item, boolean optimize);

	public abstract int getCachedSize() throws CannotCalculateSizeException;

	public abstract void writeExternal(ObjectOutput out) throws IOException;

	public abstract void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException;

}