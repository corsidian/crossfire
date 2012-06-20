/**
 * $RCSfile$
 * $Revision: 3144 $
 * $Date: 2005-12-01 14:20:11 -0300 (Thu, 01 Dec 2005) $
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

package org.b5chat.crossfire.xmpp.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.b5chat.crossfire.core.container.IModule;
import org.b5chat.crossfire.core.lockout.LockOutManager;
import org.b5chat.crossfire.core.server.Server;
import org.b5chat.crossfire.database.DbConnectionManager;
import org.b5chat.crossfire.xmpp.auth.IQAuthHandler;
import org.b5chat.crossfire.xmpp.disco.IQDiscoInfoHandler;
import org.b5chat.crossfire.xmpp.disco.IQDiscoItemsHandler;
import org.b5chat.crossfire.xmpp.disco.IServerFeaturesProvider;
import org.b5chat.crossfire.xmpp.disco.IServerIdentitiesProvider;
import org.b5chat.crossfire.xmpp.disco.IServerItemsProvider;
import org.b5chat.crossfire.xmpp.handler.IQHandler;
import org.b5chat.crossfire.xmpp.handler.IQRegisterHandler;
import org.b5chat.crossfire.xmpp.presence.IPresenceManager;
import org.b5chat.crossfire.xmpp.presence.PresenceManagerImpl;
import org.b5chat.crossfire.xmpp.presence.PresenceRouter;
import org.b5chat.crossfire.xmpp.presence.PresenceSubscribeHandler;
import org.b5chat.crossfire.xmpp.presence.PresenceUpdateHandler;
import org.b5chat.crossfire.xmpp.roster.RosterManager;
import org.b5chat.crossfire.xmpp.route.IPacketDeliverer;
import org.b5chat.crossfire.xmpp.route.IPacketRouter;
import org.b5chat.crossfire.xmpp.route.IQRouter;
import org.b5chat.crossfire.xmpp.route.IRoutingTable;
import org.b5chat.crossfire.xmpp.route.MessageRouter;
import org.b5chat.crossfire.xmpp.route.MulticastRouter;
import org.b5chat.crossfire.xmpp.route.PacketDelivererImpl;
import org.b5chat.crossfire.xmpp.route.PacketRouterImpl;
import org.b5chat.crossfire.xmpp.route.RoutingTableImpl;
import org.b5chat.crossfire.xmpp.route.TransportHandler;
import org.b5chat.crossfire.xmpp.session.SessionManager;
import org.b5chat.crossfire.xmpp.user.IUserIdentitiesProvider;
import org.b5chat.crossfire.xmpp.user.IUserItemsProvider;
import org.b5chat.crossfire.xmpp.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

/**
 * The main XMPP server that will load, initialize and start all the server's
 * modules. The server is unique in the JVM and could be obtained by using the
 * {@link #getInstance()} method.<p>
 * <p/>
 * The loaded modules will be initialized and may access through the server other
 * modules. This means that the only way for a module to locate another module is
 * through the server. The server maintains a list of loaded modules.<p>
 * <p/>
 * After starting up all the modules the server will load any available plugin.
 * For more information see: {@link org.b5chat.crossfire.core.plugin.PluginManager}.<p>
 * <p/>
 * A configuration file keeps the server configuration. This information is required for the
 * server to work correctly. The server assumes that the configuration file is named
 * <b>crossfire.xml</b> and is located in the <b>conf</b> folder. The folder that keeps
 * the configuration file must be located under the home folder. The server will try different
 * methods to locate the home folder.
 * <p/>
 * <ol>
 * <li><b>system property</b> - The server will use the value defined in the <i>crossfireHome</i>
 * system property.</li>
 * <li><b>working folder</b> -  The server will check if there is a <i>conf</i> folder in the
 * working directory. This is the case when running in standalone mode.</li>
 * <li><b>crossfire_init.xml file</b> - Attempt to load the value from crossfire_init.xml which
 * must be in the classpath</li>
 * </ol>
 *
 * @author Gaston Dombiak
 */
public class XmppServer extends Server {

	private static final Logger logger = LoggerFactory.getLogger(XmppServer.class);

	private static XmppServer instance;
	
    /**
     * Returns a singleton instance of XMPPServer.
     *
     * @return an instance.
     */
    public static XmppServer getInstance() {
        return instance;
    }

    /**
     * Creates a server and starts it.
     */
    public XmppServer() {
        super();

    	// We may only have one instance of the server running on the JVM
        if (instance != null) {
            throw new IllegalStateException("A server is already running");
        }
        instance = this;
    }

    /**
     * Returns true if the given address is local to the server (managed by this
     * server domain). Return false even if the jid's domain matches a local component's
     * service JID.
     *
     * @param jid the JID to check.
     * @return true if the address is a local address to this server.
     */
    public boolean isLocal(JID jid) {
        boolean local = false;
        if (jid != null && getName() != null && getName().equals(jid.getDomain())) {
            local = true;
        }
        return local;
    }

    /**
     * Returns true if the given address does not match the local server hostname and does not
     * match a component service JID.
     *
     * @param jid the JID to check.
     * @return true if the given address does not match the local server hostname and does not
     *         match a component service JID.
     */
    public boolean isRemote(JID jid) {
        if (jid != null) {
            if (!getName().equals(jid.getDomain())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Creates an XMPPAddress local to this server.
     *
     * @param username the user name portion of the id or null to indicate none is needed.
     * @param resource the resource portion of the id or null to indicate none is needed.
     * @return an XMPPAddress for the server.
     */
    public JID createJID(String username, String resource) {
        return new JID(username, getName(), resource);
    }

    /**
     * Creates an XMPPAddress local to this server. The construction of the new JID
     * can be optimized by skipping stringprep operations.
     *
     * @param username the user name portion of the id or null to indicate none is needed.
     * @param resource the resource portion of the id or null to indicate none is needed.
     * @param skipStringprep true if stringprep should not be applied.
     * @return an XMPPAddress for the server.
     */
    public JID createJID(String username, String resource, boolean skipStringprep) {
        return new JID(username, getName(), resource, skipStringprep);
    }




    /**
     * Verify that the database is accessible.
     */
    protected void verifyDataSource() {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement("SELECT count(*) FROM ofID");
            rs = pstmt.executeQuery();
            rs.next();
        }
        catch (Exception e) {
            System.err.println("Database setup or configuration error: " +
                    "Please verify your database settings and check the " +
                    "logs/error.log file for detailed error messages.");
            logger.error("Database could not be accessed", e);
            throw new IllegalArgumentException(e);
        }
        finally {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
    }

    


    /**
     * Returns the <code>IRoutingTable</code> registered with this server. The
     * <code>IRoutingTable</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>IRoutingTable</code> registered with this server.
     */
    public IRoutingTable getRoutingTable() {
        return (IRoutingTable) getModuleManager().getModule(RoutingTableImpl.class.getName());
    }

    /**
     * Returns the <code>IPacketDeliverer</code> registered with this server. The
     * <code>IPacketDeliverer</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>IPacketDeliverer</code> registered with this server.
     */
    public IPacketDeliverer getPacketDeliverer() {
        return (IPacketDeliverer) getModuleManager().getModule(PacketDelivererImpl.class.getName());
    }

    
    /**
     * Returns the <code>RosterManager</code> registered with this server. The
     * <code>RosterManager</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>RosterManager</code> registered with this server.
     */
    public RosterManager getRosterManager() {
        return (RosterManager) getModuleManager().getModule(RosterManager.class.getName());
    }

    /**
     * Returns the <code>IPresenceManager</code> registered with this server. The
     * <code>IPresenceManager</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>IPresenceManager</code> registered with this server.
     */
    public IPresenceManager getPresenceManager() {
        return (IPresenceManager) getModuleManager().getModule(PresenceManagerImpl.class.getName());
    }

    /**
     * Returns the <code>IPacketRouter</code> registered with this server. The
     * <code>IPacketRouter</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>IPacketRouter</code> registered with this server.
     */
    public IPacketRouter getPacketRouter() {
        return (IPacketRouter) getModuleManager().getModule(PacketRouterImpl.class.getName());
    }

    /**
     * Returns the <code>IQRegisterHandler</code> registered with this server. The
     * <code>IQRegisterHandler</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>IQRegisterHandler</code> registered with this server.
     */
    public IQRegisterHandler getIQRegisterHandler() {
        return (IQRegisterHandler) getModuleManager().getModule(IQRegisterHandler.class.getName());
    }

    /**
     * Returns the <code>IQAuthHandler</code> registered with this server. The
     * <code>IQAuthHandler</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>IQAuthHandler</code> registered with this server.
     */
    public IQAuthHandler getIQAuthHandler() {
        return (IQAuthHandler) getModuleManager().getModule(IQAuthHandler.class.getName());
    }

    /**
     * Returns a list with all the modules registered with the server that inherit from IQHandler.
     *
     * @return a list with all the modules registered with the server that inherit from IQHandler.
     */
    public List<IQHandler> getIQHandlers() {
        List<IQHandler> answer = new ArrayList<IQHandler>();
        for (IModule module : getModuleManager().getModules()) {
            if (module instanceof IQHandler) {
                answer.add((IQHandler) module);
            }
        }
        return answer;
    }

    /**
     * Returns the <code>SessionManager</code> registered with this server. The
     * <code>SessionManager</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>SessionManager</code> registered with this server.
     */
    public SessionManager getSessionManager() {
        return (SessionManager) getModuleManager().getModule(SessionManager.class.getName());
    }

    /**
     * Returns the <code>TransportHandler</code> registered with this server. The
     * <code>TransportHandler</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>TransportHandler</code> registered with this server.
     */
    public TransportHandler getTransportHandler() {
        return (TransportHandler) getModuleManager().getModule(TransportHandler.class.getName());
    }

    /**
     * Returns the <code>PresenceUpdateHandler</code> registered with this server. The
     * <code>PresenceUpdateHandler</code> was registered with the server as a module while starting
     * up the server.
     *
     * @return the <code>PresenceUpdateHandler</code> registered with this server.
     */
    public PresenceUpdateHandler getPresenceUpdateHandler() {
        return (PresenceUpdateHandler) getModuleManager().getModule(PresenceUpdateHandler.class.getName());
    }

    /**
     * Returns the <code>PresenceSubscribeHandler</code> registered with this server. The
     * <code>PresenceSubscribeHandler</code> was registered with the server as a module while
     * starting up the server.
     *
     * @return the <code>PresenceSubscribeHandler</code> registered with this server.
     */
    public PresenceSubscribeHandler getPresenceSubscribeHandler() {
        return (PresenceSubscribeHandler) getModuleManager().getModule(PresenceSubscribeHandler.class.getName());
    }

    /**
     * Returns the <code>IQRouter</code> registered with this server. The
     * <code>IQRouter</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>IQRouter</code> registered with this server.
     */
    public IQRouter getIQRouter() {
        return (IQRouter) getModuleManager().getModule(IQRouter.class.getName());
    }

    /**
     * Returns the <code>MessageRouter</code> registered with this server. The
     * <code>MessageRouter</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>MessageRouter</code> registered with this server.
     */
    public MessageRouter getMessageRouter() {
        return (MessageRouter) getModuleManager().getModule(MessageRouter.class.getName());
    }

    /**
     * Returns the <code>PresenceRouter</code> registered with this server. The
     * <code>PresenceRouter</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>PresenceRouter</code> registered with this server.
     */
    public PresenceRouter getPresenceRouter() {
        return (PresenceRouter) getModuleManager().getModule(PresenceRouter.class.getName());
    }

    /**
     * Returns the <code>MulticastRouter</code> registered with this server. The
     * <code>MulticastRouter</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>MulticastRouter</code> registered with this server.
     */
    public MulticastRouter getMulticastRouter() {
        return (MulticastRouter) getModuleManager().getModule(MulticastRouter.class.getName());
    }

    /**
     * Returns the <code>UserManager</code> registered with this server. The
     * <code>UserManager</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>UserManager</code> registered with this server.
     */
    public UserManager getUserManager() {
        return UserManager.getInstance();
    }

    /**
     * Returns the <code>LockOutManager</code> registered with this server.  The
     * <code>LockOutManager</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>LockOutManager</code> registered with this server.
     */
    public LockOutManager getLockOutManager() {
        return LockOutManager.getInstance();
    }

    /**
     * Returns a list with all the modules that provide "discoverable" features.
     *
     * @return a list with all the modules that provide "discoverable" features.
     */
    public List<IServerFeaturesProvider> getServerFeaturesProviders() {
        List<IServerFeaturesProvider> answer = new ArrayList<IServerFeaturesProvider>();
        for (IModule module : getModuleManager().getModules()) {
            if (module instanceof IServerFeaturesProvider) {
                answer.add((IServerFeaturesProvider) module);
            }
        }
        return answer;
    }
 
    /**
     * Returns a list with all the modules that provide "discoverable" identities.
     *
     * @return a list with all the modules that provide "discoverable" identities.
     */
    public List<IServerIdentitiesProvider> getServerIdentitiesProviders() {
        List<IServerIdentitiesProvider> answer = new ArrayList<IServerIdentitiesProvider>();
        for (IModule module : getModuleManager().getModules()) {
            if (module instanceof IServerIdentitiesProvider) {
                answer.add((IServerIdentitiesProvider) module);
            }
        }
        return answer;
    }

    /**
     * Returns a list with all the modules that provide "discoverable" items associated with
     * the server.
     *
     * @return a list with all the modules that provide "discoverable" items associated with
     *         the server.
     */
    public List<IServerItemsProvider> getServerItemsProviders() {
        List<IServerItemsProvider> answer = new ArrayList<IServerItemsProvider>();
        for (IModule module : getModuleManager().getModules()) {
            if (module instanceof IServerItemsProvider) {
                answer.add((IServerItemsProvider) module);
            }
        }
        return answer;
    }
    
    /**
     * Returns a list with all the modules that provide "discoverable" user identities.
     *
     * @return a list with all the modules that provide "discoverable" user identities.
     */
    public List<IUserIdentitiesProvider> getUserIdentitiesProviders() {
        List<IUserIdentitiesProvider> answer = new ArrayList<IUserIdentitiesProvider>();
        for (IModule module : getModuleManager().getModules()) {
            if (module instanceof IUserIdentitiesProvider) {
                answer.add((IUserIdentitiesProvider) module);
            }
        }
        return answer;
    }

    /**
     * Returns a list with all the modules that provide "discoverable" items associated with
     * users.
     *
     * @return a list with all the modules that provide "discoverable" items associated with
     *         users.
     */
    public List<IUserItemsProvider> getUserItemsProviders() {
        List<IUserItemsProvider> answer = new ArrayList<IUserItemsProvider>();
        for (IModule module : getModuleManager().getModules()) {
            if (module instanceof IUserItemsProvider) {
                answer.add((IUserItemsProvider) module);
            }
        }
        return answer;
    }

    /**
     * Returns the <code>IQDiscoInfoHandler</code> registered with this server. The
     * <code>IQDiscoInfoHandler</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>IQDiscoInfoHandler</code> registered with this server.
     */
    public IQDiscoInfoHandler getIQDiscoInfoHandler() {
        return (IQDiscoInfoHandler) getModuleManager().getModule(IQDiscoInfoHandler.class.getName());
    }

    /**
     * Returns the <code>IQDiscoItemsHandler</code> registered with this server. The
     * <code>IQDiscoItemsHandler</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>IQDiscoItemsHandler</code> registered with this server.
     */
    public IQDiscoItemsHandler getIQDiscoItemsHandler() {
        return (IQDiscoItemsHandler) getModuleManager().getModule(IQDiscoItemsHandler.class.getName());
    }

}
