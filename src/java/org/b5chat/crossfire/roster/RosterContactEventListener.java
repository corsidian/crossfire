package org.b5chat.crossfire.roster;

import org.b5chat.util.cache.Cache;

public class RosterContactEventListener implements RosterEventListener {
	private IRosterManager rosterManager;
	
	public RosterContactEventListener(IRosterManager rosterManager) {
		this.rosterManager = rosterManager;
	}

	public void rosterLoaded(IRoster roster) {
        // Do nothing
    }

    public boolean addingContact(IRoster roster, RosterItem item, boolean persistent) {
        // Do nothing
        return true;
    }

    public void contactAdded(IRoster roster, RosterItem item) {
        // Set object again in cache. This is done so that other cluster nodes
        // get refreshed with latest version of the object
    	rosterManager.addRoster(roster);
    }

    public void contactUpdated(IRoster roster, RosterItem item) {
        // Set object again in cache. This is done so that other cluster nodes
        // get refreshed with latest version of the object
    	rosterManager.addRoster(roster);
    }

    public void contactDeleted(IRoster roster, RosterItem item) {
        // Set object again in cache. This is done so that other cluster nodes
        // get refreshed with latest version of the object
        rosterManager.addRoster(roster);
    }
}
