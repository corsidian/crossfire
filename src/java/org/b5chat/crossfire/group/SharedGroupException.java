/**
 * $RCSfile$
 * $Revision: 771 $
 * $Date: 2005-01-02 15:11:44 -0300 (Sun, 02 Jan 2005) $
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

package org.b5chat.crossfire.group;

/**
 * Thrown when a a user is trying to add or remove a contact from his/her roster that belongs to a
 * shared group.
 *
 * @author Gaston Dombiak
 */
@SuppressWarnings("serial")
public class SharedGroupException extends Exception {

    public SharedGroupException() {
        super();
    }

    public SharedGroupException(String msg) {
        super(msg);
    }
}
