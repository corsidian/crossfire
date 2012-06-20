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
package org.b5chat.crossfire.core.util.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;


import org.b5chat.crossfire.core.property.Globals;
import org.b5chat.crossfire.core.server.InitializationException;
import org.b5chat.crossfire.core.util.GlobalConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates Cache objects. The returned caches will either be local or clustered
 * depending on the clustering enabled setting and a user's license.<p>
 * <p/>
 * When clustered caching is turned on, cache usage statistics for all caches
 * that have been created are periodically published to the clustered cache
 * named "opt-$cacheStats".
 *
 */
public class CacheFactory {

	private static final Logger Log = LoggerFactory.getLogger(CacheFactory.class);

    public static String LOCAL_CACHE_PROPERTY_NAME = "cache.clustering.local.class";

    /**
     * Storage for all caches that get created.
     */
    private static Map<String, Cache> caches = new ConcurrentHashMap<String, Cache>();

    private static String localCacheFactoryClass;
    private static CacheFactoryStrategy cacheFactoryStrategy;

    public static final int DEFAULT_MAX_CACHE_SIZE = 1024 * 256;
    public static final long DEFAULT_MAX_CACHE_LIFETIME = 6 * GlobalConstants.HOUR;

    /**
     * This map contains property names which were used to store cache configuration data
     * in local xml properties in previous versions.
     */
    private static final Map<String, String> cacheNames = new HashMap<String, String>();
    /**
     * Default properties to use for local caches. Default properties can be overridden
     * by setting the corresponding system properties.
     */
    private static final Map<String, Long> cacheProps = new HashMap<String, Long>();

    static {
        localCacheFactoryClass = Globals.getProperty(LOCAL_CACHE_PROPERTY_NAME,
                "org.b5chat.crossfire.core.util.cache.DefaultLocalCacheStrategy");
        
        cacheNames.put("Favicon Hits", "faviconHits");
        cacheNames.put("Favicon Misses", "faviconMisses");
        cacheNames.put("Group", "group");
        cacheNames.put("Group Metadata Cache", "groupMeta");
        cacheNames.put("Javascript Cache", "javascript");
        cacheNames.put("Last Activity Cache", "lastActivity");
        cacheNames.put("Multicast Service", "multicast");
        cacheNames.put("Offline Message Size", "offlinemessage");
        cacheNames.put("Offline Presence Cache", "offlinePresence");
        cacheNames.put("Privacy Lists", "listsCache");
        cacheNames.put("Remote Users Existence", "remoteUsersCache");
        cacheNames.put("Roster", "username2roster");
        cacheNames.put("User", "userCache");
        cacheNames.put("Locked Out Accounts", "lockOutCache");
        cacheNames.put("VCard", "vcardCache");
        cacheNames.put("File Transfer Cache", "fileTransfer");
        cacheNames.put("File Transfer", "transferProxy");
        cacheNames.put("POP3 Authentication", "pop3");
        cacheNames.put("LDAP Authentication", "ldap");
        cacheNames.put("Routing Servers Cache", "routeServer");
        cacheNames.put("Routing Components Cache", "routeComponent");
        cacheNames.put("Routing Users Cache", "routeUser");
        cacheNames.put("Routing AnonymousUsers Cache", "routeAnonymousUser");
        cacheNames.put("Routing User Sessions", "routeUserSessions");
        cacheNames.put("Components Sessions", "componentsSessions");
        cacheNames.put("IConnection Managers Sessions", "connManagerSessions");
        cacheNames.put("Incoming Server Sessions", "incServerSessions");
        cacheNames.put("Sessions by Hostname", "sessionsHostname");
        cacheNames.put("Secret Keys Cache", "secretKeys");
        cacheNames.put("Validated Domains", "validatedDomains");
        cacheNames.put("Directed Presences", "directedPresences");
        cacheNames.put("Disco Server Features", "serverFeatures");
        cacheNames.put("Disco Server Items", "serverItems");
        cacheNames.put("Remote Server Configurations", "serversConfigurations");
        cacheNames.put("Entity Capabilities", "entityCapabilities");
        cacheNames.put("Entity Capabilities Users", "entityCapabilitiesUsers");
        cacheNames.put("Clearspace SSO Nonce", "clearspaceSSONonce");
        cacheNames.put("PEPServiceManager", "pepServiceManager");

        cacheProps.put("cache.fileTransfer.size", 128 * 1024l);
        cacheProps.put("cache.fileTransfer.maxLifetime", 1000 * 60 * 10l);
        cacheProps.put("cache.multicast.size", 128 * 1024l);
        cacheProps.put("cache.multicast.maxLifetime", GlobalConstants.DAY);
        cacheProps.put("cache.offlinemessage.size", 100 * 1024l);
        cacheProps.put("cache.offlinemessage.maxLifetime", GlobalConstants.HOUR * 12);
        cacheProps.put("cache.pop3.size", 512 * 1024l);
        cacheProps.put("cache.pop3.maxLifetime", GlobalConstants.HOUR);
        cacheProps.put("cache.transferProxy.size", -1l);
        cacheProps.put("cache.transferProxy.maxLifetime", 1000 * 60 * 10l);
        cacheProps.put("cache.group.size", 1024 * 1024l);
        cacheProps.put("cache.group.maxLifetime", GlobalConstants.MINUTE * 15);
        cacheProps.put("cache.lockOutCache.size", 1024 * 1024l);
        cacheProps.put("cache.lockOutCache.maxLifetime", GlobalConstants.MINUTE * 15);
        cacheProps.put("cache.groupMeta.size", 512 * 1024l);
        cacheProps.put("cache.groupMeta.maxLifetime", GlobalConstants.MINUTE * 15);
        cacheProps.put("cache.javascript.size", 128 * 1024l);
        cacheProps.put("cache.javascript.maxLifetime", 3600 * 24 * 10l);
        cacheProps.put("cache.ldap.size", 512 * 1024l);
        cacheProps.put("cache.ldap.maxLifetime", GlobalConstants.HOUR * 2);
        cacheProps.put("cache.listsCache.size", 512 * 1024l);
        cacheProps.put("cache.offlinePresence.size", 512 * 1024l);
        cacheProps.put("cache.lastActivity.size", 128 * 1024l);
        cacheProps.put("cache.userCache.size", 512 * 1024l);
        cacheProps.put("cache.userCache.maxLifetime", GlobalConstants.MINUTE * 30);
        cacheProps.put("cache.remoteUsersCache.size", 512 * 1024l);
        cacheProps.put("cache.remoteUsersCache.maxLifetime", GlobalConstants.MINUTE * 30);
        cacheProps.put("cache.vcardCache.size", 512 * 1024l);
        cacheProps.put("cache.faviconHits.size", 128 * 1024l);
        cacheProps.put("cache.faviconMisses.size", 128 * 1024l);
        cacheProps.put("cache.routeServer.size", -1l);
        cacheProps.put("cache.routeServer.maxLifetime", -1l);
        cacheProps.put("cache.routeComponent.size", -1l);
        cacheProps.put("cache.routeComponent.maxLifetime", -1l);
        cacheProps.put("cache.routeUser.size", -1l);
        cacheProps.put("cache.routeUser.maxLifetime", -1l);
        cacheProps.put("cache.routeAnonymousUser.size", -1l);
        cacheProps.put("cache.routeAnonymousUser.maxLifetime", -1l);
        cacheProps.put("cache.routeUserSessions.size", -1l);
        cacheProps.put("cache.routeUserSessions.maxLifetime", -1l);
        cacheProps.put("cache.componentsSessions.size", -1l);
        cacheProps.put("cache.componentsSessions.maxLifetime", -1l);
        cacheProps.put("cache.connManagerSessions.size", -1l);
        cacheProps.put("cache.connManagerSessions.maxLifetime", -1l);
        cacheProps.put("cache.incServerSessions.size", -1l);
        cacheProps.put("cache.incServerSessions.maxLifetime", -1l);
        cacheProps.put("cache.sessionsHostname.size", -1l);
        cacheProps.put("cache.sessionsHostname.maxLifetime", -1l);
        cacheProps.put("cache.secretKeys.size", -1l);
        cacheProps.put("cache.secretKeys.maxLifetime", -1l);
        cacheProps.put("cache.validatedDomains.size", -1l);
        cacheProps.put("cache.validatedDomains.maxLifetime", -1l);
        cacheProps.put("cache.directedPresences.size", -1l);
        cacheProps.put("cache.directedPresences.maxLifetime", -1l);
        cacheProps.put("cache.serverFeatures.size", -1l);
        cacheProps.put("cache.serverFeatures.maxLifetime", -1l);
        cacheProps.put("cache.serverItems.size", -1l);
        cacheProps.put("cache.serverItems.maxLifetime", -1l);
        cacheProps.put("cache.serversConfigurations.size", 128 * 1024l);
        cacheProps.put("cache.serversConfigurations.maxLifetime", GlobalConstants.MINUTE * 30);
        cacheProps.put("cache.entityCapabilities.size", -1l);
        cacheProps.put("cache.entityCapabilities.maxLifetime", GlobalConstants.DAY * 2);
        cacheProps.put("cache.entityCapabilitiesUsers.size", -1l);
        cacheProps.put("cache.entityCapabilitiesUsers.maxLifetime", GlobalConstants.DAY * 2);
        cacheProps.put("cache.pluginCacheInfo.size", -1l);
        cacheProps.put("cache.pluginCacheInfo.maxLifetime", -1l);
        cacheProps.put("cache.clearspaceSSONonce.size", -1l);
        cacheProps.put("cache.clearspaceSSONonce.maxLifetime", GlobalConstants.MINUTE * 2);
        cacheProps.put("cache.pepServiceManager.size", 1024l * 1024 * 10);
        cacheProps.put("cache.pepServiceManager.maxLifetime", GlobalConstants.MINUTE * 30);
    }

    private CacheFactory() {
    }

    /**
     * If a local property is found for the supplied name which specifies a value for cache size, it is returned.
     * Otherwise, the defaultSize argument is returned.
     *
     * @param cacheName the name of the cache to look up a corresponding property for.
     * @return either the property value or the default value.
     */
    public static long getMaxCacheSize(String cacheName) {
        return getCacheProperty(cacheName, ".size", DEFAULT_MAX_CACHE_SIZE);
    }

    /**
     * Sets a local property which overrides the maximum cache size as configured in coherence-cache-config.xml for the
     * supplied cache name.
     * @param cacheName the name of the cache to store a value for.
     * @param size the maximum cache size.
     */
    public static void setMaxSizeProperty(String cacheName, long size) {
        cacheName = cacheName.replaceAll(" ", "");
        Globals.setProperty("cache." + cacheName + ".size", Long.toString(size));
    }

    public static boolean hasMaxSizeFromProperty(String cacheName) {
        return hasCacheProperty(cacheName, ".size");
    }

    /**
    * If a local property is found for the supplied name which specifies a value for cache entry lifetime, it
     * is returned. Otherwise, the defaultLifetime argument is returned.
     *
    * @param cacheName the name of the cache to look up a corresponding property for.
    * @return either the property value or the default value.
    */
    public static long getMaxCacheLifetime(String cacheName) {
        return getCacheProperty(cacheName, ".maxLifetime", DEFAULT_MAX_CACHE_LIFETIME);
    }

    /**
     * Sets a local property which overrides the maximum cache entry lifetime as configured in coherence-cache-config.xml
     * for the supplied cache name.
     * @param cacheName the name of the cache to store a value for.
     * @param lifetime the maximum cache entry lifetime.
     */
    public static void setMaxLifetimeProperty(String cacheName, long lifetime) {
        cacheName = cacheName.replaceAll(" ", "");
        Globals.setProperty(("cache." + cacheName + ".maxLifetime"), Long.toString(lifetime));
    }

    public static boolean hasMaxLifetimeFromProperty(String cacheName) {
        return hasCacheProperty(cacheName, ".maxLifetime");
    }

    public static void setCacheTypeProperty(String cacheName, String type) {
        cacheName = cacheName.replaceAll(" ", "");
        Globals.setProperty("cache." + cacheName + ".type", type);
    }

    public static String getCacheTypeProperty(String cacheName) {
        cacheName = cacheName.replaceAll(" ", "");
        return Globals.getProperty("cache." + cacheName + ".type");
    }

    public static void setMinCacheSize(String cacheName, long size) {
        cacheName = cacheName.replaceAll(" ", "");
        Globals.setProperty("cache." + cacheName + ".min", Long.toString(size));
    }

    public static long getMinCacheSize(String cacheName) {
        return getCacheProperty(cacheName, ".min", 0);
    }

    private static long getCacheProperty(String cacheName, String suffix, long defaultValue) {
        // First check if user is overwriting default value using a system property for the cache name
        String propName = "cache." + cacheName.replaceAll(" ", "") + suffix;
        String sizeProp = Globals.getProperty(propName);
        if (sizeProp == null && cacheNames.containsKey(cacheName)) {
            // No system property was found for the cache name so try now with short name
            propName = "cache." + cacheNames.get(cacheName) + suffix;
            sizeProp = Globals.getProperty(propName);
        }
        if (sizeProp != null) {
            try {
                return Long.parseLong(sizeProp);
            }
            catch (NumberFormatException nfe) {
                Log.warn("Unable to parse " + propName + " using default value.");
            }
        }
        // Check if there is a default size value for this cache
        Long defaultSize = cacheProps.get(propName);
        return defaultSize == null ? defaultValue : defaultSize;
    }

    private static boolean hasCacheProperty(String cacheName, String suffix) {
        // First check if user is overwriting default value using a system property for the cache name
        String propName = "cache." + cacheName.replaceAll(" ", "") + suffix;
        String sizeProp = Globals.getProperty(propName);
        if (sizeProp == null && cacheNames.containsKey(cacheName)) {
            // No system property was found for the cache name so try now with short name
            propName = "cache." + cacheNames.get(cacheName) + suffix;
            sizeProp = Globals.getProperty(propName);
        }
        if (sizeProp != null) {
            try {
                Long.parseLong(sizeProp);
                return true;
            }
            catch (NumberFormatException nfe) {
                Log.warn("Unable to parse " + propName + " using default value.");
            }
        }
        return false;
    }

    /**
     * Returns an array of all caches in the system.
     * @return an array of all caches in the system.
     */
    public static Cache[] getAllCaches() {
        List<Cache> values = new ArrayList<Cache>();
        for (Cache cache : caches.values()) {
            values.add(cache);
        }
        return values.toArray(new Cache[values.size()]);
    }

    /**
     * Returns the named cache, creating it as necessary.
     *
     * @param name         the name of the cache to create.
     * @return the named cache, creating it as necessary.
     */
    @SuppressWarnings("unchecked")
    public static synchronized <T extends Cache> T createCache(String name) {
        T cache = (T) caches.get(name);
        if (cache != null) {
            return cache;
        }

        cache = (T) cacheFactoryStrategy.createCache(name);

        return wrapCache(cache, name);
    }

    /**
     * Destroys the cache for the cache name specified.
     *
     * @param name the name of the cache to destroy.
     */
    public static void destroyCache(String name) {
        Cache cache = caches.remove(name);
        if (cache != null) {
            cacheFactoryStrategy.destroyCache(cache);
        }
    }

    /**
     * Returns an existing {@link java.util.concurrent.locks.Lock} on the specified key or creates a new one
     * if none was found. This operation is thread safe. Successive calls with the same key may or may not
     * return the same {@link java.util.concurrent.locks.Lock}. However, different threads asking for the
     * same Lock at the same time will get the same Lock object.<p>
     *
     * The supplied cache may or may not be used depending whether the server is running on cluster mode
     * or not. When not running as part of a cluster then the lock will be unrelated to the cache and will
     * only be visible in this JVM.
     *
     * @param key the object that defines the visibility or scope of the lock.
     * @param cache the cache used for holding the lock.
     * @return an existing lock on the specified key or creates a new one if none was found.
     */
    public static Lock getLock(Object key, Cache cache) {
        return cacheFactoryStrategy.getLock(key, cache);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Cache> T wrapCache(T cache, String name) {
        cache = (T) new CacheWrapper(cache);
        cache.setName(name);

        caches.put(name, cache);
        return cache;
    }

    public synchronized static void clearCaches() {
        for (String cacheName : caches.keySet()) {
            Cache cache = caches.get(cacheName);
            cache.clear();
        }
    }

    public static synchronized void initialize() throws InitializationException {
        try {
            cacheFactoryStrategy = (CacheFactoryStrategy) Class
                        .forName(localCacheFactoryClass).newInstance();
        }
        catch (InstantiationException e) {
             throw new InitializationException(e);
        }
        catch (IllegalAccessException e) {
             throw new InitializationException(e);
        }
        catch (ClassNotFoundException e) {
            throw new InitializationException(e);
        }
    }

}