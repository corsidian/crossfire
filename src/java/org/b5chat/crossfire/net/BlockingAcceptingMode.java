/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2005-2008 EMIVA Community. All rights reserved.
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
import java.net.ServerSocket;
import java.net.Socket;


import org.b5chat.crossfire.ConnectionManager;
import org.b5chat.crossfire.ServerPort;
import org.b5chat.util.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Accepts new socket connections and uses a thread for each new connection.
 *
 * @author Gaston Dombiak
 */
class BlockingAcceptingMode extends SocketAcceptingMode {

	private static final Logger Log = LoggerFactory.getLogger(BlockingAcceptingMode.class);

    protected BlockingAcceptingMode(ConnectionManager connManager, ServerPort serverPort,
            InetAddress bindInterface) throws IOException {
        super(connManager, serverPort);
        serverSocket = new ServerSocket(serverPort.getPort(), -1, bindInterface);
    }

    /**
     * About as simple as it gets.  The thread spins around an accept
     * call getting sockets and creating new reading threads for each new connection.
     */
    @Override
	public void run() {
        while (notTerminated) {
            try {
                Socket sock = serverSocket.accept();
                if (sock != null) {
                    Log.debug("Connect " + sock.toString());
                    SocketReader reader =
                            connManager.createSocketReader(sock, false, serverPort, true);
                    Thread thread = new Thread(reader, reader.getName());
                    thread.setDaemon(true);
                    thread.setPriority(Thread.NORM_PRIORITY);
                    thread.start();
                }
            }
            catch (IOException ie) {
                if (notTerminated) {
                    Log.error(LocaleUtils.getLocalizedString("admin.error.accept"),
                            ie);
                }
            }
            catch (Throwable e) {
                Log.error(LocaleUtils.getLocalizedString("admin.error.accept"), e);
            }
        }
    }
}