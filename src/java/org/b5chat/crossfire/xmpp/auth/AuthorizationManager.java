/**
 * $RCSfile$
 * $Revision: $
 * $Date: 2006-04-07 09:28:54 -0500 (Fri, 07 Apr 2006) $
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

package org.b5chat.crossfire.xmpp.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;


import org.b5chat.crossfire.core.property.Globals;
import org.b5chat.crossfire.core.util.ClassUtils;
import org.b5chat.crossfire.core.util.StringUtils;
import org.b5chat.crossfire.xmpp.user.UserAlreadyExistsException;
import org.b5chat.crossfire.xmpp.user.UserManager;
import org.b5chat.crossfire.xmpp.user.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the AuthorizationProvider objects.
 * <p/>
 * Overall description of the authentication and authorization process:
 * <p/>
 * After a client connects, and idicates a desire to use SASL, the
 * SASLAuthentication object decides which SASL mechanisms to advertise,
 * and then performs the authentication. If authentication is successful,
 * the XMPPCallbackHandler is asked to handle() an AuthorizeCallback.  The
 * XMPPCallbackHandler asks the AuthorizationManager to authorize the
 * principal to the requested username.  The AuthorizationManager manages
 * a list of AuthorizationProvider classes, and tries them one at a time
 * and returns true with the first AuthorizationProvider that authorizes
 * the principal to the username.  If no classes authorize the principal,
 * false is returned, which traces all the way back to give the client an
 * unauthorized message. Its important to note that the message the client
 * recieves will give no indication if the principal authentiated successfully,
 * you will need to check the server logs for that information.
 *
 * @author Jay Kline
 */
public class AuthorizationManager {

	private static final Logger Log = LoggerFactory.getLogger(AuthorizationManager.class);

    private static ArrayList<IAuthorizationPolicy> authorizationPolicies = new ArrayList<IAuthorizationPolicy>();
    private static ArrayList<IAuthorizationMapping> authorizationMapping = new ArrayList<IAuthorizationMapping>();
    private static AuthorizationManager instance = new AuthorizationManager();

    static {
        // Convert XML based provider setup to Database based
        Globals.migrateProperty("provider.authorization.classList");
        Globals.migrateProperty("provider.authorizationMapping.classList");
        Globals.migrateProperty("sasl.approvedRealms");
        Globals.migrateProperty("sasl.realm");

        String classList = Globals.getProperty("provider.authorization.classList");
        if (classList != null) {
            StringTokenizer st = new StringTokenizer(classList, " ,\t\n\r\f");
            while (st.hasMoreTokens()) {
                String s_provider = st.nextToken();
                try {
                    Class<?> c_provider = ClassUtils.forName(s_provider);
                    IAuthorizationPolicy provider =
                            (IAuthorizationPolicy)(c_provider.newInstance());
                    Log.debug("AuthorizationManager: Loaded " + s_provider);
                    authorizationPolicies.add(provider);
                }
                catch (Exception e) {
                    Log.error("AuthorizationManager: Error loading AuthorizationProvider: " + s_provider + "\n" + e);
                }
            }
        }
        if (authorizationPolicies.isEmpty()) {
            Log.debug("AuthorizationManager: No AuthorizationProvider's found. Loading DefaultAuthorizationPolicy");
            authorizationPolicies.add(new DefaultAuthorizationPolicy());
        }

        classList = Globals.getProperty("provider.authorizationMapping.classList");
        if (classList != null) {
            StringTokenizer st = new StringTokenizer(classList, " ,\t\n\r\f");
            while (st.hasMoreTokens()) {
                String s_provider = st.nextToken();
                try {
                    Class<?> c_provider = ClassUtils.forName(s_provider);
                    Object o_provider = c_provider.newInstance();
                    if(o_provider instanceof IAuthorizationMapping) {
                        IAuthorizationMapping provider = (IAuthorizationMapping)(o_provider);
                        Log.debug("AuthorizationManager: Loaded " + s_provider);
                        authorizationMapping.add(provider);
                    } else {
                        Log.debug("AuthorizationManager: Unknown class type.");
                    }
                } catch (Exception e) {
                    Log.error("AuthorizationManager: Error loading IAuthorizationMapping: " + s_provider + "\n" + e);
                }
            }
        }
        if (authorizationMapping.isEmpty()) {
            Log.debug("AuthorizationManager: No IAuthorizationMapping's found. Loading DefaultAuthorizationMapping");
            authorizationMapping.add(new DefaultAuthorizationMapping());
        }
    }

    private AuthorizationManager() {

    }

    /**
     * Returns the currently-installed AuthorizationProvider. Warning: You
     * should not be calling the AuthorizationProvider directly to perform
     * authorizations, it will not take into account the policy selected in
     * the <tt>crossfire.xml</tt>.  Use @see{authorize} in this class, instead.
     *
     * @return the current AuthorizationProvider.
     */
    public static Collection<IAuthorizationPolicy> getAuthorizationPolicies() {
        return authorizationPolicies;
    }

    /**
     * Returns a singleton AuthorizationManager instance.
     *
     * @return a AuthorizationManager instance.
     */
    public static AuthorizationManager getInstance() {
        return instance;
    }

    /**
     * Authorize the authenticated used to the requested username.  This uses the
     * selected the selected AuthenticationProviders.
     *
     * @param username The requested username.
     * @param principal The authenticated principal.
     * @return true if the user is authorized.
     */

    public static boolean authorize(String username, String principal) {
        for (IAuthorizationPolicy ap : authorizationPolicies) {
            if (Log.isDebugEnabled()) {
                Log.debug("AuthorizationManager: Trying "+ap.name()+".authorize("+username+" , "+principal+")");
            }

            if (ap.authorize(username, principal)) {
                // Authorized..  but do you exist?
                try {
                    UserManager.getUserProvider().loadUser(username);
                }
                catch (UserNotFoundException nfe) {
                    if (Log.isDebugEnabled()) {
                        Log.debug("AuthorizationManager: User " + username + " not found " + nfe.toString());
                    }
                    // Should we add the user?
                    if(Globals.getBooleanProperty("xmpp.auth.autoadd",false)) {
                        if (UserManager.getUserProvider().isReadOnly()) {
                            return false;
                        }
                        if (UserManager.getUserProvider().isNameRequired() || UserManager.getUserProvider().isEmailRequired()) {
                            // If these are required, there's no way we can arbitrarily auto-create this account.
                            return false;
                        }
                        try {
                            UserManager.getUserProvider().createUser(username, StringUtils.randomString(8), null, null);
                            if (Log.isDebugEnabled()) {
                                Log.info("AuthorizationManager: User "+username+" created.");
                            }
                            return true;
                        }
                        catch (UserAlreadyExistsException uaee) {
                            // Somehow the user got created in this very short timeframe.. 
                            // To be safe, lets fail here. The user can always try again.
                            if (Log.isDebugEnabled()) {
                                Log.error("AuthorizationManager: User " + username +
                                        " already exists while attempting to add user.");
                            }
                            return false;
                        }
                    }
                    return false;
                }
                // User exists
                return true;
            }
        }
        // Not authorized.
        return false;
    }

    /**
     * Map the authenticated principal to the default username.  If the authenticated 
     * principal did not supply a username, determine the default to use.
     *
     * @param principal The authentiated principal to determine the default username.
     * @return The default username for the authentiated principal.
     */

    public static String map(String principal) {
        for (IAuthorizationMapping am : authorizationMapping) {
            if (Log.isDebugEnabled()) {
                Log.debug("AuthorizationManager: Trying " + am.name() + ".map(" + principal + ")");
            }
            String username = am.map(principal);
            if( ! username.equals(principal) ) {
                return username;
            }
        }
        return principal;
    }
}
