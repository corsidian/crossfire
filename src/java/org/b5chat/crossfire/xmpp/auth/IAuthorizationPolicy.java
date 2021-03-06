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

/**
 * This is the interface the AuthorizationManager uses to
 * conduct authorizations.
 * <p/>
 * Users that wish to integrate with their own authorization
 * system must implement this interface, and are strongly
 * encouraged to extend either the AbstractAuthoriationPolicy
 * or the AbstractAuthorizationProvider classes which allow
 * the admin console manage the classes more effectively.
 * Register the class with crossfire in the <tt>crossfire.xml</tt>
 * file.  An entry in that file would look like the following:
 * <p/>
 * <pre>
 *   &lt;provider&gt;
 *     &lt;authorization&gt;
 *       &lt;classlist&gt;com.foo.auth.CustomPolicyProvider&lt;/classlist&gt;
 *     &lt;/authorization&gt;
 *   &lt;/provider&gt;</pre>
 *
 * @author Jay Kline
 */
public interface IAuthorizationPolicy {

    /**
     * Returns true if the principal is explicity authorized to the JID
     *
     * @param username  The username requested.
     * @param principal The principal requesting the username.
     * @return true is the user is authorized to be principal
     */
    public boolean authorize(String username, String principal);

    /**
     * Returns the short name of the Policy
     *
     * @return The short name of the Policy
     */
    public abstract String name();

    /**
     * Returns a description of the Policy
     *
     * @return The description of the Policy.
     */
    public abstract String description();
}