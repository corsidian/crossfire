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

package org.b5chat.crossfire.user;

import org.xmpp.packet.JID;

/**
 * Interface to be implemented by components that are capable of returning the name of entities
 * when running as internal components.
 *
 * @author Gaston Dombiak
 */
public interface IUserNameProvider {

    /**
     * Returns the name of the entity specified by the following JID.
     *
     * @param entity JID of the entity to return its name.
     * @return the name of the entity specified by the following JID.
     */
    abstract String getUserName(JID entity);
}
