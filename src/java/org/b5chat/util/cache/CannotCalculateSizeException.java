/**
 * $RCSfile$
 * $Revision: 11291 $
 * $Date: 2009-09-30 10:17:14 +0000 (śro) $
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

package org.b5chat.util.cache;

/**
 * <p>Flags an exception when we cannot determine size of the object to be cached.</p>
 *
 * @author Marcin Cieslak
 */
public class CannotCalculateSizeException extends Exception {

    public static final long serialVersionUID = 1754096832201752388L;

    public CannotCalculateSizeException() {
    }

    public CannotCalculateSizeException(Object obj) {
        super("Unable to determine size of " + obj.getClass() + " instance");
    }
}
