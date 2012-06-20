/**
 * $RCSfile$
 * $Revision: 1526 $
 * $Date: 2005-06-16 02:50:35 -0300 (Thu, 16 Jun 2005) $
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

package org.b5chat.crossfire.xmpp.user;


import java.util.Map;

/**
 * Interface to listen for group events. Use the
 * {@link UserEventDispatcher#addListener(IUserEventListener)}
 * method to register for events.
 *
 * @author Matt Tucker
 */
public interface IUserEventListener {

    /**
     * A user was created.
     *
     * @param user the user.
     * @param params event parameters.
     */
    public void userCreated(User user, Map<String,Object> params);

    /**
     * A user is being deleted.
     *
     * @param user the user.
     * @param params event parameters.
     */
    public void userDeleting(User user, Map<String,Object> params);

    /**
     * A user's name, email, or an extended property was changed.
     *
     * @param user the user.
     * @param params event parameters.
     */
    public void userModified(User user, Map<String,Object> params);
}