/**
 * $RCSfile$
 * $Revision: 38 $
 * $Date: 2004-10-21 03:30:10 -0300 (Thu, 21 Oct 2004) $
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

package net.emiva.crossfire.audit;

import net.emiva.crossfire.BasicStreamIDFactory;
import net.emiva.crossfire.IStreamId;
import net.emiva.crossfire.IStreamIdFactory;

/**
 * Factory for producing audit stream IDs. We use a factory so that
 * audit information can be identified using an appropriate storage
 * key (typically a long for RDBMS).
 *
 * @author Iain Shigeoka
 */
public class AuditStreamIDFactory implements IStreamIdFactory {

    private BasicStreamIDFactory factory = new BasicStreamIDFactory();

    public AuditStreamIDFactory() {
    }

    public IStreamId createStreamID() {
        return factory.createStreamID();
    }
}
