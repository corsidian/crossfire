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

package org.b5chat.crossfire.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;


import org.b5chat.crossfire.ConnectionManagerImpl;
import org.b5chat.crossfire.IConnectionManager;
import org.b5chat.crossfire.admin.AdminManager;
import org.b5chat.crossfire.auth.IqAuthHandler;
import org.b5chat.crossfire.core.container.IModule;
import org.b5chat.crossfire.core.net.MulticastDNSService;
import org.b5chat.crossfire.core.plugin.PluginManager;
import org.b5chat.crossfire.disco.IQDiscoInfoHandler;
import org.b5chat.crossfire.disco.IQDiscoItemsHandler;
import org.b5chat.crossfire.disco.IServerFeaturesProvider;
import org.b5chat.crossfire.disco.IServerIdentitiesProvider;
import org.b5chat.crossfire.disco.IServerItemsProvider;
import org.b5chat.crossfire.group.IqSharedGroupHandler;
import org.b5chat.crossfire.handler.IqBindHandler;
import org.b5chat.crossfire.handler.IqHandler;
import org.b5chat.crossfire.handler.IqLastActivityHandler;
import org.b5chat.crossfire.handler.IqPingHandler;
import org.b5chat.crossfire.handler.IqRegisterHandler;
import org.b5chat.crossfire.handler.IqTimeHandler;
import org.b5chat.crossfire.handler.IqVersionHandler;
import org.b5chat.crossfire.lockout.LockOutManager;
import org.b5chat.crossfire.offline.IQOfflineMessagesHandler;
import org.b5chat.crossfire.offline.OfflineMessageStore;
import org.b5chat.crossfire.offline.OfflineMessageStrategy;
import org.b5chat.crossfire.presence.IPresenceManager;
import org.b5chat.crossfire.presence.PresenceManagerImpl;
import org.b5chat.crossfire.presence.PresenceRouter;
import org.b5chat.crossfire.presence.PresenceSubscribeHandler;
import org.b5chat.crossfire.presence.PresenceUpdateHandler;
import org.b5chat.crossfire.privacy.IQPrivacyHandler;
import org.b5chat.crossfire.roster.IQRosterHandler;
import org.b5chat.crossfire.roster.RosterManager;
import org.b5chat.crossfire.route.IPacketDeliverer;
import org.b5chat.crossfire.route.IPacketRouter;
import org.b5chat.crossfire.route.IQRouter;
import org.b5chat.crossfire.route.IRoutingTable;
import org.b5chat.crossfire.route.MessageRouter;
import org.b5chat.crossfire.route.MulticastRouter;
import org.b5chat.crossfire.route.PacketDelivererImpl;
import org.b5chat.crossfire.route.PacketRouterImpl;
import org.b5chat.crossfire.route.PacketTransporterImpl;
import org.b5chat.crossfire.route.RoutingTableImpl;
import org.b5chat.crossfire.route.TransportHandler;
import org.b5chat.crossfire.session.IQSessionEstablishmentHandler;
import org.b5chat.crossfire.session.IRemoteSessionLocator;
import org.b5chat.crossfire.session.SessionManager;
import org.b5chat.crossfire.user.IUserIdentitiesProvider;
import org.b5chat.crossfire.user.IUserItemsProvider;
import org.b5chat.crossfire.user.UserManager;
import org.b5chat.database.DbConnectionManager;
import org.b5chat.util.Globals;
import org.b5chat.util.InitializationException;
import org.b5chat.util.LocaleUtils;
import org.b5chat.util.TaskEngine;
import org.b5chat.util.Version;
import org.b5chat.util.cache.CacheFactory;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;
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
public class XmppServer {

	private static final Logger Log = LoggerFactory.getLogger(XmppServer.class);

    private static XmppServer instance;

    private String name;
    private String host;
    private Version version;
    private Date startDate;
    private boolean initialized = false;
    private boolean started = false;
    private NodeID nodeID;
    private static final NodeID DEFAULT_NODE_ID = NodeID.getInstance(new byte[0]);

    /**
     * All modules loaded by this server
     */
    private Map<Class, IModule> modules = new LinkedHashMap<Class, IModule>();

    /**
     * Listeners that will be notified when the server has started or is about to be stopped.
     */
    private List<IXmppServerListener> listeners = new CopyOnWriteArrayList<IXmppServerListener>();

    /**
     * Location of the home directory. All configuration files should be
     * located here.
     */
    private File crossfireHome;
    private ClassLoader loader;

    private PluginManager pluginManager;
    private IRemoteSessionLocator remoteSessionLocator;

    /**
     * True if in setup mode
     */
    private boolean setupMode = true;

    private static final String STARTER_CLASSNAME =
            "org.b5chat.crossfire.core.starter.ServerStarter";
    private static final String WRAPPER_CLASSNAME =
            "org.tanukisoftware.wrapper.WrapperManager";
    private boolean shuttingDown;
    private XMPPServerInfoImpl xmppServerInfo;

    /**
     * Returns a singleton instance of XmppServer.
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
        // We may only have one instance of the server running on the JVM
        if (instance != null) {
            throw new IllegalStateException("A server is already running");
        }
        instance = this;
        start();
    }

    /**
     * Returns a snapshot of the server's status.
     *
     * @return the server information current at the time of the method call.
     */
    public IXmppServerInfo getServerInfo() {
        if (!initialized) {
            throw new IllegalStateException("Not initialized yet");
        }
        return xmppServerInfo;
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
        if (jid != null && name != null && name.equals(jid.getDomain())) {
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
            if (!name.equals(jid.getDomain())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an ID that uniquely identifies this server in a cluster. When not running in cluster mode
     * the returned value is always the same. However, when in cluster mode the value should be set
     * when joining the cluster and must be unique even upon restarts of this node.
     *
     * @return an ID that uniquely identifies this server in a cluster.
     */
    public NodeID getNodeID() {
        return nodeID == null ? DEFAULT_NODE_ID : nodeID;
    }

    /**
     * Sets an ID that uniquely identifies this server in a cluster. When not running in cluster mode
     * the returned value is always the same. However, when in cluster mode the value should be set
     * when joining the cluster and must be unique even upon restarts of this node.
     *
     * @param nodeID an ID that uniquely identifies this server in a cluster or null if not in a cluster.
     */
    public void setNodeID(NodeID nodeID) {
        this.nodeID = nodeID;
    }

    /**
     * Creates an XMPPAddress local to this server.
     *
     * @param username the user name portion of the id or null to indicate none is needed.
     * @param resource the resource portion of the id or null to indicate none is needed.
     * @return an XMPPAddress for the server.
     */
    public JID createJID(String username, String resource) {
        return new JID(username, name, resource);
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
        return new JID(username, name, resource, skipStringprep);
    }

    /**
     * Returns a collection with the JIDs of the server's admins. The collection may include
     * JIDs of local users and users of remote servers.
     *
     * @return a collection with the JIDs of the server's admins.
     */
    public Collection<JID> getAdmins() {
        return AdminManager.getInstance().getAdminAccounts();
    }

    /**
     * Adds a new server listener that will be notified when the server has been started
     * or is about to be stopped.
     *
     * @param listener the new server listener to add.
     */
    public void addServerListener(IXmppServerListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a server listener that was being notified when the server was being started
     * or was about to be stopped.
     *
     * @param listener the server listener to remove.
     */
    public void removeServerListener(IXmppServerListener listener) {
        listeners.remove(listener);
    }

    private void initialize() throws FileNotFoundException {
        locatecrossfire();

        name = Globals.getProperty("xmpp.domain", "127.0.0.1").toLowerCase();

        try {
            host = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException ex) {
            Log.warn("Unable to determine local hostname.", ex);
        }

        version = new Version(0, 5, 1, Version.ReleaseStatus.Alpha, -1);
        if ("true".equals(Globals.getXMLProperty("setup"))) {
            setupMode = false;
        }

        if (isStandAlone()) {
            Runtime.getRuntime().addShutdownHook(new XmppServerShutdownHookThread(this));
        }

        loader = Thread.currentThread().getContextClassLoader();

        try {
            CacheFactory.initialize();
        } catch (InitializationException e) {
            e.printStackTrace();
            Log.error(e.getMessage(), e);
        }

        initialized = true;
    }

    /**
     * Finish the setup process. Because this method is meant to be called from inside
     * the Admin console plugin, it spawns its own thread to do the work so that the
     * class loader is correct.
     */
    public void finishSetup() {
        if (!setupMode) {
            return;
        }
        // Make sure that setup finished correctly.
        if ("true".equals(Globals.getXMLProperty("setup"))) {
            // Set the new server domain assigned during the setup process
            name = Globals.getProperty("xmpp.domain").toLowerCase();
            xmppServerInfo.setXMPPDomain(name);

            // Initialize list of admins now (before we restart Jetty)
            AdminManager.getInstance().getAdminAccounts();

            Thread finishSetup = new XmppServerFinalSetupThread(this);
            // Use the correct class loader.
            finishSetup.setContextClassLoader(loader);
            finishSetup.start();
            // We can now safely indicate that setup has finished
            setupMode = false;

            // Update server info
            xmppServerInfo = new XMPPServerInfoImpl(name, host, version, startDate, getConnectionManager());
        }
    }

    public void start() {
        try {
            initialize();

            startDate = new Date();
            // Store server info
            xmppServerInfo = new XMPPServerInfoImpl(name, host, version, startDate, getConnectionManager());

            // Create PluginManager now (but don't start it) so that modules may use it
            File pluginDir = new File(crossfireHome, "plugins");
            pluginManager = new PluginManager(pluginDir);

            // If the server has already been setup then we can start all the server's modules
            if (!setupMode) {
                verifyDataSource();
                // First load all the modules so that modules may access other modules while
                // being initialized
                loadModules();
                // Initize all the modules
                initModules();
                // Start all the modules
                startModules();
            }

            // Load plugins (when in setup mode only the admin console will be loaded)
            pluginManager.start();

            // Log that the server has been started
            String startupBanner = LocaleUtils.getLocalizedString("short.title") + " " + version.getVersionString() +
                    " [" + Globals.formatDateTime(new Date()) + "]";
            Log.info(startupBanner);
            System.out.println(startupBanner);

            started = true;
            
            // Notify server listeners that the server has been started
            for (IXmppServerListener listener : listeners) {
                listener.serverStarted();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.error(e.getMessage(), e);
            System.out.println(LocaleUtils.getLocalizedString("startup.error"));
            shutdownServer();
        }
    }

    protected void loadModules() {
        // Load boot modules
        loadModule(RoutingTableImpl.class.getName());
        loadModule(RosterManager.class.getName());
        // Load core modules
        loadModule(PresenceManagerImpl.class.getName());
        loadModule(SessionManager.class.getName());
        loadModule(PacketRouterImpl.class.getName());
        loadModule(IQRouter.class.getName());
        loadModule(MessageRouter.class.getName());
        loadModule(PresenceRouter.class.getName());
        loadModule(MulticastRouter.class.getName());
        loadModule(PacketTransporterImpl.class.getName());
        loadModule(PacketDelivererImpl.class.getName());
        loadModule(TransportHandler.class.getName());
        loadModule(OfflineMessageStrategy.class.getName());
        loadModule(OfflineMessageStore.class.getName());
        // Load standard modules
        loadModule(IqBindHandler.class.getName());
        loadModule(IQSessionEstablishmentHandler.class.getName());
        loadModule(IqAuthHandler.class.getName());
        loadModule(IqPingHandler.class.getName());
        loadModule(IqRegisterHandler.class.getName());
        loadModule(IQRosterHandler.class.getName());
        loadModule(IqTimeHandler.class.getName());
        loadModule(IqVersionHandler.class.getName());
        loadModule(IqLastActivityHandler.class.getName());
        loadModule(PresenceSubscribeHandler.class.getName());
        loadModule(PresenceUpdateHandler.class.getName());
        loadModule(IQOfflineMessagesHandler.class.getName());
        loadModule(MulticastDNSService.class.getName());
        loadModule(IqSharedGroupHandler.class.getName());
        loadModule(IQPrivacyHandler.class.getName());
        loadModule(IQDiscoInfoHandler.class.getName());
        loadModule(IQDiscoItemsHandler.class.getName());
        // Load this module always last since we don't want to start listening for clients
        // before the rest of the modules have been started
        loadModule(ConnectionManagerImpl.class.getName());
    }

    /**
     * Loads a module.
     *
     * @param module the name of the class that implements the IModule interface.
     */
    private void loadModule(String module) {
        try {
            Class modClass = loader.loadClass(module);
            IModule mod = (IModule) modClass.newInstance();
            this.modules.put(modClass, mod);
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
        }
    }

    protected void initModules() {
        for (IModule module : modules.values()) {
            boolean isInitialized = false;
            try {
                module.initialize(this);
                isInitialized = true;
            }
            catch (Exception e) {
                e.printStackTrace();
                // Remove the failed initialized module
                this.modules.remove(module.getClass());
                if (isInitialized) {
                    module.stop();
                    module.destroy();
                }
                Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
            }
        }
    }

    /**
     * <p>Following the loading and initialization of all the modules
     * this method is called to iterate through the known modules and
     * start them.</p>
     */
    protected void startModules() {
        for (IModule module : modules.values()) {
            boolean started = false;
            try {
                module.start();
            }
            catch (Exception e) {
                if (started && module != null) {
                    module.stop();
                    module.destroy();
                }
                Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
            }
        }
    }

    /**
     * Restarts the server and all it's modules only if the server is restartable. Otherwise do
     * nothing.
     */
    public void restart() {
        if (isStandAlone() && isRestartable()) {
            try {
                Class wrapperClass = Class.forName(WRAPPER_CLASSNAME);
                Method restartMethod = wrapperClass.getMethod("restart", (Class []) null);
                restartMethod.invoke(null, (Object []) null);
            }
            catch (Exception e) {
                Log.error("Could not restart container", e);
            }
        }
    }

    /**
     * Restarts the HTTP server only when running in stand alone mode. The restart
     * process will be done in another thread that will wait 1 second before doing
     * the actual restart. The delay will give time to the page that requested the
     * restart to fully render its content.
     */
    public void restartHTTPServer() {
        Thread restartThread = new XmppServerRestartThread(this);
        restartThread.setContextClassLoader(loader);
        restartThread.start();
    }

    /**
     * Stops the server only if running in standalone mode. Do nothing if the server is running
     * inside of another server.
     */
    public void stop() {
        // Only do a system exit if we're running standalone
        if (isStandAlone()) {
            // if we're in a wrapper, we have to tell the wrapper to shut us down
            if (isRestartable()) {
                try {
                    Class wrapperClass = Class.forName(WRAPPER_CLASSNAME);
                    Method stopMethod = wrapperClass.getMethod("stop", Integer.TYPE);
                    stopMethod.invoke(null, 0);
                }
                catch (Exception e) {
                    Log.error("Could not stop container", e);
                }
            }
            else {
                shutdownServer();
                Thread shutdownThread = new XmppServerShutdownThread();
                shutdownThread.setDaemon(true);
                shutdownThread.start();
            }
        }
        else {
            // Close listening socket no matter what the condition is in order to be able
            // to be restartable inside a container.
            shutdownServer();
        }
    }

    public boolean isSetupMode() {
        return setupMode;
    }

    public boolean isRestartable() {
        boolean restartable;
        try {
            restartable = Class.forName(WRAPPER_CLASSNAME) != null;
        }
        catch (ClassNotFoundException e) {
            restartable = false;
        }
        return restartable;
    }

    /**
     * Returns if the server is running in standalone mode. We consider that it's running in
     * standalone if the "org.b5chat.crossfire.core.starter.ServerStarter" class is present in the
     * system.
     *
     * @return true if the server is running in standalone mode.
     */
    public boolean isStandAlone() {
        boolean standalone;
        try {
            standalone = Class.forName(STARTER_CLASSNAME) != null;
        }
        catch (ClassNotFoundException e) {
            standalone = false;
        }
        return standalone;
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
            Log.error("Database could not be accessed", e);
            throw new IllegalArgumentException(e);
        }
        finally {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
    }

    /**
     * Verifies that the given home guess is a real crossfire home directory.
     * We do the verification by checking for the crossfire config file in
     * the config dir of b5chatHome.
     *
     * @param homeGuess a guess at the path to the home directory.
     * @param b5chatConfigName the name of the config file to check.
     * @return a file pointing to the home directory or null if the
     *         home directory guess was wrong.
     * @throws java.io.FileNotFoundException if there was a problem with the home
     *                                       directory provided
     */
    private File verifyHome(String homeGuess, String b5chatConfigName) throws FileNotFoundException {
        File crossfireHome = new File(homeGuess);
        File configFile = new File(crossfireHome, b5chatConfigName);
        if (!configFile.exists()) {
            throw new FileNotFoundException();
        }
        else {
            try {
                return new File(crossfireHome.getCanonicalPath());
            }
            catch (Exception ex) {
                throw new FileNotFoundException();
            }
        }
    }

    /**
     * <p>Retrieve the b5chat home for the container.</p>
     *
     * @throws FileNotFoundException If b5chatHome could not be located
     */
    private void locatecrossfire() throws FileNotFoundException {
        String b5chatConfigName = "conf" + File.separator + "crossfire.xml";
        // First, try to load it crossfireHome as a system property.
        if (crossfireHome == null) {
            String homeProperty = System.getProperty("crossfireHome");
            try {
                if (homeProperty != null) {
                    crossfireHome = verifyHome(homeProperty, b5chatConfigName);
                }
            }
            catch (FileNotFoundException fe) {
                // Ignore.
            }
        }

        // If we still don't have home, let's assume this is standalone
        // and just look for home in a standard sub-dir location and verify
        // by looking for the config file
        if (crossfireHome == null) {
            try {
                crossfireHome = verifyHome("..", b5chatConfigName).getCanonicalFile();
            }
            catch (FileNotFoundException fe) {
                // Ignore.
            }
            catch (IOException ie) {
                // Ignore.
            }
        }

        // If home is still null, no outside process has set it and
        // we have to attempt to load the value from crossfire_init.xml,
        // which must be in the classpath.
        if (crossfireHome == null) {
            InputStream in = null;
            try {
                in = getClass().getResourceAsStream("/crossfire_init.xml");
                if (in != null) {
                    SAXReader reader = new SAXReader();
                    Document doc = reader.read(in);
                    String path = doc.getRootElement().getText();
                    try {
                        if (path != null) {
                            crossfireHome = verifyHome(path, b5chatConfigName);
                        }
                    }
                    catch (FileNotFoundException fe) {
                        fe.printStackTrace();
                    }
                }
            }
            catch (Exception e) {
                System.err.println("Error loading crossfire_init.xml to find home.");
                e.printStackTrace();
            }
            finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                }
                catch (Exception e) {
                    System.err.println("Could not close open connection");
                    e.printStackTrace();
                }
            }
        }

        if (crossfireHome == null) {
            System.err.println("Could not locate home");
            throw new FileNotFoundException();
        }
        else {
            // Set the home directory for the config file
            Globals.setHomeDirectory(crossfireHome.toString());
            // Set the name of the config file
            Globals.setConfigName(b5chatConfigName);
        }
    }
    
    /**
     * Makes a best effort attempt to shutdown the server
     */
    protected void shutdownServer() {
        shuttingDown = true;
        // Notify server listeners that the server is about to be stopped
        for (IXmppServerListener listener : listeners) {
            listener.serverStopping();
        }
        // Shutdown the task engine.
        TaskEngine.getInstance().shutdown();

        // If we don't have modules then the server has already been shutdown
        if (modules.isEmpty()) {
            return;
        }
        // Get all modules and stop and destroy them
        for (IModule module : modules.values()) {
            module.stop();
            module.destroy();
        }
        // Stop all plugins
        if (pluginManager != null) {
            pluginManager.shutdown();
        }
        modules.clear();
        // Stop the Db connection manager.
        DbConnectionManager.destroyConnectionProvider();
        // hack to allow safe stopping
        Log.info("crossfire stopped");
    }
    
    /**
     * Returns true if the server is being shutdown.
     *
     * @return true if the server is being shutdown.
     */
    public boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * Returns the <code>IConnectionManager</code> registered with this server. The
     * <code>IConnectionManager</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>IConnectionManager</code> registered with this server.
     */
    public IConnectionManager getConnectionManager() {
        return (IConnectionManager) modules.get(ConnectionManagerImpl.class);
    }

    /**
     * Returns the <code>IRoutingTable</code> registered with this server. The
     * <code>IRoutingTable</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>IRoutingTable</code> registered with this server.
     */
    public IRoutingTable getRoutingTable() {
        return (IRoutingTable) modules.get(RoutingTableImpl.class);
    }

    /**
     * Returns the <code>IPacketDeliverer</code> registered with this server. The
     * <code>IPacketDeliverer</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>IPacketDeliverer</code> registered with this server.
     */
    public IPacketDeliverer getPacketDeliverer() {
        return (IPacketDeliverer) modules.get(PacketDelivererImpl.class);
    }

    /**
     * Returns the <code>RosterManager</code> registered with this server. The
     * <code>RosterManager</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>RosterManager</code> registered with this server.
     */
    public RosterManager getRosterManager() {
        return (RosterManager) modules.get(RosterManager.class);
    }

    /**
     * Returns the <code>IPresenceManager</code> registered with this server. The
     * <code>IPresenceManager</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>IPresenceManager</code> registered with this server.
     */
    public IPresenceManager getPresenceManager() {
        return (IPresenceManager) modules.get(PresenceManagerImpl.class);
    }

    /**
     * Returns the <code>OfflineMessageStore</code> registered with this server. The
     * <code>OfflineMessageStore</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>OfflineMessageStore</code> registered with this server.
     */
    public OfflineMessageStore getOfflineMessageStore() {
        return (OfflineMessageStore) modules.get(OfflineMessageStore.class);
    }

    /**
     * Returns the <code>OfflineMessageStrategy</code> registered with this server. The
     * <code>OfflineMessageStrategy</code> was registered with the server as a module while starting
     * up the server.
     *
     * @return the <code>OfflineMessageStrategy</code> registered with this server.
     */
    public OfflineMessageStrategy getOfflineMessageStrategy() {
        return (OfflineMessageStrategy) modules.get(OfflineMessageStrategy.class);
    }

    /**
     * Returns the <code>IPacketRouter</code> registered with this server. The
     * <code>IPacketRouter</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>IPacketRouter</code> registered with this server.
     */
    public IPacketRouter getPacketRouter() {
        return (IPacketRouter) modules.get(PacketRouterImpl.class);
    }

    /**
     * Returns the <code>IqRegisterHandler</code> registered with this server. The
     * <code>IqRegisterHandler</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>IqRegisterHandler</code> registered with this server.
     */
    public IqRegisterHandler getIQRegisterHandler() {
        return (IqRegisterHandler) modules.get(IqRegisterHandler.class);
    }

    /**
     * Returns the <code>IqAuthHandler</code> registered with this server. The
     * <code>IqAuthHandler</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>IqAuthHandler</code> registered with this server.
     */
    public IqAuthHandler getIQAuthHandler() {
        return (IqAuthHandler) modules.get(IqAuthHandler.class);
    }
    
    /**
     * Returns the <code>PluginManager</code> instance registered with this server.
     *
     * @return the PluginManager instance.
     */
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    /**
     * Returns a list with all the modules registered with the server that inherit from IqHandler.
     *
     * @return a list with all the modules registered with the server that inherit from IqHandler.
     */
    public List<IqHandler> getIQHandlers() {
        List<IqHandler> answer = new ArrayList<IqHandler>();
        for (IModule module : modules.values()) {
            if (module instanceof IqHandler) {
                answer.add((IqHandler) module);
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
        return (SessionManager) modules.get(SessionManager.class);
    }

    /**
     * Returns the <code>TransportHandler</code> registered with this server. The
     * <code>TransportHandler</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>TransportHandler</code> registered with this server.
     */
    public TransportHandler getTransportHandler() {
        return (TransportHandler) modules.get(TransportHandler.class);
    }

    /**
     * Returns the <code>PresenceUpdateHandler</code> registered with this server. The
     * <code>PresenceUpdateHandler</code> was registered with the server as a module while starting
     * up the server.
     *
     * @return the <code>PresenceUpdateHandler</code> registered with this server.
     */
    public PresenceUpdateHandler getPresenceUpdateHandler() {
        return (PresenceUpdateHandler) modules.get(PresenceUpdateHandler.class);
    }

    /**
     * Returns the <code>PresenceSubscribeHandler</code> registered with this server. The
     * <code>PresenceSubscribeHandler</code> was registered with the server as a module while
     * starting up the server.
     *
     * @return the <code>PresenceSubscribeHandler</code> registered with this server.
     */
    public PresenceSubscribeHandler getPresenceSubscribeHandler() {
        return (PresenceSubscribeHandler) modules.get(PresenceSubscribeHandler.class);
    }

    /**
     * Returns the <code>IQRouter</code> registered with this server. The
     * <code>IQRouter</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>IQRouter</code> registered with this server.
     */
    public IQRouter getIQRouter() {
        return (IQRouter) modules.get(IQRouter.class);
    }

    /**
     * Returns the <code>MessageRouter</code> registered with this server. The
     * <code>MessageRouter</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>MessageRouter</code> registered with this server.
     */
    public MessageRouter getMessageRouter() {
        return (MessageRouter) modules.get(MessageRouter.class);
    }

    /**
     * Returns the <code>PresenceRouter</code> registered with this server. The
     * <code>PresenceRouter</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>PresenceRouter</code> registered with this server.
     */
    public PresenceRouter getPresenceRouter() {
        return (PresenceRouter) modules.get(PresenceRouter.class);
    }

    /**
     * Returns the <code>MulticastRouter</code> registered with this server. The
     * <code>MulticastRouter</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>MulticastRouter</code> registered with this server.
     */
    public MulticastRouter getMulticastRouter() {
        return (MulticastRouter) modules.get(MulticastRouter.class);
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
        for (IModule module : modules.values()) {
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
        for (IModule module : modules.values()) {
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
        for (IModule module : modules.values()) {
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
        for (IModule module : modules.values()) {
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
        for (IModule module : modules.values()) {
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
        return (IQDiscoInfoHandler) modules.get(IQDiscoInfoHandler.class);
    }

    /**
     * Returns the <code>IQDiscoItemsHandler</code> registered with this server. The
     * <code>IQDiscoItemsHandler</code> was registered with the server as a module while starting up
     * the server.
     *
     * @return the <code>IQDiscoItemsHandler</code> registered with this server.
     */
    public IQDiscoItemsHandler getIQDiscoItemsHandler() {
        return (IQDiscoItemsHandler) modules.get(IQDiscoItemsHandler.class);
    }

    /**
     * Returns the locator to use to find sessions hosted in other cluster nodes. When not running
     * in a cluster a <tt>null</tt> value is returned.
     *
     * @return the locator to use to find sessions hosted in other cluster nodes.
     */
    public IRemoteSessionLocator getRemoteSessionLocator() {
        return remoteSessionLocator;
    }

    /**
     * Sets the locator to use to find sessions hosted in other cluster nodes. When not running
     * in a cluster set a <tt>null</tt> value.
     *
     * @param remoteSessionLocator the locator to use to find sessions hosted in other cluster nodes.
     */
    public void setRemoteSessionLocator(IRemoteSessionLocator remoteSessionLocator) {
        this.remoteSessionLocator = remoteSessionLocator;
    }

    /**
     * Returns whether or not the server has been started.
     * 
     * @return whether or not the server has been started.
     */
    public boolean isStarted() {
        return started;
    }
}