package org.b5chat.crossfire.roster;

import org.b5chat.crossfire.XMPPServer;
import org.b5chat.crossfire.container.BasicModule;
import org.b5chat.crossfire.event.GroupEventDispatcher;
import org.b5chat.crossfire.event.UserEventDispatcher;
import org.b5chat.crossfire.group.GroupManager;
import org.b5chat.crossfire.user.UserNameManager;
import org.b5chat.database.DbConnectionManager;
import org.b5chat.util.Globals;
import org.b5chat.util.cache.Cache;
import org.b5chat.util.cache.CacheFactory;


public class RosterModule extends BasicModule {
	private XMPPServer server;
	private IRosterManager rosterManager;

	private RosterUserEventListener rosterUserEventListener;
    private RosterGroupEventListener rosterGroupEventListener;
    private RosterContactEventListener rosterContactEventListener;
    
	public RosterModule() {
		super("Roster Module");	    
	}
	
    /**
     * Returns true if the roster service is enabled. When disabled it is not possible to
     * retrieve users rosters or broadcast presence packets to roster contacts.
     *
     * @return true if the roster service is enabled.
     */
    public boolean isRosterServiceEnabled() {
        return Globals.getBooleanProperty("xmpp.client.roster.active", true);
    }
    
    @Override
	public void initialize(XMPPServer server) {
        super.initialize(server);
        
        this.server = server;
	    rosterUserEventListener = new RosterUserEventListener(rosterManager, server);
	    rosterGroupEventListener = new RosterGroupEventListener(rosterManager, server, 
	    		GroupManager.getInstance(), server.getUserManager());
	    rosterContactEventListener = new RosterContactEventListener(rosterManager);
	    
        rosterManager = new RosterManager(server, server.getRoutingTable(), server.getPresenceManager(), 
        		server.getSessionManager(), server.getPacketRouter(), GroupManager.getInstance(), 
        		server.getUserManager(),
        		UserNameManager.getInstance(), DbConnectionManager.getInstance());

        // not sure what below is for.. seems out of place.. possible quick bug fix.
        rosterManager.getRosterEventDispatcher().addListener(rosterContactEventListener);
    }
    
    public IRosterManager getRosterManager() {
    	return rosterManager;
    }

    @Override
	public void start() throws IllegalStateException {
        super.start();
        // Add this module as a user event listener so we can update
        // rosters when users are created or deleted
        UserEventDispatcher.addListener(rosterUserEventListener);
        // Add the new instance as a listener of group events
        GroupEventDispatcher.addListener(rosterGroupEventListener);
    }

    @Override
	public void stop() {
        super.stop();
        // Remove this module as a user event listener
        UserEventDispatcher.removeListener(rosterUserEventListener);
        // Remove this module as a listener of group events
        GroupEventDispatcher.removeListener(rosterGroupEventListener);
    }
}
