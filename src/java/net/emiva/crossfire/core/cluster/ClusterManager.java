/**
 * $RCSfile: $
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2005-2008 EMIVA Community. All rights reserved.
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

package net.emiva.crossfire.core.cluster;

import java.util.Collection;

import net.emiva.crossfire.core.property.GlobalProperties;
import net.emiva.crossfire.server.XmppServer;
import net.emiva.util.Globals;
import net.emiva.util.cache.CacheFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A cluster manager is responsible for triggering events related to clustering.
 * A future version will also provide statistics about the cluster.
 *
 * @author Gaston Dombiak
 */
public class ClusterManager {
	
	private static final Logger Log = LoggerFactory.getLogger(ClusterManager.class);
	private static ClusterEventDispatcher dispatcher;
	
    public static String CLUSTER_PROPERTY_NAME = "clustering.enabled";
    
    static {
        dispatcher = new ClusterEventDispatcher();
        dispatcher.setDaemon(true);
        dispatcher.start();
    }

    public static void addListener(IClusterEventListener listener) {
    	dispatcher.addListener(listener);
    }

    public static void removeListener(IClusterEventListener listener) {
    	dispatcher.removeListener(listener);
    }
    
    /**
     * Starts the cluster service if clustering is enabled. The process of starting clustering
     * will recreate caches as distributed caches.<p>
     *
     * Before starting a cluster the
     * {@link XmppServer#setRemoteSessionLocator(net.emiva.crossfire.session.IRemoteSessionLocator)} and
     * {@link net.emiva.crossfire.route.IRoutingTable#setRemotePacketRouter(net.emiva.crossfire.RemotePacketRouter)}
     * need to be properly configured.
     */
    public static synchronized void startup() {
        if (isClusteringStarted()) {
            return;
        }
        // See if clustering should be enabled.
        if (isClusteringEnabled()) {
            if (XmppServer.getInstance().getRemoteSessionLocator() == null) {
                throw new IllegalStateException("No IRemoteSessionLocator was found.");
            }
            if (XmppServer.getInstance().getRoutingTable().getRemotePacketRouter() == null) {
                throw new IllegalStateException("No IRemotePacketRouter was found.");
            }
            // Start up the cluster and reset caches
            CacheFactory.startClustering();
        }
    }

    /**
     * Shuts down the clustering service. This method should be called when the emiva
     * system is shutting down, and must not be called otherwise. Failing to call
     * this method may temporarily impact cluster performance, as the system will
     * have to do extra work to recover from a non-clean shutdown.
     * If clustering is not enabled, this method will do nothing.
     */
    public static synchronized void shutdown() {
        // Reset packet router to use to deliver packets to remote cluster nodes
        XmppServer.getInstance().getRoutingTable().setRemotePacketRouter(null);
        if (isClusteringStarted()) {
            Log.debug("ClusterManager: Shutting down clustered cache service.");
            CacheFactory.stopClustering();
        }
        // Reset the session locator to use
        XmppServer.getInstance().setRemoteSessionLocator(null);
    }

    /**
     * Sets true if clustering support is enabled. An attempt to start or join
     * an existing cluster will be attempted in the service was enabled. On the
     * other hand, if disabled then this JVM will leave or stop the cluster.
     *
     * @param enabled if clustering support is enabled.
     */
    public static void setClusteringEnabled(boolean enabled) {
        if (enabled) {
            // Check that clustering is not already enabled and we are already in a cluster
            if (isClusteringEnabled() && isClusteringStarted()) {
                return;
            }
        }
        else {
            // Check that clustering is disabled
            if (!isClusteringEnabled()) {
                return;
            }
        }
        Globals.setXMLProperty(CLUSTER_PROPERTY_NAME, Boolean.toString(enabled));
        if (!enabled) {
            shutdown();
        }
        else {
            // Reload emiva properties. This will ensure that this nodes copy of the
            // properties starts correct.
           GlobalProperties.getInstance().init();
           startup();
        }
    }

    /**
     * Returns true if clustering support is enabled. This does not mean
     * that clustering has started or that clustering will be able to start.
     *
     * @return true if clustering support is enabled.
     */
    public static boolean isClusteringEnabled() {
        return Globals.getXMLProperty(CLUSTER_PROPERTY_NAME, false);
    }

    /**
     * Returns true if clustering is installed and can be used by this JVM
     * to join a cluster. A false value could mean that either clustering
     * support is not available or the license does not allow to have more
     * than 1 cluster node.
     *
     * @return true if clustering is installed and can be used by 
     * this JVM to join a cluster.
     */
    public static boolean isClusteringAvailable() {
        return CacheFactory.isClusteringAvailable();
    }

    /**
     * Returns true is clustering is currently being started. Once the cluster
     * is started or failed to be started this value will be false.
     *
     * @return true is clustering is currently being started.
     */
    public static boolean isClusteringStarting() {
        return CacheFactory.isClusteringStarting();
    }

    /**
     * Returns true if this JVM is part of a cluster. The cluster may have many nodes
     * or this JVM could be the only node.
     *
     * @return true if this JVM is part of a cluster.
     */
    public static boolean isClusteringStarted() {
        return CacheFactory.isClusteringStarted();
    }

    /**
     * Returns true if this member is the senior member in the cluster. If clustering
     * is not enabled, this method will also return true. This test is useful for
     * tasks that should only be run on a single member in a cluster.
     *
     * @return true if this cluster member is the senior or if clustering is not enabled.
     */
    public static boolean isSeniorClusterMember() {
        return CacheFactory.isSeniorClusterMember();
    }

    /**
     * Returns basic information about the current members of the cluster or an empty
     * collection if not running in a cluster.
     *
     * @return information about the current members of the cluster or an empty
     *         collection if not running in a cluster.
     */
    public static Collection<IClusterNodeInfo> getNodesInfo() {
        return CacheFactory.getClusterNodesInfo();
    }

    /**
     * Returns the maximum number of cluster members allowed. Both values 0 and 1 mean that clustering
     * is not available. However, a value of 1 means that it's a license problem rather than not having
     * the ability to do clustering as defined with value 0.
     *
     * @return the maximum number of cluster members allowed or 0 or 1 if clustering is not allowed.
     */
    public static int getMaxClusterNodes() {
        return CacheFactory.getMaxClusterNodes();
    }

    /**
     * Returns the id of the node that is the senior cluster member. When not in a cluster the returned
     * node id will be the {@link XmppServer#getNodeID()}.
     *
     * @return the id of the node that is the senior cluster member.
     */
    public static NodeID getSeniorClusterMember() {
        byte[] clusterMemberID = CacheFactory.getSeniorClusterMemberID();
        if (clusterMemberID == null) {
            return XmppServer.getInstance().getNodeID();
        }
        return NodeID.getInstance(clusterMemberID);
    }

    /**
     * Returns true if the specified node ID belongs to a known cluster node
     * of this cluster.
     *
     * @param nodeID the ID of the node to verify.
     * @return true if the specified node ID belongs to a known cluster node
     *         of this cluster.
     */
    public static boolean isClusterMember(byte[] nodeID) {
        for (IClusterNodeInfo nodeInfo : getNodesInfo()) {
            if (nodeInfo.getNodeID().equals(nodeID)) {
                return true;
            }
        }
        return false;
    }
}
