package org.b5chat.crossfire.roster;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.b5chat.crossfire.SharedGroupException;
import org.b5chat.crossfire.group.Group;
import org.b5chat.util.cache.CannotCalculateSizeException;
import org.xmpp.packet.JID;

public interface IRosterItem {

	/**
	 * <p>Indicates the roster item should be removed.</p>
	 */
	public static final SubType SUB_REMOVE = new SubType("remove", -1);
	/**
	 * <p>No subscription is established.</p>
	 */
	public static final SubType SUB_NONE = new SubType("none", 0);
	/**
	 * <p>The roster owner has a subscription to the roster item's presence.</p>
	 */
	public static final SubType SUB_TO = new SubType("to", 1);
	/**
	 * <p>The roster item has a subscription to the roster owner's presence.</p>
	 */
	public static final SubType SUB_FROM = new SubType("from", 2);
	/**
	 * <p>The roster item and owner have a mutual subscription.</p>
	 */
	public static final SubType SUB_BOTH = new SubType("both", 3);
	/**
	 * <p>The roster item has no pending subscription requests.</p>
	 */
	public static final AskType ASK_NONE = new AskType("", -1);
	/**
	 * <p>The roster item has been asked for permission to subscribe to their presence
	 * but no response has been received.</p>
	 */
	public static final AskType ASK_SUBSCRIBE = new AskType("subscribe", 0);
	/**
	 * <p>The roster owner has asked to the roster item to unsubscribe from it's
	 * presence but has not received confirmation.</p>
	 */
	public static final AskType ASK_UNSUBSCRIBE = new AskType("unsubscribe", 1);
	/**
	 * <p>There are no subscriptions that have been received but not presented to the user.</p>
	 */
	public static final RecvType RECV_NONE = new RecvType("", -1);
	/**
	 * <p>The server has received a subscribe request, but has not forwarded it to the user.</p>
	 */
	public static final RecvType RECV_SUBSCRIBE = new RecvType("sub", 1);
	/**
	 * <p>The server has received an unsubscribe request, but has not forwarded it to the user.</p>
	 */
	public static final RecvType RECV_UNSUBSCRIBE = new RecvType("unsub", 2);

	/**
	 * <p>Obtain the current subscription status of the item.</p>
	 *
	 * @return The subscription status of the item
	 */
	public abstract SubType getSubStatus();

	/**
	 * <p>Set the current subscription status of the item.</p>
	 *
	 * @param subStatus The subscription status of the item
	 */
	public abstract void setSubStatus(SubType subStatus);

	/**
	 * <p>Obtain the current ask status of the item.</p>
	 *
	 * @return The ask status of the item
	 */
	public abstract AskType getAskStatus();

	/**
	 * <p>Set the current ask status of the item.</p>
	 *
	 * @param askStatus The ask status of the item
	 */
	public abstract void setAskStatus(AskType askStatus);

	/**
	 * <p>Obtain the current recv status of the item.</p>
	 *
	 * @return The recv status of the item
	 */
	public abstract RecvType getRecvStatus();

	/**
	 * <p>Set the current recv status of the item.</p>
	 *
	 * @param recvStatus The recv status of the item
	 */
	public abstract void setRecvStatus(RecvType recvStatus);

	/**
	 * <p>Obtain the address of the item.</p>
	 *
	 * @return The address of the item
	 */
	public abstract JID getJid();

	/**
	 * <p>Obtain the current nickname for the item.</p>
	 *
	 * @return The subscription status of the item
	 */
	public abstract String getNickname();

	/**
	 * <p>Set the current nickname for the item.</p>
	 *
	 * @param nickname The subscription status of the item
	 */
	public abstract void setNickname(String nickname);

	/**
	 * Returns the groups for the item. Shared groups won't be included in the answer.
	 *
	 * @return The groups for the item.
	 */
	public abstract List<String> getGroups();

	/**
	 * Set the current groups for the item.
	 *
	 * @param groups The new lists of groups the item belongs to.
	 * @throws org.b5chat.crossfire.SharedGroupException if trying to remove shared group.
	 */
	public abstract void setGroups(List<String> groups)
			throws SharedGroupException;

	/**
	 * Returns the shared groups for the item.
	 *
	 * @return The shared groups this item belongs to.
	 */
	public abstract Collection<Group> getSharedGroups();

	/**
	 * Returns the invisible shared groups for the item. These groups are for internal use
	 * and help track the reason why a roster item has a presence subscription of type FROM
	 * when using shared groups.
	 *
	 * @return The shared groups this item belongs to.
	 */
	public abstract Collection<Group> getInvisibleSharedGroups();

	/**
	 * Adds a new group to the shared groups list.
	 *
	 * @param sharedGroup The shared group to add to the list of shared groups.
	 */
	public abstract void addSharedGroup(Group sharedGroup);

	/**
	 * Adds a new group to the list shared groups that won't be sent to the user. These groups
	 * are for internal use and help track the reason why a roster item has a presence
	 * subscription of type FROM when using shared groups.
	 *
	 * @param sharedGroup The shared group to add to the list of shared groups.
	 */
	public abstract void addInvisibleSharedGroup(Group sharedGroup);

	/**
	 * Removes a group from the shared groups list.
	 *
	 * @param sharedGroup The shared group to remove from the list of shared groups.
	 */
	public abstract void removeSharedGroup(Group sharedGroup);

	/**
	 * Returns true if this item belongs to a shared group. Return true even if the item belongs
	 * to a personal group and a shared group.
	 *
	 * @return true if this item belongs to a shared group.
	 */
	public abstract boolean isShared();

	/**
	 * Returns true if this item belongs ONLY to shared groups. This means that the the item is
	 * considered to be "only shared" if it doesn't belong to a personal group but only to shared
	 * groups.
	 *
	 * @return true if this item belongs ONLY to shared groups.
	 */
	public abstract boolean isOnlyShared();

	public abstract Set<String> getInvisibleSharedGroupsNames();

	/**
	 * Returns the roster ID associated with this particular roster item. A value of zero
	 * means that the roster item is not being persisted in the backend store.<p>
	 *
	 * Databases can use the roster ID as the key in locating roster items.
	 *
	 * @return The roster ID
	 */
	public abstract long getID();

	/**
	 * Sets the roster ID associated with this particular roster item. A value of zero
	 * means that the roster item is not being persisted in the backend store.<p>
	 *
	 * Databases can use the roster ID as the key in locating roster items.
	 *
	 * @param rosterID The roster ID.
	 */
	public abstract void setID(long rosterID);

	/**
	 * <p>Update the cached item as a copy of the given item.</p>
	 * <p/>
	 * <p>A convenience for getting the item and setting each attribute.</p>
	 *
	 * @param item The item who's settings will be copied into the cached copy
	 * @throws org.b5chat.crossfire.SharedGroupException if trying to remove shared group.
	 */
	public abstract void setAsCopyOf(org.xmpp.packet.Roster.Item item)
			throws SharedGroupException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.b5chat.util.cache.Cacheable#getCachedSize()
	 */
	public abstract int getCachedSize() throws CannotCalculateSizeException;

	public abstract void writeExternal(ObjectOutput out) throws IOException;

	public abstract void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException;

}