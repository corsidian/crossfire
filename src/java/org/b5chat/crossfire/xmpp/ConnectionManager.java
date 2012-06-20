/**
 * $RCSfile: ConnectionManager.java,v $
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2005-2008 B5Chat Community. All rights reserved.
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

package org.b5chat.crossfire.xmpp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ExecutorThreadModel;
import org.apache.mina.common.SimpleByteBufferAllocator;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;
import org.b5chat.crossfire.core.container.BasicModule;
import org.b5chat.crossfire.core.net.SocketSendingTracker;
import org.b5chat.crossfire.core.net.StalledSessionsFilter;
import org.b5chat.crossfire.core.nio.ClientConnectionHandler;
import org.b5chat.crossfire.core.nio.XMPPCodecFactory;
import org.b5chat.crossfire.core.plugin.IPluginManagerListener;
import org.b5chat.crossfire.core.plugin.PluginManager;
import org.b5chat.crossfire.core.property.Globals;
import org.b5chat.crossfire.core.server.ServerPort;
import org.b5chat.crossfire.core.util.LocaleUtils;
import org.b5chat.crossfire.xmpp.route.IPacketDeliverer;
import org.b5chat.crossfire.xmpp.route.IPacketRouter;
import org.b5chat.crossfire.xmpp.server.XmppServer;
import org.b5chat.crossfire.xmpp.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionManager extends BasicModule implements IConnectionManager {

	private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private SocketAcceptor socketAcceptor;
    private ArrayList<ServerPort> ports;

    private SessionManager sessionManager;
    private IPacketDeliverer deliverer;
    private IPacketRouter router;
    private String serverName;
    private String localIPAddress = null;

    // Used to know if the sockets have been started
    private boolean isSocketStarted = false;

    public ConnectionManager() {
        super("IConnection Manager");
        ports = new ArrayList<ServerPort>(4);
    }

    private synchronized void createListeners() {
        if (isSocketStarted || sessionManager == null || deliverer == null || router == null || serverName == null) {
            return;
        }
        // Create the port listener for clients
        createClientListeners();
    }

    private synchronized void startListeners() {
        if (isSocketStarted || sessionManager == null || deliverer == null || router == null || serverName == null) {
            return;
        }

        // Check if plugins have been loaded
        PluginManager pluginManager = XmppServer.getInstance().getPluginManager();
        if (!pluginManager.isExecuted()) {
            pluginManager.addPluginManagerListener(new IPluginManagerListener() {
                public void pluginsMonitored() {
                    // Stop listening for plugin events
                    XmppServer.getInstance().getPluginManager().removePluginManagerListener(this);
                    // Start listeners
                    startListeners();
                }
            });
            return;
        }

        isSocketStarted = true;

        // Setup port info
        try {
            localIPAddress = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e) {
        	logger.warn("Setup port info", e);
        	if (localIPAddress == null) {
                localIPAddress = "Unknown";
            }
        }
        // Start the port listener for clients
        startClientListeners(localIPAddress);
    }

    private void createClientListeners() {
        // Start clients plain socket unless it's been disabled.
        if (isClientListenerEnabled()) {
            // Create SocketAcceptor with correct number of processors
            socketAcceptor = buildSocketAcceptor();
            // Customize Executor that will be used by processors to process incoming stanzas
            ExecutorThreadModel threadModel = ExecutorThreadModel.getInstance("client");
            int eventThreads = Globals.getIntProperty("xmpp.client.processing.threads", 16);
            ThreadPoolExecutor eventExecutor = (ThreadPoolExecutor)threadModel.getExecutor();
            eventExecutor.setCorePoolSize(eventThreads + 1);
            eventExecutor.setMaximumPoolSize(eventThreads + 1);
            eventExecutor.setKeepAliveTime(60, TimeUnit.SECONDS);

            socketAcceptor.getDefaultConfig().setThreadModel(threadModel);
            // Add the XMPP codec filter
            socketAcceptor.getFilterChain().addFirst("xmpp", new ProtocolCodecFilter(new XMPPCodecFactory()));
            // Kill sessions whose outgoing queues keep growing and fail to send traffic
            socketAcceptor.getFilterChain().addAfter("xmpp", "outCap", new StalledSessionsFilter());
        }
    }

    private void startClientListeners(String localIPAddress) {
        // Start clients plain socket unless it's been disabled.
        if (isClientListenerEnabled()) {
            int port = getClientListenerPort();
            try {
                // Listen on a specific network interface if it has been set.
                String interfaceName = Globals.getXMLProperty("network.interface");
                InetAddress bindInterface = null;
                if (interfaceName != null) {
                    if (interfaceName.trim().length() > 0) {
                        bindInterface = InetAddress.getByName(interfaceName);
                    }
                }
                // Start accepting connections
                socketAcceptor
                        .bind(new InetSocketAddress(bindInterface, port), new ClientConnectionHandler(serverName));

                ports.add(new ServerPort(port, serverName, localIPAddress, false, null, ServerPort.Type.client));

                List<String> params = new ArrayList<String>();
                params.add(Integer.toString(port));
                logger.info(LocaleUtils.getLocalizedString("startup.plain", params));
            }
            catch (Exception e) {
                System.err.println("Error starting XMPP listener on port " + port + ": " +
                        e.getMessage());
                logger.error(LocaleUtils.getLocalizedString("admin.error.socket-setup"), e);
            }
        }
    }

    private void stopClientListeners() {
        if (socketAcceptor != null) {
            socketAcceptor.unbindAll();
            for (ServerPort port : ports) {
                if (port.isClientPort() && !port.isSecure()) {
                    ports.remove(port);
                    break;
                }
            }
            socketAcceptor = null;
        }
    }

    public Collection<ServerPort> getPorts() {
        return ports;
    }

    @Override
	public void initialize(XmppServer server) {
        super.initialize(server);
        serverName = server.getServerInfo().getXMPPDomain();
        router = server.getPacketRouter();
        deliverer = server.getPacketDeliverer();
        sessionManager = server.getSessionManager();
        // Check if we need to configure MINA to use Direct or Heap Buffers
        // Note: It has been reported that heap buffers are 50% faster than direct buffers
        if (Globals.getBooleanProperty("xmpp.socket.heapBuffer", true)) {
            ByteBuffer.setUseDirectBuffers(false);
            ByteBuffer.setAllocator(new SimpleByteBufferAllocator());
        }
    }

    public void enableClientListener(boolean enabled) {
        if (enabled == isClientListenerEnabled()) {
            // Ignore new setting
            return;
        }
        if (enabled) {
            Globals.setProperty("xmpp.socket.plain.active", "true");
            // Start the port listener for clients
            createClientListeners();
            startClientListeners(localIPAddress);
        }
        else {
            Globals.setProperty("xmpp.socket.plain.active", "false");
            // Stop the port listener for clients
            stopClientListeners();
        }
    }

    public boolean isClientListenerEnabled() {
        return Globals.getBooleanProperty("xmpp.socket.plain.active", true);
    }

    public void setClientListenerPort(int port) {
        if (port == getClientListenerPort()) {
            // Ignore new setting
            return;
        }
        Globals.setProperty("xmpp.socket.plain.port", String.valueOf(port));
        // Stop the port listener for clients
        stopClientListeners();
        if (isClientListenerEnabled()) {
            // Start the port listener for clients
            createClientListeners();
            startClientListeners(localIPAddress);
        }
    }

    public SocketAcceptor getSocketAcceptor() {
        return socketAcceptor;
    }

    public int getClientListenerPort() {
        return Globals.getIntProperty("xmpp.socket.plain.port", DEFAULT_PORT);
    }

    private SocketAcceptor buildSocketAcceptor() {
        SocketAcceptor socketAcceptor;
        // Create SocketAcceptor with correct number of processors
        int ioThreads = Globals.getIntProperty("xmpp.processor.count", Runtime.getRuntime().availableProcessors());
        // Set the executor that processors will use. Note that processors will use another executor
        // for processing events (i.e. incoming traffic)
        Executor ioExecutor = new ThreadPoolExecutor(
            ioThreads + 1, ioThreads + 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>() );
        socketAcceptor = new SocketAcceptor(ioThreads, ioExecutor);
        // Set that it will be possible to bind a socket if there is a connection in the timeout state
        SocketAcceptorConfig socketAcceptorConfig = socketAcceptor.getDefaultConfig();
        socketAcceptorConfig.setReuseAddress(true);
        // Set the listen backlog (queue) length. Default is 50.
        socketAcceptorConfig.setBacklog(Globals.getIntProperty("xmpp.socket.backlog", 50));

        // Set default (low level) settings for new socket connections
        SocketSessionConfig socketSessionConfig = socketAcceptorConfig.getSessionConfig();
        //socketSessionConfig.setKeepAlive();
        int receiveBuffer = Globals.getIntProperty("xmpp.socket.buffer.receive", -1);
        if (receiveBuffer > 0 ) {
            socketSessionConfig.setReceiveBufferSize(receiveBuffer);
        }
        int sendBuffer = Globals.getIntProperty("xmpp.socket.buffer.send", -1);
        if (sendBuffer > 0 ) {
            socketSessionConfig.setSendBufferSize(sendBuffer);
        }
        int linger = Globals.getIntProperty("xmpp.socket.linger", -1);
        if (linger > 0 ) {
            socketSessionConfig.setSoLinger(linger);
        }
        socketSessionConfig.setTcpNoDelay(
                Globals.getBooleanProperty("xmpp.socket.tcp-nodelay", socketSessionConfig.isTcpNoDelay()));
        return socketAcceptor;
    }

    // #####################################################################
    // IModule management
    // #####################################################################

    @Override
	public void start() {
        super.start();
        createListeners();
        startListeners();
        SocketSendingTracker.getInstance().start();
    }

    @Override
	public void stop() {
        super.stop();
        stopClientListeners();
        SocketSendingTracker.getInstance().shutdown();
        serverName = null;
    }
}
