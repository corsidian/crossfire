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

package org.b5chat.crossfire.core.net.sasl;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.security.Security;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;


import org.b5chat.crossfire.core.lockout.LockOutManager;
import org.b5chat.crossfire.core.net.XMPPCallbackHandler;
import org.b5chat.crossfire.core.property.Globals;
import org.b5chat.crossfire.core.util.StringUtils;
import org.b5chat.crossfire.xmpp.auth.AuthFactory;
import org.b5chat.crossfire.xmpp.auth.AuthToken;
import org.b5chat.crossfire.xmpp.server.XmppServer;
import org.b5chat.crossfire.xmpp.session.IClientSession;
import org.b5chat.crossfire.xmpp.session.ISession;
import org.b5chat.crossfire.xmpp.session.LocalClientSession;
import org.b5chat.crossfire.xmpp.session.LocalSession;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SASLAuthentication is responsible for returning the available SASL mechanisms to use and for
 * actually performing the SASL authentication.<p>
 *
 * The list of available SASL mechanisms is determined by:
 * <ol>
 *      <li>The type of {@link org.b5chat.crossfire.xmpp.user.IUserProvider} being used since
 *      some SASL mechanisms require the server to be able to retrieve user passwords</li>
 *      <li>Whether anonymous logins are enabled or not.</li>
 *      <li>Whether shared secret authentication is enabled or not.</li>
 *      <li>Whether the underlying connection has been secured or not.</li>
 * </ol>
 *
 * @author Hao Chen
 * @author Gaston Dombiak
 */
public class SASLAuthentication {

	private static final Logger Log = LoggerFactory.getLogger(SASLAuthentication.class);

    /**
     * The utf-8 charset for decoding and encoding Jabber packet streams.
     */
    protected static String CHARSET = "UTF-8";

    private static final String SASL_NAMESPACE = "xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\"";

    private static Map<String, ElementType> typeMap = new TreeMap<String, ElementType>();

    private static Set<String> mechanisms = null;

    static {
        initMechanisms();
    }

    public enum ElementType {

        AUTH("auth"), RESPONSE("response"), CHALLENGE("challenge"), FAILURE("failure"), UNDEF("");

        private String name = null;

        @Override
		public String toString() {
            return name;
        }

        private ElementType(String name) {
            this.name = name;
            typeMap.put(this.name, this);
        }

        public static ElementType valueof(String name) {
            if (name == null) {
                return UNDEF;
            }
            ElementType e = typeMap.get(name);
            return e != null ? e : UNDEF;
        }
    }

    public enum Status {
        /**
         * Entity needs to respond last challenge. ISession is still negotiating
         * SASL authentication.
         */
        needResponse,
        /**
         * SASL negotiation has failed. The entity may retry a few times before the connection
         * is closed.
         */
        failed,
        /**
         * SASL negotiation has been successful.
         */
        authenticated;
    }

    /**
     * Returns a string with the valid SASL mechanisms available for the specified session. If
     * the session's connection is not secured then only include the SASL mechanisms that don't
     * require TLS.
     *
     * @param session The current session
     *
     * @return a string with the valid SASL mechanisms available for the specified session.
     */
    public static String getSASLMechanisms(LocalSession session) {
        if (!(session instanceof IClientSession)) {
            return "";
        }
        StringBuilder sb = new StringBuilder(195);
        sb.append("<mechanisms xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
        for (String mech : getSupportedMechanisms()) {
            sb.append("<mechanism>");
            sb.append(mech);
            sb.append("</mechanism>");
        }
        sb.append("</mechanisms>");
        return sb.toString();
    }

    public static Element getSASLMechanismsElement(ISession session) {
        if (!(session instanceof IClientSession)) {
            return null;
        }

        Element mechs = DocumentHelper.createElement(new QName("mechanisms",
                new Namespace("", "urn:ietf:params:xml:ns:xmpp-sasl")));
        for (String mech : getSupportedMechanisms()) {
            Element mechanism = mechs.addElement("mechanism");
            mechanism.setText(mech);
        }
        return mechs;
    }

    /**
     * Handles the SASL authentication packet. The entity may be sending an initial
     * authentication request or a response to a challenge made by the server. The returned
     * value indicates whether the authentication has finished either successfully or not or
     * if the entity is expected to send a response to a challenge.
     *
     * @param session the session that is authenticating with the server.
     * @param doc the stanza sent by the authenticating entity.
     * @return value that indicates whether the authentication has finished either successfully
     *         or not or if the entity is expected to send a response to a challenge.
     * @throws UnsupportedEncodingException If UTF-8 charset is not supported.
     */
    public static Status handle(LocalSession session, Element doc) throws UnsupportedEncodingException {
        Status status;
        String mechanism;
        if (doc.getNamespace().asXML().equals(SASL_NAMESPACE)) {
            ElementType type = ElementType.valueof(doc.getName());
            switch (type) {
                case AUTH:
                    mechanism = doc.attributeValue("mechanism");
                    // Store the requested SASL mechanism by the client
                    session.setSessionData("SaslMechanism", mechanism);
                    //Log.debug("SASLAuthentication.doHandshake() AUTH entered: "+mechanism);
                    if (mechanism.equalsIgnoreCase("ANONYMOUS") &&
                            mechanisms.contains("ANONYMOUS")) {
                        status = doAnonymousAuthentication(session);
                    }
                    else if (mechanisms.contains(mechanism)) {
                        // The selected SASL mechanism requires the server to send a challenge
                        // to the client
                        try {
                            Map<String, String> props = new TreeMap<String, String>();
                            props.put(Sasl.QOP, "auth");
                            if (mechanism.equals("GSSAPI")) {
                                props.put(Sasl.SERVER_AUTH, "TRUE");
                            }
                            SaslServer ss = Sasl.createSaslServer(mechanism, "xmpp",
                                    Globals.getProperty("xmpp.fqdn", session.getServerName()), props,
                                    new XMPPCallbackHandler());
                            // evaluateResponse doesn't like null parameter
                            byte[] token = new byte[0];
                            if (doc.getText().length() > 0) {
                                // If auth request includes a value then validate it
                                token = StringUtils.decodeBase64(doc.getText().trim());
                                if (token == null) {
                                    token = new byte[0];
                                }
                            }
                            if (mechanism.equals("DIGEST-MD5")) {
                                // RFC2831 (DIGEST-MD5) says the client MAY provide an initial response on subsequent
                                // authentication. Java SASL does not (currently) support this and thows an exception
                                // if we try.  This violates the RFC, so we just strip any initial token.
                                token = new byte[0];
                            }
                            byte[] challenge = ss.evaluateResponse(token);
                            if (ss.isComplete()) {
                                authenticationSuccessful(session, ss.getAuthorizationID(),
                                    challenge);
                                status = Status.authenticated;
                            }
                            else {
                                // Send the challenge
                                sendChallenge(session, challenge);
                                status = Status.needResponse;
                            }
                            session.setSessionData("SaslServer", ss);
                        }
                        catch (SaslException e) {
                        	Log.info("User Login Failed. " + e.getMessage());
                            authenticationFailed(session);
                            status = Status.failed;
                        }
                    }
                    else {
                        Log.warn("Client wants to do a MECH we don't support: '" +
                                mechanism + "'");
                        authenticationFailed(session);
                        status = Status.failed;
                    }
                    break;
                case RESPONSE:
                    // Store the requested SASL mechanism by the client
                    mechanism = (String) session.getSessionData("SaslMechanism");
                    if (mechanism.equalsIgnoreCase("b5chat-SHAREDSECRET")) {
                        status = doSharedSecretAuthentication(session, doc);
                    }
                    else if (mechanisms.contains(mechanism)) {
                        SaslServer ss = (SaslServer) session.getSessionData("SaslServer");
                        if (ss != null) {
                            boolean ssComplete = ss.isComplete();
                            String response = doc.getTextTrim();
                            try {
                                if (ssComplete) {
                                    authenticationSuccessful(session, ss.getAuthorizationID(),
                                            null);
                                    status = Status.authenticated;
                                }
                                else {
                                    byte[] data = StringUtils.decodeBase64(response);
                                    if (data == null) {
                                        data = new byte[0];
                                    }
                                    byte[] challenge = ss.evaluateResponse(data);
                                    if (ss.isComplete()) {
                                        authenticationSuccessful(session, ss.getAuthorizationID(),
                                                challenge);
                                        status = Status.authenticated;
                                    }
                                    else {
                                        // Send the challenge
                                        sendChallenge(session, challenge);
                                        status = Status.needResponse;
                                    }
                                }
                            }
                            catch (SaslException e) {
                                Log.debug("SASLAuthentication: SaslException", e);
                                authenticationFailed(session);
                                status = Status.failed;
                            }
                        }
                        else {
                            Log.error("SaslServer is null, should be valid object instead.");
                            authenticationFailed(session);
                            status = Status.failed;
                        }
                    }
                    else {
                        Log.warn(
                                "Client responded to a MECH we don't support: '" + mechanism + "'");
                        authenticationFailed(session);
                        status = Status.failed;
                    }
                    break;
                default:
                    authenticationFailed(session);
                    status = Status.failed;
                    // Ignore
                    break;
            }
        }
        else {
            Log.debug("SASLAuthentication: Unknown namespace sent in auth element: " + doc.asXML());
            authenticationFailed(session);
            status = Status.failed;
        }
        // Check if SASL authentication has finished so we can clean up temp information
        if (status == Status.failed || status == Status.authenticated) {
            // Remove the SaslServer from the ISession
            session.removeSessionData("SaslServer");
            // Remove the requested SASL mechanism by the client
            session.removeSessionData("SaslMechanism");
        }
        return status;
    }

    /**
     * Returns true if shared secret authentication is enabled. Shared secret
     * authentication creates an anonymous session, but requires that the authenticating
     * entity know a shared secret key. The client sends a digest of the secret key,
     * which is compared against a digest of the local shared key.
     *
     * @return true if shared secret authentication is enabled.
     */
    public static boolean isSharedSecretAllowed() {
        return Globals.getBooleanProperty("xmpp.auth.sharedSecretEnabled");
    }

    /**
     * Sets whether shared secret authentication is enabled. Shared secret
     * authentication creates an anonymous session, but requires that the authenticating
     * entity know a shared secret key. The client sends a digest of the secret key,
     * which is compared against a digest of the local shared key.
     *
     * @param sharedSecretAllowed true if shared secret authentication should be enabled.
     */
    public static void setSharedSecretAllowed(boolean sharedSecretAllowed) {
        Globals.setProperty("xmpp.auth.sharedSecretEnabled", sharedSecretAllowed ? "true" : "false");
    }

    /**
     * Returns the shared secret value, or <tt>null</tt> if shared secret authentication is
     * disabled. If this is the first time the shared secret value has been requested (and
     * shared secret auth is enabled), the key will be randomly generated and stored in the
     * property <tt>xmpp.auth.sharedSecret</tt>.
     *
     * @return the shared secret value.
     */
    public static String getSharedSecret() {
        if (!isSharedSecretAllowed()) {
            return null;
        }
        String sharedSecret = Globals.getProperty("xmpp.auth.sharedSecret");
        if (sharedSecret == null) {
            sharedSecret = StringUtils.randomString(8);
            Globals.setProperty("xmpp.auth.sharedSecret", sharedSecret);
        }
        return sharedSecret;
    }

    /**
     * Returns true if the supplied digest matches the shared secret value. The digest
     * must be an MD5 hash of the secret key, encoded as hex. This value is supplied
     * by clients attempting shared secret authentication.
     *
     * @param digest the MD5 hash of the secret key, encoded as hex.
     * @return true if authentication succeeds.
     */
    public static boolean authenticateSharedSecret(String digest) {
        if (!isSharedSecretAllowed()) {
            return false;
        }
        String sharedSecert = getSharedSecret();
        return StringUtils.hash(sharedSecert).equals(digest);
    }


    private static Status doAnonymousAuthentication(LocalSession session) {
        if (XmppServer.getInstance().getIQAuthHandler().isAnonymousAllowed()) {
            // Verify that client can connect from his IP address
            boolean forbidAccess = false;
            try {
                String hostAddress = session.getConnection().getHostAddress();
                if (!LocalClientSession.getAllowedAnonymIPs().isEmpty() &&
                        !LocalClientSession.getAllowedAnonymIPs().containsKey(hostAddress)) {
                    byte[] address = session.getConnection().getAddress();
                    String range1 = (address[0] & 0xff) + "." + (address[1] & 0xff) + "." +
                            (address[2] & 0xff) +
                            ".*";
                    String range2 = (address[0] & 0xff) + "." + (address[1] & 0xff) + ".*.*";
                    String range3 = (address[0] & 0xff) + ".*.*.*";
                    if (!LocalClientSession.getAllowedAnonymIPs().containsKey(range1) &&
                            !LocalClientSession.getAllowedAnonymIPs().containsKey(range2) &&
                            !LocalClientSession.getAllowedAnonymIPs().containsKey(range3)) {
                        forbidAccess = true;
                    }
                }
            } catch (UnknownHostException e) {
                forbidAccess = true;
            }
            if (forbidAccess) {
                authenticationFailed(session);
                return Status.failed;
            }
            // Just accept the authentication :)
            authenticationSuccessful(session, null, null);
            return Status.authenticated;
        }
        else {
            // anonymous login is disabled so close the connection
            authenticationFailed(session);
            return Status.failed;
        }
    }

    private static Status doSharedSecretAuthentication(LocalSession session, Element doc)
            throws UnsupportedEncodingException
    {
        String secretDigest;
        String response = doc.getTextTrim();
        if (response == null || response.length() == 0) {
            // No info was provided so send a challenge to get it
            sendChallenge(session, new byte[0]);
            return Status.needResponse;
        }

        // Parse data and obtain username & password
        String data = new String(StringUtils.decodeBase64(response), CHARSET);
        StringTokenizer tokens = new StringTokenizer(data, "\0");
        tokens.nextToken();
        secretDigest = tokens.nextToken();
        if (authenticateSharedSecret(secretDigest)) {
            authenticationSuccessful(session, null, null);
            return Status.authenticated;
        }
        // Otherwise, authentication failed.
        authenticationFailed(session);
        return Status.failed;
    }

    private static void sendChallenge(ISession session, byte[] challenge) {
        StringBuilder reply = new StringBuilder(250);
        if (challenge == null) {
            challenge = new byte[0];
        }
        String challenge_b64 = StringUtils.encodeBase64(challenge).trim();
        if ("".equals(challenge_b64)) {
            challenge_b64 = "="; // Must be padded if null
        }
        reply.append(
                "<challenge xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
        reply.append(challenge_b64);
        reply.append("</challenge>");
        session.deliverRawText(reply.toString());
    }

    private static void authenticationSuccessful(LocalSession session, String username,
            byte[] successData) {
        if (username != null && LockOutManager.getInstance().isAccountDisabled(username)) {
            // Interception!  This person is locked out, fail instead!
            LockOutManager.getInstance().recordFailedLogin(username);
            authenticationFailed(session);
            return;
        }
        StringBuilder reply = new StringBuilder(80);
        reply.append("<success xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\"");
        if (successData != null) {
            String successData_b64 = StringUtils.encodeBase64(successData).trim();
            reply.append(">").append(successData_b64).append("</success>");
        }
        else {
            reply.append("/>");
        }
        session.deliverRawText(reply.toString());
        // We only support SASL for c2s
        if (session instanceof IClientSession) {
            ((LocalClientSession) session).setAuthToken(new AuthToken(username));
        }
    }

    private static void authenticationFailed(LocalSession session) {
        StringBuilder reply = new StringBuilder(80);
        reply.append("<failure xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
        reply.append("<not-authorized/></failure>");
        session.deliverRawText(reply.toString());
        // Give a number of retries before closing the connection
        Integer retries = (Integer) session.getSessionData("authRetries");
        if (retries == null) {
            retries = 1;
        }
        else {
            retries = retries + 1;
        }
        session.setSessionData("authRetries", retries);
        if (retries >= Globals.getIntProperty("xmpp.auth.retries", 3) ) {
            // Close the connection
            session.close();
        }
    }

    /**
     * Adds a new SASL mechanism to the list of supported SASL mechanisms by the server. The
     * new mechanism will be offered to clients and connection managers as stream features.<p>
     *
     * Note: this method simply registers the SASL mechanism to be advertised as a supported
     * mechanism by crossfire. Actual SASL handling is done by Java itself, so you must add
     * the provider to Java.
     *
     * @param mechanism the new SASL mechanism.
     */
    public static void addSupportedMechanism(String mechanism) {
        mechanisms.add(mechanism);
    }

    /**
     * Removes a SASL mechanism from the list of supported SASL mechanisms by the server.
     *
     * @param mechanism the SASL mechanism to remove.
     */
    public static void removeSupportedMechanism(String mechanism) {
        mechanisms.remove(mechanism);
    }

    /**
     * Returns the list of supported SASL mechanisms by the server. Note that Java may have
     * support for more mechanisms but some of them may not be returned since a special setup
     * is required that might be missing. Use {@link #addSupportedMechanism(String)} to add
     * new SASL mechanisms.
     *
     * @return the list of supported SASL mechanisms by the server.
     */
    public static Set<String> getSupportedMechanisms() {
        Set<String> answer = new HashSet<String>(mechanisms);
        // Clean up not-available mechanisms
        for (Iterator<String> it=answer.iterator(); it.hasNext();) {
            String mech = it.next();
            if (mech.equals("CRAM-MD5") || mech.equals("DIGEST-MD5")) {
                // Check if the user provider in use supports passwords retrieval. Accessing
                // to the users passwords will be required by the CallbackHandler
                if (!AuthFactory.getAuthProvider().supportsPasswordRetrieval()) {
                    it.remove();
                }
            }
            else if (mech.equals("ANONYMOUS")) {
                // Check anonymous is supported
                if (!XmppServer.getInstance().getIQAuthHandler().isAnonymousAllowed()) {
                    it.remove();
                }
            }
            else if (mech.equals("b5chat-SHAREDSECRET")) {
                // Check shared secret is supported
                if (!isSharedSecretAllowed()) {
                    it.remove();
                }
            }
        }
        return answer;
    }

    private static void initMechanisms() {
        // Convert XML based provider setup to Database based
        Globals.migrateProperty("sasl.mechs");
        Globals.migrateProperty("sasl.gssapi.debug");
        Globals.migrateProperty("sasl.gssapi.config");
        Globals.migrateProperty("sasl.gssapi.useSubjectCredsOnly");

        mechanisms = new HashSet<String>();
        String available = Globals.getProperty("sasl.mechs");
        if (available == null) {
            mechanisms.add("ANONYMOUS");
            mechanisms.add("PLAIN");
            mechanisms.add("DIGEST-MD5");
            mechanisms.add("CRAM-MD5");
            mechanisms.add("b5chat-SHAREDSECRET");
        }
        else {
            StringTokenizer st = new StringTokenizer(available, " ,\t\n\r\f");
            while (st.hasMoreTokens()) {
                String mech = st.nextToken().toUpperCase();
                // Check that the mech is a supported mechansim. Maybe we shouldnt check this and allow any?
                if (mech.equals("ANONYMOUS") ||
                        mech.equals("PLAIN") ||
                        mech.equals("DIGEST-MD5") ||
                        mech.equals("CRAM-MD5") ||
                        mech.equals("GSSAPI") ||
                        mech.equals("EXTERNAL") ||
                        mech.equals("b5chat-SHAREDSECRET")) 
                {
                    Log.debug("SASLAuthentication: Added " + mech + " to mech list");
                    mechanisms.add(mech);
                }
            }

            if (mechanisms.contains("GSSAPI")) {
                if (Globals.getProperty("sasl.gssapi.config") != null) {
                    System.setProperty("java.security.krb5.debug",
                            Globals.getProperty("sasl.gssapi.debug", "false"));
                    System.setProperty("java.security.auth.login.config",
                            Globals.getProperty("sasl.gssapi.config"));
                    System.setProperty("javax.security.auth.useSubjectCredsOnly",
                            Globals.getProperty("sasl.gssapi.useSubjectCredsOnly", "false"));
                }
                else {
                    //Not configured, remove the option.
                    Log.debug("SASLAuthentication: Removed GSSAPI from mech list");
                    mechanisms.remove("GSSAPI");
                }
            }
        }
        //Add our providers to the Security class
        Security.addProvider(new org.b5chat.crossfire.core.net.sasl.SASLProvider());
    }
}
