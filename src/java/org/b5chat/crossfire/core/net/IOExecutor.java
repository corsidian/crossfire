/**
 * $RCSfile$
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

package org.b5chat.crossfire.core.net;


import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.b5chat.util.Globals;

/**
 * Thread pool to be used for processing incoming packets when using non-blocking
 * connections.
 *
 * // TODO Change thead pool configuration. Would be nice to have something that can be
 * // TODO dynamically adjusted to demand and circumstances.
 *
 * @author Daniele Piras
 */
class IOExecutor {

    // SingleTon ...
    protected static IOExecutor instance = new IOExecutor();

    // Pool obj
    protected ThreadPoolExecutor executeMsgPool;

    // Internal queue for the pool
    protected LinkedBlockingQueue<Runnable> executeQueue;


    /*
    * Simple constructor that initialize the main executor structure.
    *
    */
    protected IOExecutor() {
        // Read poolsize parameter...
        int poolSize = Globals.getIntProperty("tiscali.pool.size", 15);
        // Create queue for executor
        executeQueue = new LinkedBlockingQueue<Runnable>(10000);
        // Create executor
        executeMsgPool =
                new ThreadPoolExecutor(poolSize, poolSize, 60, TimeUnit.SECONDS, executeQueue);
    }

    public static void execute(Runnable task) {
        instance.executeMsgPool.execute(task);
    }

}