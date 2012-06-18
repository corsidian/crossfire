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

package org.b5chat.crossfire.disco;

import java.util.Iterator;

/**
 * IServerItemsProvider are responsible for providing the items associated with the SERVER. Example
 * of server items are: Public Chatrooms, PubSub service, etc.<p>
 * <p/>
 * When the server starts up, IQDiscoItemsHandler will request to all the services that implement
 * the IServerItemsProvider interface for their DiscoServerItems. Each DiscoServerItem will provide
 * its IDiscoInfoProvider which will automatically be included in IQDiscoInfoHandler as the provider
 * for this item's JID. Moreover, each DiscoServerItem will also provide its IDiscoItemsProvider
 * which will automatically be included in IQDiscoItemsHandler. Special attention must be paid to
 * the JID since all the items with the same host will share the same IDiscoInfoProvider or
 * IDiscoItemsProvider. Therefore, a service must implement this interface in order to get its
 * services published as items associatd with the server.
 *
 * @author Gaston Dombiak
 */
public interface IServerItemsProvider {

    /**
     * Returns an Iterator (of DiscoServerItem) with the items associated with the server or null
     * if none.
     *
     * @return an Iterator (of DiscoServerItem) with the items associated with the server or null
     *         if none.
     */
    public abstract Iterator<DiscoServerItem> getItems();
}
