/**
 * $RCSfile$
 * $Revision: 1583 $
 * $Date: 2005-07-03 17:55:39 -0300 (Sun, 03 Jul 2005) $
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

package org.b5chat.crossfire.net;



import java.io.IOException;
import java.net.InetAddress;

import org.b5chat.crossfire.ConnectionManager;
import org.b5chat.crossfire.ServerPort;
import org.b5chat.util.Globals;

/**
 * Implements a network front end with a dedicated thread reading
 * each incoming socket. Blocking and non-blocking modes are supported.
 * By default blocking mode is used. Use the <i>xmpp.socket.blocking</i>
 * system property to change the blocking mode. Restart the server after making
 * changes to the system property.
 *
 * @author Gaston Dombiak
 */
public class SocketAcceptThread extends Thread {

    /**
     * Holds information about the port on which the server will listen for connections.
     */
    private ServerPort serverPort;

    private SocketAcceptingMode acceptingMode;

    public SocketAcceptThread(ConnectionManager connManager, ServerPort serverPort)
            throws IOException {
        super("Socket Listener at port " + serverPort.getPort());
        // Listen on a specific network interface if it has been set.
        String interfaceName = Globals.getXMLProperty("network.interface");
        InetAddress bindInterface = null;
        if (interfaceName != null) {
            if (interfaceName.trim().length() > 0) {
                bindInterface = InetAddress.getByName(interfaceName);
                // Create the new server port based on the new bind address
                serverPort = new ServerPort(serverPort.getPort(),
                        serverPort.getDomainNames().get(0), interfaceName, serverPort.isSecure(),
                        serverPort.getSecurityType(), serverPort.getType());
            }
        }
        this.serverPort = serverPort;
        // Set the blocking reading mode to use
        acceptingMode = new BlockingAcceptingMode(connManager, serverPort, bindInterface);
    }

    /**
     * Retrieve the port this server socket is bound to.
     *
     * @return the port the socket is bound to.
     */
    public int getPort() {
        return serverPort.getPort();
    }

    /**
     * Returns information about the port on which the server is listening for connections.
     *
     * @return information about the port on which the server is listening for connections.
     */
    public ServerPort getServerPort() {
        return serverPort;
    }

    /**
     * Unblock the thread and force it to terminate.
     */
    public void shutdown() {
        acceptingMode.shutdown();
    }

    /**
     * About as simple as it gets.  The thread spins around an accept
     * call getting sockets and handing them to the SocketManager.
     */
    @Override
	public void run() {
        acceptingMode.run();
        // We stopped accepting new connections so close the listener
        shutdown();
    }
}
