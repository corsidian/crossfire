/**
 * $RCSfile$
 * $Revision: 1583 $
 * $Date: 2005-07-03 17:55:39 -0300 (Sun, 03 Jul 2005) $
 *
 * Copyright (C) 2004-2008 EMIVA Community. All rights reserved.
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

package net.emiva.crossfire;

import net.emiva.crossfire.core.net.SocketReader;
import net.emiva.crossfire.server.ServerPort;

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;

/**
 * Coordinates connections (accept, read, termination) on the server.
 *
 * @author Iain Shigeoka
 */
public interface IConnectionManager {

    /**
     * The default XMPP port for clients. This port can be used with secured
     * and unsecured connections. Clients will initially connect using an unsecure
     * connection and may secure it by using StartTLS.
     */
    final int DEFAULT_PORT = 5222;
    /**
     * The default legacy Jabber port for SSL traffic. This old method, and soon
     * to be deprecated, uses encrypted connections as soon as they are created.
     */
    final int DEFAULT_SSL_PORT = 5223;

    /**
     * Returns an array of the ports managed by this connection manager.
     *
     * @return an iterator of the ports managed by this connection manager
     *      (can be an empty but never null).
     */
    public Collection<ServerPort> getPorts();

    /**
     * Sets if the port listener for unsecured clients will be available or not. When disabled
     * there won't be a port listener active. Therefore, new clients won't be able to connect to
     * the server.
     *
     * @param enabled true if new unsecured clients will be able to connect to the server.
     */
    public void enableClientListener(boolean enabled);

    /**
     * Returns true if the port listener for unsecured clients is available. When disabled
     * there won't be a port listener active. Therefore, new clients won't be able to connect to
     * the server.
     *
     * @return true if the port listener for unsecured clients is available.
     */
    public boolean isClientListenerEnabled();

    /**
     * Sets if the port listener for secured clients will be available or not. When disabled
     * there won't be a port listener active. Therefore, new secured clients won't be able to
     * connect to the server.
     *
     * @param enabled true if new secured clients will be able to connect to the server.
     */
    public void enableClientSSLListener(boolean enabled);

    /**
     * Returns true if the port listener for secured clients is available. When disabled
     * there won't be a port listener active. Therefore, new secured clients won't be able to
     * connect to the server.
     *
     * @return true if the port listener for unsecured clients is available.
     */
    public boolean isClientSSLListenerEnabled();

    /**
     * Sets the port to use for unsecured clients. Default port: 5222.
     *
     * @param port the port to use for unsecured clients.
     */
    public void setClientListenerPort(int port);

    /**
     * Returns the port to use for unsecured clients. Default port: 5222.
     *
     * @return the port to use for unsecured clients.
     */
    public int getClientListenerPort();

    /**
     * Sets the port to use for secured clients. Default port: 5223.
     *
     * @param port the port to use for secured clients.
     */
    public void setClientSSLListenerPort(int port);

    /**
     * Returns the port to use for secured clients. Default port: 5223.
     *
     * @return the port to use for secured clients.
     */
    public int getClientSSLListenerPort();
}
