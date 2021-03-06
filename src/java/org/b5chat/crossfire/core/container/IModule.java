/**
 * $RCSfile$
 * $Revision: 624 $
 * $Date: 2004-12-05 02:38:08 -0300 (Sun, 05 Dec 2004) $
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

package org.b5chat.crossfire.core.container;

import org.b5chat.crossfire.xmpp.server.XmppServer;

/**
 * Logical, server-managed entities must implement this interface. A module
 * represents an operational unit and may contain zero or more services
 * and rely on zero or more services that may be hosted by the container.
 * <p/>
 * In order to be hosted in the b5chat server container, all modules must:
 * </p>
 * <ul>
 * <li>Implement the IModule interface</li>
 * <li>Have a public no-arg constructor</li>
 * </ul>
 * <p/>
 * The b5chat container will run all modules through a simple lifecycle:
 * <pre>
 * constructor -> initialize() -> start() -> stop() -> destroy() -> finalizer
 *                    |<-----------------------|          ^
 *                    |                                   |
 *                    V----------------------------------->
 * </pre>
 * </p>
 * <p/>
 * The IModule interface is intended to provide the simplest mechanism
 * for creating, deploying, and managing server modules.
 * </p>
 *
 * @author Iain Shigeoka
 */
public interface IModule {

    /**
     * Returns the name of the module for display in administration interfaces.
     *
     * @return The name of the module.
     */
    String getName();

    /**
     * Initialize the module with the container.
     * Modules may be initialized and never started, so modules
     * should be prepared for a call to destroy() to follow initialize().
     *
     * @param server the server hosting this module.
     */
    void initialize(XmppServer server);

    /**
     * Start the module (must return quickly). Any long running
     * operations should spawn a thread and allow the method to return
     * immediately.
     */
    void start();

    /**
     * Stop the module. The module should attempt to free up threads
     * and prepare for either another call to initialize (reconfigure the module)
     * or for destruction.
     */
    void stop();

    /**
     * IModule should free all resources and prepare for deallocation.
     */
    void destroy();
}
