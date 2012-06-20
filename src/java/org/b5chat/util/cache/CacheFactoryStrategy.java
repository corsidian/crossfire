/**
 * $RCSfile$
 * $Revision: 3144 $
 * $Date: 2005-12-01 14:20:11 -0300 (Thu, 01 Dec 2005) $
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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * Implementation of CacheFactory that relies on the specific clustering solution.
 *
 * @author Gaston Dombiak
 */
public interface CacheFactoryStrategy {

    /**
     * Creates a new cache for the cache name specified. The created cache is
     * already configured. Different implementations could store the cache
     * configuration in different ways. It is recommended to store the cache
     * configuration in an external file so it is easier for customers to change
     * the default configuration.
     *
     * @param name name of the cache to create.
     * @return newly created and configured cache.
     */
    Cache<String,Object> createCache(String name);

    /**
     * Destroys the supplied cache.
     *
     * @param cache the cache to destroy.
     */
    void destroyCache(Cache<String,Object> cache);

    /**
     * Updates the statistics of the specified caches and publishes them into
     * a cache for statistics. The statistics cache is already known to the application
     * but this could change in the future (?). When not in cluster mode then
     * do nothing.<p>
     *
     * The statistics cache must contain a long array of 5 positions for each cache
     * with the following content:
     * <ol>
     *  <li>cache.getCacheSize()</li>
     *  <li>cache.getMaxCacheSize()</li>
     *  <li>cache.size()</li>
     *  <li>cache.getCacheHits()</li>
     *  <li>cache.getCacheMisses()</li>
     * </ol>
     *
     * @param caches caches to get their stats and publish them in a statistics cache.
     */
    void updateCacheStats(Map<String, Cache<String,Object>> caches);

    /**
     * Returns an existing lock on the specified key or creates a new one if none was found. This
     * operation is thread safe. The supplied cache may or may not be used depending whether
     * the server is running on cluster mode or not. When not running as part of a cluster then
     * the lock will be unrelated to the cache and will only be visible in this JVM.
     *
     * @param key the object that defines the visibility or scope of the lock.
     * @param cache the cache used for holding the lock.
     * @return an existing lock on the specified key or creates a new one if none was found.
     */
    Lock getLock(Object key, Cache<String,Object> cache);
}
