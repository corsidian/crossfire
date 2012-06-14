package org.b5chat.crossfire.roster;

import org.b5chat.util.cache.Cache;

public class RosterContactEventListener implements IRosterEventListener {
	private IRosterManager rosterManager;
	
	public RosterContactEventListener(IRosterManager rosterManager) {
		this.rosterManager = rosterManager;
	}

	public void rosterLoaded(IRoster roster) {
        // Do nothing
    }

    public boolean addingContact(IRoster roster, IRosterItem item, boolean persistent) {
        // Do nothing
        return true;
    }

    public void contactAdded(IRoster roster, IRosterItem item) {
        // Set object again in cache. This is done so that other cluster nodes
        // get refreshed with latest version of the object
    	rosterManager.addRoster(roster);
    }

    public void contactUpdated(IRoster roster, IRosterItem item) {
        // Set object again in cache. This is done so that other cluster nodes
        // get refreshed with latest version of the object
    	rosterManager.addRoster(roster);
    }

    public void contactDeleted(IRoster roster, IRosterItem item) {
        // Set object again in cache. This is done so that other cluster nodes
        // get refreshed with latest version of the object
        rosterManager.addRoster(roster);
    }
}
