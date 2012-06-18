/**
 * $RCSfile: IConnection.java,v $
 * $Revision: 3187 $
 * $Date: 2005-12-11 13:34:34 -0300 (Sun, 11 Dec 2005) $
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

package org.b5chat.crossfire;


import org.b5chat.crossfire.auth.UnauthorizedException;
import org.b5chat.crossfire.route.IPacketDeliverer;
import org.b5chat.crossfire.session.LocalSession;
import org.xmpp.packet.Packet;

import java.net.UnknownHostException;
import java.security.cert.Certificate;

/**
 * Represents a connection on the server.
 *
 * @author Iain Shigeoka
 */
public interface IConnection {

    /**
     * Verifies that the connection is still live. Typically this is done by
     * sending a whitespace character between packets.
     *
     * @return true if the socket remains valid, false otherwise.
     */
    public boolean validate();

    /**
     * Initializes the connection with it's owning session. Allows the
     * connection class to configure itself with session related information
     * (e.g. stream ID).
     *
     * @param session the session that owns this connection
     */
    public void init(LocalSession session);

    /**
     * Returns the raw IP address of this <code>InetAddress</code>
     * object. The result is in network byte order: the highest order
     * byte of the address is in <code>getAddress()[0]</code>.
     *
     * @return  the raw IP address of this object.
     * @throws java.net.UnknownHostException if IP address of host could not be determined.
     */
    public byte[] getAddress() throws UnknownHostException;

    /**
     * Returns the IP address string in textual presentation.
     *
     * @return  the raw IP address in a string format.
     * @throws java.net.UnknownHostException if IP address of host could not be determined.
     */
    public String getHostAddress() throws UnknownHostException;

    /**
     * Gets the host name for this IP address.
     *
     * <p>If this InetAddress was created with a host name,
     * this host name will be remembered and returned;
     * otherwise, a reverse name lookup will be performed
     * and the result will be returned based on the system
     * configured name lookup service. If a lookup of the name service
     * is required, call
     * {@link java.net.InetAddress#getCanonicalHostName() getCanonicalHostName}.
     *
     * <p>If there is a security manager, its
     * <code>checkConnect</code> method is first called
     * with the hostname and <code>-1</code>
     * as its arguments to see if the operation is allowed.
     * If the operation is not allowed, it will return
     * the textual representation of the IP address.
     *
     * @return  the host name for this IP address, or if the operation
     *    is not allowed by the security check, the textual
     *    representation of the IP address.
     * @throws java.net.UnknownHostException if IP address of host could not be determined.
     *
     * @see java.net.InetAddress#getCanonicalHostName
     * @see SecurityManager#checkConnect
     */
    public String getHostName() throws UnknownHostException;
    
    /**
     * Close this session including associated socket connection. The order of
     * events for closing the session is:
     * <ul>
     *      <li>Set closing flag to prevent redundant shutdowns.
     *      <li>Call notifyEvent all listeners that the channel is shutting down.
     *      <li>Close the socket.
     * </ul>
     */
    public void close();

    /**
     * Notification message indicating that the server is being shutdown. Implementors
     * should send a stream error whose condition is system-shutdown before closing
     * the connection.
     */
    public void systemShutdown();

    /**
     * Returns true if the connection/session is closed.
     *
     * @return true if the connection is closed.
     */
    public boolean isClosed();

    /**
     * Registers a listener for close event notification. Registrations after
     * the ISession is closed will be immediately notified <em>before</em>
     * the registration call returns (within the context of the
     * registration call). An optional handback object can be associated with
     * the registration if the same listener is registered to listen for multiple
     * connection closures.
     *
     * @param listener the listener to register for events.
     * @param handbackMessage the object to send in the event notification.
     */
    public void registerCloseListener(IConnectionCloseListener listener, Object handbackMessage);

    /**
     * Removes a registered close event listener. Registered listeners must
     * be able to receive close events up until the time this method returns.
     * (i.e. it is possible to call unregister, receive a close event registration,
     * and then have the unregister call return.)
     *
     * @param listener the listener to deregister for close events.
     */
    public void removeCloseListener(IConnectionCloseListener listener);

    /**
     * Delivers the packet to this connection without checking the recipient.
     * The method essentially calls <code>socket.send(packet.getWriteBuffer())</code>.
     *
     * @param packet the packet to deliver.
     * @throws org.b5chat.crossfire.auth.UnauthorizedException if a permission error was detected.
     */
    public void deliver(Packet packet) throws UnauthorizedException;

    /**
     * Delivers raw text to this connection. This is a very low level way for sending
     * XML stanzas to the client. This method should not be used unless you have very
     * good reasons for not using {@link #deliver(org.xmpp.packet.Packet)}.<p>
     *
     * This method avoids having to get the writer of this connection and mess directly
     * with the writer. Therefore, this method ensures a correct delivery of the stanza
     * even if other threads were sending data concurrently.
     *
     * @param text the XML stanzas represented kept in a String.
     */
    public void deliverRawText(String text);

    /**
     * Returns the major version of XMPP being used by this connection
     * (major_version.minor_version. In most cases, the version should be
     * "1.0". However, older clients using the "Jabber" protocol do not set a
     * version. In that case, the version is "0.0".
     *
     * @return the major XMPP version being used by this connection.
     */
    public int getMajorXMPPVersion();

    /**
     * Returns the minor version of XMPP being used by this connection
     * (major_version.minor_version. In most cases, the version should be
     * "1.0". However, older clients using the "Jabber" protocol do not set a
     * version. In that case, the version is "0.0".
     *
     * @return the minor XMPP version being used by this connection.
     */
    public int getMinorXMPPVersion();

    /**
     * Sets the XMPP version information. In most cases, the version should be "1.0".
     * However, older clients using the "Jabber" protocol do not set a version. In that
     * case, the version is "0.0".
     *
     * @param majorVersion the major version.
     * @param minorVersion the minor version.
     */
    public void setXMPPVersion(int majorVersion, int minorVersion);

    /**
     * Returns the language code that should be used for this connection
     * (e.g. "en").
     *
     * @return the language code for the connection.
     */
    public String getLanguage();

    /**
     * Sets the language code that should be used for this connection (e.g. "en").
     *
     * @param language the language code.
     */
    public void setLanaguage(String language);

    /**
     * Returns the packet deliverer to use when delivering a packet over the socket fails. The
     * packet deliverer will retry to send the packet using some other connection, will store
     * the packet offline for later retrieval or will just drop it.
     *
     * @return the packet deliverer to use when delivering a packet over the socket fails.
     */
    IPacketDeliverer getPacketDeliverer();

    /**
     * Enumeration that specifies if clients should be authenticated (and how) while
     * negotiating TLS.
     */
    enum ClientAuth {

        /**
         * No authentication will be performed on the client. Client credentials will not
         * be verified while negotiating TLS.
         */
        disabled,

        /**
         * Clients will try to be authenticated. Unlike {@link #needed}, if the client
         * chooses not to provide authentication information about itself, the TLS negotiations
         * will stop and the connection will be dropped. This option is only useful for
         * engines in the server mode.
         */
        wanted,

        /**
         * Clients need to be authenticated. Unlike {@link #wanted}, if the client
         * chooses not to provide authentication information about itself, the TLS negotiations
         * will continue. This option is only useful for engines in the server mode.
         */
        needed
    }
}
