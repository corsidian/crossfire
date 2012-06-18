/**
 * $RCSfile: $
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

import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;


import org.b5chat.crossfire.IConnection;
import org.b5chat.crossfire.IConnectionCloseListener;
import org.b5chat.crossfire.route.IPacketDeliverer;
import org.b5chat.crossfire.session.ISession;
import org.b5chat.crossfire.session.LocalSession;
import org.b5chat.util.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of the IConnection interface that models abstract connections. Abstract
 * connections are connections that don't have a physical connection counterpart. Instead they
 * can be seen as conceptual or just 'abstract' connections.<p>
 *
 * Default values and common behavior of virtual connections are modeled in this class. Subclasses
 * should just need to specify how packets are delivered and what means closing the connection.
 *
 * @author Gaston Dombiak
 */
public abstract class VirtualConnection implements IConnection {

	private static final Logger Log = LoggerFactory.getLogger(VirtualConnection.class);

    protected LocalSession session;

    final private Map<IConnectionCloseListener, Object> listeners =
            new HashMap<IConnectionCloseListener, Object>();

    private boolean closed = false;

    public String getLanguage() {
        // Information not available. Return any value. This is not actually used.
        return null;
    }

    public int getMajorXMPPVersion() {
        // Information not available. Return any value. This is not actually used.
        return 0;
    }

    public int getMinorXMPPVersion() {
        // Information not available. Return any value. This is not actually used.
        return 0;
    }

    public Certificate[] getLocalCertificates() {
        // Ignore
        return new Certificate[0];
    }

    public Certificate[] getPeerCertificates() {
        // Ignore
        return new Certificate[0];
    }

    public void setUsingSelfSignedCertificate(boolean isSelfSigned) {
    }

    public boolean isUsingSelfSignedCertificate() {
        return false;
    }

    public boolean isClosed() {
        if (session == null) {
            return closed;
        }
        return session.getStatus() == ISession.STATUS_CLOSED;
    }

    public void setXMPPVersion(int majorVersion, int minorVersion) {
        //Ignore
    }

    public void setLanaguage(String language) {
        //Ignore
    }

    public IPacketDeliverer getPacketDeliverer() {
        //Ignore
        return null;
    }

    public boolean validate() {
        // Return true since the virtual connection is valid until it no longer exists
        return true;
    }

    public void init(LocalSession session) {
        this.session = session;
    }

    /**
     * Closes the session, the virtual connection and notifies listeners that the connection
     * has been closed.
     */
    public void close() {
        boolean wasClosed = false;
        synchronized (this) {
            if (!isClosed()) {
                try {
                    if (session != null) {
                        session.setStatus(ISession.STATUS_CLOSED);
                    }
                    closeVirtualConnection();
                    closed = true;
                }
                catch (Exception e) {
                    Log.error(LocaleUtils.getLocalizedString("admin.error.close")
                            + "\n" + this.toString(), e);
                }
                wasClosed = true;
            }
        }
        if (wasClosed) {
            notifyCloseListeners();
        }
    }

    public void registerCloseListener(IConnectionCloseListener listener, Object handbackMessage) {
        if (isClosed()) {
            listener.onConnectionClose(handbackMessage);
        }
        else {
            listeners.put(listener, handbackMessage);
        }
    }

    public void removeCloseListener(IConnectionCloseListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies all close listeners that the connection has been closed.
     */
    private void notifyCloseListeners() {
        synchronized (listeners) {
            for (IConnectionCloseListener listener : listeners.keySet()) {
                try {
                    listener.onConnectionClose(listeners.get(listener));
                }
                catch (Exception e) {
                    Log.error("Error notifying listener: " + listener, e);
                }
            }
        }
    }

    /**
     * Closes the virtual connection. Subsclasses should indicate what closing a virtual
     * connection means. At this point the session has a CLOSED state.
     */
    public abstract void closeVirtualConnection();
}
