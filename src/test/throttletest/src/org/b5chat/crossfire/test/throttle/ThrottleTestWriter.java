/**
 * $RCSfile$
 * $Revision: 3144 $
 * $Date: 2005-12-01 14:20:11 -0300 (Thu, 01 Dec 2005) $
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

package net.emiva.crossfire.test.throttle;

import net.emiva.smack.XMPPConnection;
import net.emiva.smack.PacketCollector;
import net.emiva.smack.ConnectionConfiguration;
import net.emiva.smack.filter.PacketIDFilter;
import net.emiva.smack.packet.IQ;
import net.emiva.smack.packet.Packet;
import net.emiva.smack.packet.Message;
import net.emiva.smackx.packet.Time;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple client to test XMPP server throttling. When server throttling is working
 * properly, a server should slow down incoming packets to match the speed of outgoing
 * packets (otherwise, memory would fill up until a server crash).<p/>
 *
 * This client should be deployed as follows:
 * <pre>
 * [ writer ] -- fast connection --> [ xmpp server ] -- slow connection --> reader
 * </pre>
 *
 * A good way to simulate fast and slow connections is to use virtual machines where
 * the network interface speed can be set (to simulate a modem, etc).
 *
 * java ThrottleTestWriter [server] [username] [password]
 *
 * @author Matt Tucker
 */
public class ThrottleTestWriter {

    private static boolean done = false;
    private static AtomicInteger packetCount = new AtomicInteger(0);

    /**
     * Starts the throttle test write client.
     *
     * @param args application arguments.
     */
    public static void main(String [] args) {
        if (args.length != 3) {
            System.out.println("Usage: java ThrottleTestWriter [server] [username] [password]");
            System.exit(0);
        }
        String server = args[0];
        String username = args[1];
        String password = args[2];
        try {
            // Connect to the server, without TLS encryption.
            ConnectionConfiguration config = new ConnectionConfiguration(server);
            config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
            final XMPPConnection con = new XMPPConnection(config);
            System.out.print("Connecting to " + server + "... ");
            con.connect();

            con.login(username, password, "writer");
            System.out.print("success.");
            System.out.println("");

            // Get the "real" server address.
            server = con.getServiceName();

            String writerAddress = username + "@" + server + "/writer";
            final String readerAddress = username + "@" + server + "/reader";

            System.out.println("Registered as " + writerAddress);

            // Look for the reader process.
            System.out.print("Looking for " + readerAddress + "...");
            while (true) {
                IQ testIQ = new Time();
                testIQ.setType(IQ.Type.GET);
                testIQ.setTo(readerAddress);
                PacketCollector collector = con.createPacketCollector(new PacketIDFilter(testIQ.getPacketID()));
                con.sendPacket(testIQ);
                // Wait 5 seconds.
                long start = System.currentTimeMillis();
                Packet result = collector.nextResult(5000);
                collector.cancel();
                // If we got a result, continue.
                if (result != null && result.getError() == null) {
                    System.out.println(" found reader. Starting packet flood.");
                    break;
                }
                System.out.print(".");
                long end = System.currentTimeMillis();
                if (end - start < 5000) {
                    try {
                        Thread.sleep(5000 - (end-start));
                    }
                    catch (Exception e) {
                        // ignore.
                    }
                }
            }

            // Create a process to log how many packets we're writing out.
            Runnable statsRunnable = new Runnable() {

                public void run() {
                    while (!done) {
                        try {
                            Thread.sleep(5000);
                        }
                        catch (Exception e) { /* ignore */ }
                        int count = packetCount.getAndSet(0);
                        System.out.println("Packets per second: " + (count/5));
                    }
                }
            };
            Thread statsThread = new Thread(statsRunnable);
            statsThread.setDaemon(true);
            statsThread.start();

            // Now start flooding packets.
            Message testMessage = new Message(readerAddress);
            testMessage.setBody("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
            while (!done) {
                con.sendPacket(testMessage);
                packetCount.getAndIncrement();
            }
        }
        catch (Exception e) {
            System.out.println("\nError: " + e.getMessage());
            e.printStackTrace();
        }
    }
}