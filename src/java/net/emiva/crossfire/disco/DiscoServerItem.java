/**
 * $RCSfile$
 * $Revision: 128 $
 * $Date: 2004-10-25 20:42:00 -0300 (Mon, 25 Oct 2004) $
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

package net.emiva.crossfire.disco;

import org.xmpp.packet.JID;

/**
 * Represent a DiscoItem provided by the server. Therefore, the DiscoServerItems are responsible
 * for providing the IDiscoInfoProvider and IDiscoItemsProvider that will provide the information and
 * items related to this item.<p>
 * <p/>
 * When the server starts up, IQDiscoItemsHandler will request to all the services that implement
 * the IServerItemsProvider interface for their DiscoServerItems. Each DiscoServerItem will provide
 * its IDiscoInfoProvider which will automatically be included in IQDiscoInfoHandler as the provider
 * for this item's JID. Moreover, each DiscoServerItem will also provide its IDiscoItemsProvider
 * which will automatically be included in IQDiscoItemsHandler. Special attention must be paid to
 * the JID since all the items with the same host will share the same IDiscoInfoProvider or
 * IDiscoItemsProvider.
 *
 * @author Gaston Dombiak
 */
public class DiscoServerItem extends DiscoItem {

	private final IDiscoInfoProvider infoProvider;
	private final IDiscoItemsProvider itemsProvider;
	
	public DiscoServerItem(JID jid, String name, String node, String action, IDiscoInfoProvider infoProvider, IDiscoItemsProvider itemsProvider) {
		super(jid, name, node, action);
		
		if (infoProvider == null)
		{
			throw new IllegalArgumentException("Argument 'infoProvider' cannot be null.");
		}
		
		if (itemsProvider == null)
		{
			throw new IllegalArgumentException("Argument 'itemsProvider' cannot be null.");
		}
		
		this.infoProvider = infoProvider;
		this.itemsProvider = itemsProvider;
	}
	
    /**
     * Returns the IDiscoInfoProvider responsible for providing the information related to this item.
     * The IDiscoInfoProvider will be automatically included in IQDiscoInfoHandler as the provider
     * for this item's JID.
     *
     * @return the IDiscoInfoProvider responsible for providing the information related to this item.
     */
    public IDiscoInfoProvider getDiscoInfoProvider()
    {
    	return infoProvider;
    }

    /**
     * Returns the IDiscoItemsProvider responsible for providing the items related to this item.
     * The IDiscoItemsProvider will be automatically included in IQDiscoItemsHandler as the provider
     * for this item's JID.
     *
     * @return the IDiscoItemsProvider responsible for providing the items related to this item.
     */
    public IDiscoItemsProvider getDiscoItemsProvider()
    {
    	return itemsProvider;
    }
}
