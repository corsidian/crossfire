/**
 * $RCSfile$
 * $Revision: 128 $
 * $Date: 2004-10-25 20:42:00 -0300 (Mon, 25 Oct 2004) $
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

package org.b5chat.crossfire.xmpp;

/**
 * Represents a runtime packet exception typically from a malformed
 * packet. Uncaught Packet exceptions will cause the originating session
 * to close.
 *
 * @author Iain Shigeoka
 */
@SuppressWarnings("serial")
public class PacketException extends RuntimeException {

    public PacketException() {
    }

    public PacketException(String s) {
        super(s);
    }

}
