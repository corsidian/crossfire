/**
 * Copyright (C) 2004-2009 EMIVA Community. All rights reserved.
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

package net.emiva.crossfire.user;

/**
 * A UserProvider to be used in conjunction with
 * {@link net.emiva.crossfire.auth.NativeAuthProvider NativeAuthProvider}, which
 * authenticates using OS-level authentication. New user accounts will automatically be
 * created as needed (upon successful initial authentication). To enable this provider,
 * edit the XML config file file and set:
 *
 * <pre>
 * &lt;provider&gt;
 *     &lt;auth&gt;
 *         &lt;className&gt;net.emiva.crossfire.auth.NativeAuthProvider&lt;/className&gt;
 *     &lt;/auth&gt;
 *     &lt;user&gt;
 *         &lt;className&gt;net.emiva.crossfire.user.NativeUserProvider&lt;/className&gt;
 *     &lt;/user&gt;
 * &lt;/provider&gt;
 * </pre>
 *
 * @see net.emiva.crossfire.auth.NativeAuthProvider NativeAuthProvider
 *
 * @author Matt Tucker
 */
public class NativeUserProvider extends DefaultUserProvider {

    
}
