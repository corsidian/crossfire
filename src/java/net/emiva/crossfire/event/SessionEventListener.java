/**
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

package net.emiva.crossfire.event;

import net.emiva.crossfire.session.Session;

/**
 * Interface to listen for session events. Use the
 * {@link SessionEventDispatcher#addListener(SessionEventListener)}
 * method to register for events.
 *
 * @author Matt Tucker
 */
public interface SessionEventListener {

    /**
     * Notification event indicating that a user has authenticated with the server. The
     * authenticated user is not an anonymous user.
     *
     * @param session the authenticated session of a non anonymous user.
     */
    public void sessionCreated(Session session);    

    /**
     * An authenticated session of a non anonymous user was destroyed.
     *
     * @param session the authenticated session of a non anonymous user.
     */
    public void sessionDestroyed(Session session);

    /**
     * Notification event indicating that an anonymous user has authenticated with the server.
     *
     * @param session the authenticated session of an anonymous user.
     */
    public void anonymousSessionCreated(Session session);

    /**
     * An authenticated session of an anonymous user was destroyed.
     *
     * @param session the authenticated session of an anonymous user.
     */
    public void anonymousSessionDestroyed(Session session);

    /**
     * A session has finished resource binding.
     *
     * @param session the session on which resource binding was performed.
     */
    public void resourceBound(Session session);
}