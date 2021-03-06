/**
 * $RCSfile: $
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

package org.b5chat.crossfire.xmpp.entitycaps;


import java.util.HashSet;
import java.util.Set;

import org.b5chat.crossfire.core.util.cache.CacheSizes;
import org.b5chat.crossfire.core.util.cache.Cacheable;
import org.b5chat.crossfire.core.util.cache.CannotCalculateSizeException;

/**
 * Contains identities and supported features describing client capabilities
 * for an entity.
 * 
 * @author Armando Jagucki
 *
 */
// TODO: Instances of this class should not be cached in distributed caches. The overhead of distributing data is a lot higher than recalculating the hash on every cluster node. We should remove the Externalizable interface, and turn this class into an immutable class.
@SuppressWarnings("serial")
public class EntityCapabilities implements Cacheable {

    /**
     * Identities included in these entity capabilities.
     */
    private Set<String> identities = new HashSet<String>();

    /**
     * Features included in these entity capabilities.
     */
    private Set<String> features = new HashSet<String>();

    /**
     * Hash string that corresponds to the entity capabilities. To be
     * regenerated and used for discovering potential poisoning of entity
     * capabilities information.
     */
    private String verAttribute;

    /**
     * The hash algorithm that was used to create the hash string.
     */
    private String hashAttribute;
    
    /**
     * Adds an identity to the entity capabilities.
     * 
     * @param identity the identity
     * @return true if the entity capabilities did not already include the
     *         identity
     */
    boolean addIdentity(String identity) {
        return identities.add(identity);
    }

    /**
     * Adds a feature to the entity capabilities.
     * 
     * @param feature the feature
     * @return true if the entity capabilities did not already include the
     *         feature
     */
    boolean addFeature(String feature) {
        return features.add(feature);
    }

    /**
     * Determines whether or not a given identity is included in these entity
     * capabilities.
     * 
     * @param category the category of the identity
     * @param type the type of the identity
     * @return true if identity is included, false if not
     */
    public boolean containsIdentity(String category, String type) {
        return identities.contains(category + "/" + type);
    }

    /**
     * Determines whether or not a given feature is included in these entity
     * capabilities.
     * 
     * @param feature the feature
     * @return true if feature is included, false if not
     */
    public boolean containsFeature(String feature) {
        return features.contains(feature);
    }

    void setVerAttribute(String verAttribute) {
        this.verAttribute = verAttribute;
    }
    
    String getVerAttribute() {
    	return this.verAttribute;
    }

    void setHashAttribute(String hashAttribute) {
    	this.hashAttribute = hashAttribute;
    }

    String getHashAttribute() {
    	return this.hashAttribute;
    }
    
    public int getCachedSize() throws CannotCalculateSizeException {
        int size = CacheSizes.sizeOfCollection(identities);
        size += CacheSizes.sizeOfCollection(features);
        size += CacheSizes.sizeOfString(verAttribute);
        return size;
    }
}
