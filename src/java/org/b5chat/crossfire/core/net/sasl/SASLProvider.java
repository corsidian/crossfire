/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
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

package org.b5chat.crossfire.core.net.sasl;

import java.security.Provider;

/**
 * This is the Provider object providing the SaslServerFactory written by B5Chat Community. 
 *
 * @see SASLServerFactoryImpl
 */

public class SASLProvider extends Provider {

    /**
     * Constructs a the b5chat SASL provider.
     */
    public SASLProvider() {
        super("b5chat", 1.0, "b5chat SASL provider v1.0, implementing server mechanisms for: PLAIN, CLEARSPACE");
        // Add SaslServer supporting the PLAIN SASL mechanism
        put("SaslServerFactory.PLAIN", "org.b5chat.crossfire.core.net.sasl.SASLServerFactoryImpl");
        // Add SaslServer supporting the Clearspace SASL mechanism
        put("SaslServerFactory.CLEARSPACE", "org.b5chat.crossfire.core.net.sasl.SASLServerFactoryImpl");
    }
}