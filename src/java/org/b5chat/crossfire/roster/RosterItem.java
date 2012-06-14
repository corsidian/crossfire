/**
 * $RCSfile: RosterItem.java,v $
 * $Revision: 3080 $
 * $Date: 2005-11-15 01:28:23 -0300 (Tue, 15 Nov 2005) $
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


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.b5chat.crossfire.SharedGroupException;
import org.b5chat.crossfire.group.Group;
import org.b5chat.crossfire.group.GroupManager;
import org.b5chat.crossfire.group.GroupNotFoundException;
import org.b5chat.crossfire.user.UserNameManager;
import org.b5chat.crossfire.user.UserNotFoundException;
import org.b5chat.util.cache.CacheSizes;
import org.b5chat.util.cache.Cacheable;
import org.b5chat.util.cache.CannotCalculateSizeException;
import org.b5chat.util.cache.ExternalizableUtil;
import org.xmpp.packet.JID;

/**
 * <p>Represents a single roster item for a User's Roster.</p>
 * <p>The server doesn't need to know anything about roster groups so they are
 * not stored with easy retrieval or manipulation in mind. The important data
 * elements of a roster item (beyond the jid adddress of the roster entry) includes:</p>
 * <p/>
 * <ul>
 * <li>nick   - A nickname for the user when used in this roster</li>
 * <li>sub    - A subscription type: to, from, none, both</li>
 * <li>ask    - An optional subscription ask status: subscribe, unsubscribe</li>
 * <li>groups - A list of groups to organize roster entries under (e.g. friends, co-workers, etc)</li>
 * </ul>
 *
 * @author Gaston Dombiak
 */
public class RosterItem implements Cacheable, Externalizable, IRosterItem {

    protected RecvType recvStatus;
    protected JID jid;
    protected String nickname;
    protected List<String> groups;
    protected Set<String> sharedGroups = new HashSet<String>();
    protected Set<String> invisibleSharedGroups = new HashSet<String>();
    protected SubType subStatus;
    protected AskType askStatus;
    /**
     * Holds the ID that uniquely identifies the roster in the backend store. A value of
     * zero means that the roster item is not persistent.
     */
    private long rosterID;

    private UserNameManager userNameManager;
    private GroupManager groupManager;
    
    /**
     * Constructor added for Externalizable. Do not use this constructor.
     */
    public RosterItem() {
    }

    public RosterItem(long id,
                                JID jid,
                                SubType subStatus,
                                AskType askStatus,
                                RecvType recvStatus,
                                String nickname,
                                List<String> groups,
                                UserNameManager userNameManager, 
                                GroupManager groupManager) {
        this(jid, subStatus, askStatus, recvStatus, nickname, groups, userNameManager, groupManager);
        this.rosterID = id;
    }

    public RosterItem(JID jid,
                           SubType subStatus,
                           AskType askStatus,
                           RecvType recvStatus,
                           String nickname,
                           List<String> groups,
                           UserNameManager userNameManager,
                           GroupManager groupManager) {
    	this.userNameManager = userNameManager;
    	this.groupManager = groupManager;
    	
        this.jid = jid;
        this.subStatus = subStatus;
        this.askStatus = askStatus;
        this.recvStatus = recvStatus;
        this.nickname = nickname;
        this.groups = new LinkedList<String>();
        if (groups != null) {
            for (String group : groups) {
                this.groups.add(group);
            }
        }
    }

    /**
     * Create a roster item from the data in another one.
     *
     * @param item Item that contains the info of the roster item.
     */
    public RosterItem(org.xmpp.packet.Roster.Item item, 
    		UserNameManager userNameManager, GroupManager groupManager) {
        this(item.getJID(),
                getSubType(item),
                getAskStatus(item),
                IRosterItem.RECV_NONE,
                item.getName(),
                new LinkedList<String>(item.getGroups()), userNameManager, groupManager);
    }

    private static AskType getAskStatus(org.xmpp.packet.Roster.Item item) {
        if (item.getAsk() == org.xmpp.packet.Roster.Ask.subscribe) {
            return IRosterItem.ASK_SUBSCRIBE;
        }
        else if (item.getAsk() == org.xmpp.packet.Roster.Ask.unsubscribe) {
            return IRosterItem.ASK_UNSUBSCRIBE;
        }
        else {
            return IRosterItem.ASK_NONE;
        }
    }

    private static SubType getSubType(org.xmpp.packet.Roster.Item item) {
        if (item.getSubscription() == org.xmpp.packet.Roster.Subscription.to) {
            return IRosterItem.SUB_TO;
        }
        else if (item.getSubscription() == org.xmpp.packet.Roster.Subscription.from) {
            return IRosterItem.SUB_FROM;
        }
        else if (item.getSubscription() == org.xmpp.packet.Roster.Subscription.both) {
            return IRosterItem.SUB_BOTH;
        }
        else if (item.getSubscription() == org.xmpp.packet.Roster.Subscription.remove) {
            return IRosterItem.SUB_REMOVE;
        }
        else {
            return IRosterItem.SUB_NONE;
        }
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#getSubStatus()
	 */
    @Override
	public SubType getSubStatus() {
        return subStatus;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#setSubStatus(org.b5chat.crossfire.roster.RosterItem.SubType)
	 */
    @Override
	public void setSubStatus(SubType subStatus) {
        // Optimization: Load user only if we need to set the nickname of the roster item
        if ("".equals(nickname) && (subStatus == SUB_BOTH || subStatus == SUB_TO)) {
            try {
                nickname = userNameManager.getUserName(jid);
            }
            catch (UserNotFoundException e) {
                // Do nothing
            }
        }
        this.subStatus = subStatus;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#getAskStatus()
	 */
    @Override
	public AskType getAskStatus() {
        if (isShared()) {
            // Redefine the ask status since the item belongs to a shared group
            return ASK_NONE;
        }
        else {
            return askStatus;
        }
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#setAskStatus(org.b5chat.crossfire.roster.RosterItem.AskType)
	 */
    @Override
	public void setAskStatus(AskType askStatus) {
        this.askStatus = askStatus;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#getRecvStatus()
	 */
    @Override
	public RecvType getRecvStatus() {
        return recvStatus;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#setRecvStatus(org.b5chat.crossfire.roster.RosterItem.RecvType)
	 */
    @Override
	public void setRecvStatus(RecvType recvStatus) {
        this.recvStatus = recvStatus;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#getJid()
	 */
    @Override
	public JID getJid() {
        return jid;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#getNickname()
	 */
    @Override
	public String getNickname() {
        return nickname;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#setNickname(java.lang.String)
	 */
    @Override
	public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#getGroups()
	 */
    @Override
	public List<String> getGroups() {
        return groups;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#setGroups(java.util.List)
	 */
    @Override
	public void setGroups(List<String> groups) throws SharedGroupException {
        if (groups == null) {
            this.groups = new LinkedList<String>();
        }
        else {
            // Raise an error if the user is trying to remove the item from a shared group
            for (Group group: getSharedGroups()) {
                // Get the display name of the group
                String groupName = group.getProperties().get("sharedRoster.displayName");
                // Check if the group has been removed from the new groups list
                if (!groups.contains(groupName)) {
                    throw new SharedGroupException("Cannot remove item from shared group");
                }
            }

            // Remove shared groups from the param
            Collection<Group> existingGroups = groupManager.getSharedGroups();
            for (Iterator<String> it=groups.iterator(); it.hasNext();) {
                String groupName = it.next();
                try {
                    // Optimistic approach for performance reasons. Assume first that the shared
                    // group name is the same as the display name for the shared roster

                    // Check if exists a shared group with this name
                    Group group = groupManager.getGroup(groupName);
                    // Get the display name of the group
                    String displayName = group.getProperties().get("sharedRoster.displayName");
                    if (displayName != null && displayName.equals(groupName)) {
                        // Remove the shared group from the list (since it exists)
                        try {
                            it.remove();
                        }
                        catch (IllegalStateException e) {
                            // Do nothing
                        }
                    }
                }
                catch (GroupNotFoundException e) {
                    // Check now if there is a group whose display name matches the requested group
                    for (Group group : existingGroups) {
                        // Get the display name of the group
                        String displayName = group.getProperties().get("sharedRoster.displayName");
                        if (displayName != null && displayName.equals(groupName)) {
                            // Remove the shared group from the list (since it exists)
                            try {
                                it.remove();
                            }
                            catch (IllegalStateException ise) {
                                // Do nothing
                            }
                        }
                    }
                }
            }
            this.groups = groups;
        }
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#getSharedGroups()
	 */
    @Override
	public Collection<Group> getSharedGroups() {
        Collection<Group> groups = new ArrayList<Group>(sharedGroups.size());
        for (String groupName : sharedGroups) {
            try {
                groups.add(groupManager.getGroup(groupName));
            }
            catch (GroupNotFoundException e) {
                // Do nothing
            }
        }
        return groups;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#getInvisibleSharedGroups()
	 */
    @Override
	public Collection<Group> getInvisibleSharedGroups() {
        Collection<Group> groups = new ArrayList<Group>(invisibleSharedGroups.size());
        for (String groupName : invisibleSharedGroups) {
            try {
                groups.add(groupManager.getGroup(groupName));
            }
            catch (GroupNotFoundException e) {
                // Do nothing
            }
        }
        return groups;
    }

    public Set<String> getInvisibleSharedGroupsNames() {
        return invisibleSharedGroups;
    }

    void setInvisibleSharedGroupsNames(Set<String> groupsNames) {
        invisibleSharedGroups = groupsNames;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#addSharedGroup(org.b5chat.crossfire.group.Group)
	 */
    @Override
	public void addSharedGroup(Group sharedGroup) {
        sharedGroups.add(sharedGroup.getName());
        invisibleSharedGroups.remove(sharedGroup.getName());
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#addInvisibleSharedGroup(org.b5chat.crossfire.group.Group)
	 */
    @Override
	public void addInvisibleSharedGroup(Group sharedGroup) {
        invisibleSharedGroups.add(sharedGroup.getName());
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#removeSharedGroup(org.b5chat.crossfire.group.Group)
	 */
    @Override
	public void removeSharedGroup(Group sharedGroup) {
        sharedGroups.remove(sharedGroup.getName());
        invisibleSharedGroups.remove(sharedGroup.getName());
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#isShared()
	 */
    @Override
	public boolean isShared() {
        return !sharedGroups.isEmpty() || !invisibleSharedGroups.isEmpty();
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#isOnlyShared()
	 */
    @Override
	public boolean isOnlyShared() {
        return isShared() && groups.isEmpty();
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#getID()
	 */
    @Override
	public long getID() {
        return rosterID;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#setID(long)
	 */
    @Override
	public void setID(long rosterID) {
        this.rosterID = rosterID;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#setAsCopyOf(org.xmpp.packet.Roster.Item)
	 */
    @Override
	public void setAsCopyOf(org.xmpp.packet.Roster.Item item) throws SharedGroupException {
        setGroups(new LinkedList<String>(item.getGroups()));
        setNickname(item.getName());
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.b5chat.util.cache.Cacheable#getCachedSize()
	 */
    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#getCachedSize()
	 */
    @Override
	public int getCachedSize() throws CannotCalculateSizeException {
        int size = jid.toBareJID().length();
        size += CacheSizes.sizeOfString(nickname);
        size += CacheSizes.sizeOfCollection(groups);
        size += CacheSizes.sizeOfCollection(invisibleSharedGroups);
        size += CacheSizes.sizeOfCollection(sharedGroups);
        size += CacheSizes.sizeOfInt(); // subStatus
        size += CacheSizes.sizeOfInt(); // askStatus
        size += CacheSizes.sizeOfInt(); // recvStatus
        size += CacheSizes.sizeOfLong(); // id
        return size;
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#writeExternal(java.io.ObjectOutput)
	 */
    @Override
	public void writeExternal(ObjectOutput out) throws IOException {
        ExternalizableUtil.getInstance().writeSafeUTF(out, jid.toString());
        ExternalizableUtil.getInstance().writeBoolean(out, nickname != null);
        if (nickname != null) {
            ExternalizableUtil.getInstance().writeSafeUTF(out, nickname);
        }
        ExternalizableUtil.getInstance().writeStrings(out, groups);
        ExternalizableUtil.getInstance().writeStrings(out, sharedGroups);
        ExternalizableUtil.getInstance().writeStrings(out, invisibleSharedGroups);
        ExternalizableUtil.getInstance().writeInt(out, recvStatus.getValue());
        ExternalizableUtil.getInstance().writeInt(out, subStatus.getValue());
        ExternalizableUtil.getInstance().writeInt(out, askStatus.getValue());
        ExternalizableUtil.getInstance().writeLong(out, rosterID);
    }

    /* (non-Javadoc)
	 * @see org.b5chat.crossfire.roster.IRosterItem#readExternal(java.io.ObjectInput)
	 */
    @Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        jid = new JID(ExternalizableUtil.getInstance().readSafeUTF(in));
        if (ExternalizableUtil.getInstance().readBoolean(in)) {
            nickname = ExternalizableUtil.getInstance().readSafeUTF(in);
        }
        this.groups = new LinkedList<String>();
        ExternalizableUtil.getInstance().readStrings(in, groups);
        ExternalizableUtil.getInstance().readStrings(in, sharedGroups);
        ExternalizableUtil.getInstance().readStrings(in, invisibleSharedGroups);
        recvStatus = RecvType.getTypeFromInt(ExternalizableUtil.getInstance().readInt(in));
        subStatus = SubType.getTypeFromInt(ExternalizableUtil.getInstance().readInt(in));
        askStatus = AskType.getTypeFromInt(ExternalizableUtil.getInstance().readInt(in));
        rosterID = ExternalizableUtil.getInstance().readLong(in);
    }
}
