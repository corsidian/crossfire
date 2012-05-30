/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
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

package net.emiva.crossfire.sasl;

import java.security.Provider;

/**
 * This is the Provider object providing the SaslServerFactory written by EMIVA Community. 
 *
 * @see SaslServerFactoryImpl
 */

public class SaslProvider extends Provider {

    /**
     * Constructs a the emiva SASL provider.
     */
    public SaslProvider() {
        super("emiva", 1.0, "emiva SASL provider v1.0, implementing server mechanisms for: PLAIN, CLEARSPACE");
        // Add SaslServer supporting the PLAIN SASL mechanism
        put("SaslServerFactory.PLAIN", "net.emiva.crossfire.sasl.SaslServerFactoryImpl");
        // Add SaslServer supporting the Clearspace SASL mechanism
        put("SaslServerFactory.CLEARSPACE", "net.emiva.crossfire.sasl.SaslServerFactoryImpl");
    }
}