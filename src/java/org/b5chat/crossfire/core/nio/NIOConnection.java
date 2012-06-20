/**
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

package org.b5chat.crossfire.core.nio;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.security.cert.Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;


import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.SSLFilter;
import org.b5chat.crossfire.core.property.Globals;
import org.b5chat.crossfire.core.util.XMLWriter;
import org.b5chat.crossfire.xmpp.IConnection;
import org.b5chat.crossfire.xmpp.IConnectionCloseListener;
import org.b5chat.crossfire.xmpp.auth.UnauthorizedException;
import org.b5chat.crossfire.xmpp.route.IPacketDeliverer;
import org.b5chat.crossfire.xmpp.session.ISession;
import org.b5chat.crossfire.xmpp.session.LocalSession;
import org.dom4j.io.OutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.Packet;

/**
 * Implementation of {@link IConnection} inteface specific for NIO connections when using
 * the MINA framework.<p>
 *
 * MINA project can be found at <a href="http://mina.apache.org">here</a>.
 *
 * @author Gaston Dombiak
 */
public class NIOConnection implements IConnection {

	private static final Logger Log = LoggerFactory.getLogger(NIOConnection.class);

    /**
     * The utf-8 charset for decoding and encoding XMPP packet streams.
     */
    public static final String CHARSET = "UTF-8";

    private LocalSession session;
    private IoSession ioSession;

    private IConnectionCloseListener closeListener;

    /**
     * Deliverer to use when the connection is closed or was closed when delivering
     * a packet.
     */
    private IPacketDeliverer backupDeliverer;
    private int majorVersion = 1;
    private int minorVersion = 0;
    private String language = null;

    private static ThreadLocal<CharsetEncoder> encoder = new ThreadLocalEncoder();
    /**
     * Flag that specifies if the connection should be considered closed. Closing a NIO connection
     * is an asynch operation so instead of waiting for the connection to be actually closed just
     * keep this flag to avoid using the connection between #close was used and the socket is actually
     * closed.
     */
    private boolean closed;


    public NIOConnection(IoSession session, IPacketDeliverer packetDeliverer) {
        this.ioSession = session;
        this.backupDeliverer = packetDeliverer;
        closed = false;
    }

    public boolean validate() {
        if (isClosed()) {
            return false;
        }
        deliverRawText(" ");
        return !isClosed();
    }

    public void registerCloseListener(IConnectionCloseListener listener, Object ignore) {
        if (closeListener != null) {
            throw new IllegalStateException("Close listener already configured");
        }
        if (isClosed()) {
            listener.onConnectionClose(session);
        }
        else {
            closeListener = listener;
        }
    }

    public void removeCloseListener(IConnectionCloseListener listener) {
        if (closeListener == listener) {
            closeListener = null;
        }
    }

    public byte[] getAddress() throws UnknownHostException {
        return ((InetSocketAddress) ioSession.getRemoteAddress()).getAddress().getAddress();
    }

    public String getHostAddress() throws UnknownHostException {
        return ((InetSocketAddress) ioSession.getRemoteAddress()).getAddress().getHostAddress();
    }

    public String getHostName() throws UnknownHostException {
        return ((InetSocketAddress) ioSession.getRemoteAddress()).getAddress().getHostName();
    }

    public Certificate[] getLocalCertificates() {
        SSLSession sslSession = (SSLSession) ioSession.getAttribute(SSLFilter.SSL_SESSION);
        if (sslSession != null) {
            return sslSession.getLocalCertificates();
        }
        return new Certificate[0];
    }

    public Certificate[] getPeerCertificates() {
        try {
            SSLSession sslSession = (SSLSession) ioSession.getAttribute(SSLFilter.SSL_SESSION);
            if (sslSession != null) {
                return sslSession.getPeerCertificates();
            }
        } catch (SSLPeerUnverifiedException e) {
            Log.warn("Error retrieving client certificates of: " + session, e);
        }
        return new Certificate[0];
    }
    
    public IPacketDeliverer getPacketDeliverer() {
        return backupDeliverer;
    }

    public void close() {
        boolean closedSuccessfully = false;
        synchronized (this) {
            if (!isClosed()) {
                try {
                    deliverRawText("</stream:stream>", false);
                } catch (Exception e) {
                    // Ignore
                }
                if (session != null) {
                    session.setStatus(ISession.STATUS_CLOSED);
                }
                ioSession.close();
                closed = true;
                closedSuccessfully = true;
            }
        }
        if (closedSuccessfully) {
            notifyCloseListeners();
        }
    }

    public void systemShutdown() {
        deliverRawText("<stream:error><system-shutdown " +
                "xmlns='urn:ietf:params:xml:ns:xmpp-streams'/></stream:error>");
        close();
    }

    /**
     * Notifies all close listeners that the connection has been closed.
     * Used by subclasses to properly finish closing the connection.
     */
    private void notifyCloseListeners() {
        if (closeListener != null) {
            try {
                closeListener.onConnectionClose(session);
            } catch (Exception e) {
                Log.error("Error notifying listener: " + closeListener, e);
            }
        }
    }

    public void init(LocalSession owner) {
        session = owner;
    }

    public boolean isClosed() {
        if (session == null) {
            return closed;
        }
        return session.getStatus() == ISession.STATUS_CLOSED;
    }

    public boolean isSecure() {
        return ioSession.getFilterChain().contains("tls");
    }

    public void deliver(Packet packet) throws UnauthorizedException {
        if (isClosed()) {
            backupDeliverer.deliver(packet);
        }
        else {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            buffer.setAutoExpand(true);

            boolean errorDelivering = false;
            try {
                XMLWriter xmlSerializer =
                        new XMLWriter(new ByteBufferWriter(buffer, encoder.get()), new OutputFormat());
                xmlSerializer.write(packet.getElement());
                xmlSerializer.flush();
                buffer.flip();
                ioSession.write(buffer);
            }
            catch (Exception e) {
                Log.debug("NIOConnection: Error delivering packet" + "\n" + this.toString(), e);
                errorDelivering = true;
            }
            if (errorDelivering) {
                close();
                // Retry sending the packet again. Most probably if the packet is a
                // Message it will be stored offline
                backupDeliverer.deliver(packet);
            }
            else {
                session.incrementServerPacketCount();
            }
        }
    }

    public void deliverRawText(String text) {
        // Deliver the packet in asynchronous mode
        deliverRawText(text, true);
    }

    private void deliverRawText(String text, boolean asynchronous) {
        if (!isClosed()) {
            ByteBuffer buffer = ByteBuffer.allocate(text.length());
            buffer.setAutoExpand(true);

            boolean errorDelivering = false;
            try {
                //Charset charset = Charset.forName(CHARSET);
                //buffer.putString(text, charset.newEncoder());
                buffer.put(text.getBytes(CHARSET));
                buffer.flip();
                if (asynchronous) {
                    ioSession.write(buffer);
                }
                else {
                    // Send stanza and wait for ACK (using a 2 seconds default timeout)
                    boolean ok =
                            ioSession.write(buffer).join(Globals.getIntProperty("connection.ack.timeout", 2000));
                    if (!ok) {
                        Log.warn("No ACK was received when sending stanza to: " + this.toString());
                    }
                }
            }
            catch (Exception e) {
                Log.debug("NIOConnection: Error delivering raw text" + "\n" + this.toString(), e);
                errorDelivering = true;
            }
            // Close the connection if delivering text fails and we are already not closing the connection
            if (errorDelivering && asynchronous) {
                close();
            }
        }
    }


    public int getMajorXMPPVersion() {
        return majorVersion;
    }

    public int getMinorXMPPVersion() {
        return minorVersion;
    }

    public void setXMPPVersion(int majorVersion, int minorVersion) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanaguage(String language) {
        this.language = language;
    }

    @Override
	public String toString() {
        return super.toString() + " MINA ISession: " + ioSession;
    }

    private static class ThreadLocalEncoder extends ThreadLocal<CharsetEncoder> {

        @Override
		protected CharsetEncoder initialValue() {
            return Charset.forName(CHARSET).newEncoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);
        }
    }
}
