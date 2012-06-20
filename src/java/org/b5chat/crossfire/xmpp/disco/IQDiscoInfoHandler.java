/**
 * $RCSfile: IQDiscoInfoHandler.java,v $
 * $Revision: 2859 $
 * $Date: 2005-09-22 02:30:39 -0300 (Thu, 22 Sep 2005) $
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

package org.b5chat.crossfire.xmpp.disco;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;


import org.b5chat.crossfire.core.property.Globals;
import org.b5chat.crossfire.core.server.NodeID;
import org.b5chat.crossfire.core.util.cache.Cache;
import org.b5chat.crossfire.core.util.cache.CacheFactory;
import org.b5chat.crossfire.xmpp.handler.IQHandler;
import org.b5chat.crossfire.xmpp.handler.IQHandlerInfo;
import org.b5chat.crossfire.xmpp.server.XmppServer;
import org.b5chat.crossfire.xmpp.session.SessionManager;
import org.b5chat.crossfire.xmpp.user.IUserIdentitiesProvider;
import org.b5chat.crossfire.xmpp.user.UserManager;
import org.b5chat.crossfire.xmpp.user.UserNotFoundException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;
import org.xmpp.forms.DataForm;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;
import org.xmpp.resultsetmanagement.ResultSet;

/**
 * IQDiscoInfoHandler is responsible for handling disco#info requests. This class holds a map with
 * the main entities and the associated IDiscoInfoProvider. We are considering the host of the
 * recipient JIDs as main entities. It's the IDiscoInfoProvider responsibility to provide information
 * about the JID's name together with any possible requested node.<p>
 * <p/>
 * For example, let's have in the entities map the following entries: "localhost" and
 * "conference.localhost". Associated with each entry we have different DiscoInfoProviders. Now we
 * receive a disco#info request for the following JID: "room@conference.localhost" which is a disco
 * request for a MUC room. So IQDiscoInfoHandler will look for the IDiscoInfoProvider associated
 * with the JID's host which in this case is "conference.localhost". Once we have located the
 * provider we will delegate to the provider the responsibility to provide the info specific to
 * the JID's name which in this case is "room". Among the information that a room could provide we
 * could find its identity and the features it supports (e.g. 'muc_passwordprotected',
 * 'muc_unmoderated', etc.). Finally, after we have collected all the information provided by the
 * provider we will add it to the reply. On the other hand, if no provider was found or the provider
 * has no information for the requested name/node then a not-found error will be returned.
 *
 * @author Gaston Dombiak
 */
public class IQDiscoInfoHandler extends IQHandler {

    public static final String NAMESPACE_DISCO_INFO = "http://jabber.org/protocol/disco#info";
	private Map<String, IDiscoInfoProvider> entities = new HashMap<String, IDiscoInfoProvider>();
    private Set<String> localServerFeatures = new CopyOnWriteArraySet<String>();
    private Cache<String, Set<NodeID>> serverFeatures;
    private List<Element> serverIdentities = new ArrayList<Element>();
    private Map<String, IDiscoInfoProvider> serverNodeProviders = new ConcurrentHashMap<String, IDiscoInfoProvider>();
    private IQHandlerInfo info;

    private List<Element> anonymousUserIdentities = new ArrayList<Element>();
    private List<Element> registeredUserIdentities = new ArrayList<Element>();
    private List<String> userFeatures = new ArrayList<String>();

    public IQDiscoInfoHandler() {
        super("XMPP Disco Info Handler");
        info = new IQHandlerInfo("query", NAMESPACE_DISCO_INFO);
        // Initialize the user identity and features collections (optimization to avoid creating
        // the same objects for each response)
        Element userIdentity = DocumentHelper.createElement("identity");
        userIdentity.addAttribute("category", "account");
        userIdentity.addAttribute("type", "anonymous");
        anonymousUserIdentities.add(userIdentity);
        userIdentity = DocumentHelper.createElement("identity");
        userIdentity.addAttribute("category", "account");
        userIdentity.addAttribute("type", "registered");
        registeredUserIdentities.add(userIdentity);
        userFeatures.add(NAMESPACE_DISCO_INFO);
    }

    @Override
	public IQHandlerInfo getInfo() {
        return info;
    }

    @Override
	public IQ handleIQ(IQ packet) {
        // Create a copy of the sent pack that will be used as the reply
        // we only need to add the requested info to the reply if any otherwise add 
        // a not found error
        IQ reply = IQ.createResultIQ(packet);

        // Look for a IDiscoInfoProvider associated with the requested entity.
        // We consider the host of the recipient JID of the packet as the entity. It's the 
        // IDiscoInfoProvider responsibility to provide information about the JID's name together 
        // with any possible requested node.  
        IDiscoInfoProvider infoProvider = getProvider(packet.getTo() == null ?
                XmppServer.getInstance().getServerInfo().getXMPPDomain() : packet.getTo().getDomain());
        if (infoProvider != null) {
            // Get the JID's name
            String name = packet.getTo() == null ? null : packet.getTo().getNode();
            if (name == null || name.trim().length() == 0) {
                name = null;
            }
            // Get the requested node
            Element iq = packet.getChildElement();
            String node = iq.attributeValue("node");
            //String node = metaData.getProperty("query:node");

            // Check if we have information about the requested name and node
            if (infoProvider.hasInfo(name, node, packet.getFrom())) {
                reply.setChildElement(iq.createCopy());
                Element queryElement = reply.getChildElement();

                // Add to the reply all the identities provided by the IDiscoInfoProvider
                Element identity;
                Iterator<Element> identities = infoProvider.getIdentities(name, node, packet.getFrom());
                while (identities.hasNext()) {
                    identity = identities.next();
                    identity.setQName(new QName(identity.getName(), queryElement.getNamespace()));
                    queryElement.add((Element)identity.clone());
                }

                // Add to the reply all the features provided by the IDiscoInfoProvider
                Iterator<String> features = infoProvider.getFeatures(name, node, packet.getFrom());
                boolean hasDiscoInfoFeature = false;
                boolean hasDiscoItemsFeature = false;
                boolean hasResultSetManagementFeature = false;
                
                while (features.hasNext()) {
					final String feature = features.next();
					queryElement.addElement("feature").addAttribute("var", feature);
					if (feature.equals(NAMESPACE_DISCO_INFO)) {
						hasDiscoInfoFeature = true;
					} else if (feature.equals("http://jabber.org/protocol/disco#items")) {
						hasDiscoItemsFeature = true;
					} else if (feature.equals(ResultSet.NAMESPACE_RESULT_SET_MANAGEMENT)) {
						hasResultSetManagementFeature = true;
					}
                }

				if (hasDiscoItemsFeature && !hasResultSetManagementFeature) {
					// IQDiscoItemsHandler provides result set management
					// support.
					queryElement.addElement("feature").addAttribute("var",
							ResultSet.NAMESPACE_RESULT_SET_MANAGEMENT);
				}
                
                if (!hasDiscoInfoFeature) {
					// XEP-0030 requires that every entity that supports service
					// discovery broadcasts the disco#info feature.
					queryElement.addElement("feature").addAttribute("var", NAMESPACE_DISCO_INFO);
				}
                
                // Add to the reply the extended info (XDataForm) provided by the IDiscoInfoProvider
                DataForm dataForm = infoProvider.getExtendedInfo(name, node, packet.getFrom());
                if (dataForm != null) {
                    queryElement.add(dataForm.getElement());
                }
            }
            else {
                // If the IDiscoInfoProvider has no information for the requested name and node 
                // then answer a not found error
                reply.setChildElement(packet.getChildElement().createCopy());
                reply.setError(PacketError.Condition.item_not_found);
            }
        }
        else {
            // If we didn't find a IDiscoInfoProvider then answer a not found error
            reply.setChildElement(packet.getChildElement().createCopy());
            reply.setError(PacketError.Condition.item_not_found);
        }

        return reply;
    }

    /**
     * Sets the IDiscoInfoProvider to use when a disco#info packet is sent to the server itself
     * and the specified node. For instance, if node matches "http://jabber.org/protocol/offline"
     * then a special IDiscoInfoProvider should be use to return information about offline messages.
     *
     * @param node the node that the provider will handle.
     * @param provider the IDiscoInfoProvider that will handle disco#info packets sent with the
     *        specified node.
     */
    public void setServerNodeInfoProvider(String node, IDiscoInfoProvider provider) {
        serverNodeProviders.put(node, provider);
    }

    /**
     * Removes the IDiscoInfoProvider to use when a disco#info packet is sent to the server itself
     * and the specified node.
     *
     * @param node the node that the provider was handling.
     */
    public void removeServerNodeInfoProvider(String node) {
        serverNodeProviders.remove(node);
    }

    /**
     * Returns the IDiscoInfoProvider responsible for providing information about a given entity or
     * null if none was found.
     *
     * @param name the name of the identity.
     * @return the IDiscoInfoProvider responsible for providing information about a given entity or
     *         null if none was found.
     */
    private IDiscoInfoProvider getProvider(String name) {
        return entities.get(name);
    }

    /**
     * Sets that a given IDiscoInfoProvider will provide information about a given entity. This
     * message must be used when new modules (e.g. MUC) are implemented and need to provide
     * information about them.
     *
     * @param name     the name of the entity.
     * @param provider the IDiscoInfoProvider that will provide the entity's information.
     */
    protected void setProvider(String name, IDiscoInfoProvider provider) {
        entities.put(name, provider);
    }

    /**
     * Removes the IDiscoInfoProvider related to a given entity.
     *
     * @param name the name of the entity.
     */
    protected void removeProvider(String name) {
        entities.remove(name);
    }

    /**
     * Adds the features provided by the new service that implements the IServerFeaturesProvider
     * interface. This information will be used whenever a disco for information is made against
     * the server (i.e. the packet's target is the server).
     * Example of features are: jabber:iq:agents, jabber:iq:time, etc.
     *
     * @param provider the IServerFeaturesProvider that provides new server features.
     */
    private void addServerFeaturesProvider(IServerFeaturesProvider provider) {
        for (Iterator<String> it = provider.getFeatures(); it.hasNext();) {
            addServerFeature(it.next());
        }
    }

    /**
     * Adds one specific feature to the information returned whenever a disco for information is
     * made against the server.
     *
     * @param namespace the namespace identifying the new server feature.
     */
    public void addServerFeature(String namespace) {
        if (localServerFeatures.add(namespace)) {
            Lock lock = CacheFactory.getLock(namespace, serverFeatures);
            try {
                lock.lock();
                Set<NodeID> nodeIDs = serverFeatures.get(namespace);
                if (nodeIDs == null) {
                    nodeIDs = new HashSet<NodeID>();
                }
                nodeIDs.add(XmppServer.getInstance().getNodeID());
                serverFeatures.put(namespace, nodeIDs);
            }
            finally {
                lock.unlock();
            }
        }
    }

    /**
     * Removes a feature from the information returned whenever a disco for information is
     * made against the server.
     *
     * @param namespace the namespace of the feature to be removed.
     */
    public void removeServerFeature(String namespace) {
        if (localServerFeatures.remove(namespace)) {
            Lock lock = CacheFactory.getLock(namespace, serverFeatures);
            try {
                lock.lock();
                Set<NodeID> nodeIDs = serverFeatures.get(namespace);
                if (nodeIDs != null) {
                    nodeIDs.remove(XmppServer.getInstance().getNodeID());
                    if (nodeIDs.isEmpty()) {
                        serverFeatures.remove(namespace);
                    }
                    else {
                        serverFeatures.put(namespace, nodeIDs);
                    }
                }
            }
            finally {
                lock.unlock();
            }
        }
    }

    @Override
	public void initialize(XmppServer server) {
        super.initialize(server);
        serverFeatures = CacheFactory.createCache("Disco Server Features");
        addServerFeature(NAMESPACE_DISCO_INFO);
        // Track the implementors of IServerFeaturesProvider so that we can collect the features
        // provided by the server
        for (IServerFeaturesProvider provider : server.getServerFeaturesProviders()) {
            addServerFeaturesProvider(provider);
        }
        // Collect the implementors of IServerIdentitiesProvider so that we can collect the identities
        // for protocols supported by the server
        for (IServerIdentitiesProvider provider : server.getServerIdentitiesProviders()) {
            for (Iterator<Element> it = provider.getIdentities(); it.hasNext();) {
                serverIdentities.add(it.next());
            }
        }
        // Collect the implementors of IUserIdentitiesProvider so that we can collect identities
        // for registered users.
        for (IUserIdentitiesProvider provider : server.getUserIdentitiesProviders()) {
            for (Iterator<Element> it = provider.getIdentities(); it.hasNext();) {
                registeredUserIdentities.add(it.next());
            }
        }

        setProvider(server.getServerInfo().getXMPPDomain(), getServerInfoProvider());
    }

    /**
     * Returns the IDiscoInfoProvider responsible for providing information at the server level. This
     * means that this IDiscoInfoProvider will provide information whenever a disco request whose
     * recipient JID is the server (e.g. localhost) is made.
     *
     * @return the IDiscoInfoProvider responsible for providing information at the server level.
     */
    private IDiscoInfoProvider getServerInfoProvider() {
        return new IDiscoInfoProvider() {
            final ArrayList<Element> identities = new ArrayList<Element>();

            public Iterator<Element> getIdentities(String name, String node, JID senderJID) {
                if (node != null && serverNodeProviders.get(node) != null) {
                    // Redirect the request to the disco info provider of the specified node
                    return serverNodeProviders.get(node).getIdentities(name, node, senderJID);
                }
                if (name == null) {
                    // Answer identity of the server
                    synchronized (identities) {
                        if (identities.isEmpty()) {
                            Element identity = DocumentHelper.createElement("identity");
                            identity.addAttribute("category", "server");
                            identity.addAttribute("name", Globals.getProperty(
                                    "xmpp.server.name", "crossfire Server"));
                            identity.addAttribute("type", "im");

                            identities.add(identity);
                            
                            // Include identities from modules that implement IServerIdentitiesProvider
                            for (Element identityElement : serverIdentities) {
                                identities.add(identityElement);
                            }
                        }
                    }
                    return identities.iterator();
                }
                else {
                    if (SessionManager.getInstance().isAnonymousRoute(name)) {
                        // Answer identity of an anonymous user.
                        return anonymousUserIdentities.iterator();
                    }
                    else {
                        // Answer identity of a registered user.
                        // Note: We know that this user exists because #hasInfo returned true
                        return registeredUserIdentities.iterator();
                    }
                }
            }

            public Iterator<String> getFeatures(String name, String node, JID senderJID) {
                if (node != null && serverNodeProviders.get(node) != null) {
                    // Redirect the request to the disco info provider of the specified node
                    return serverNodeProviders.get(node).getFeatures(name, node, senderJID);
                }
                if (name == null) {
                    // Answer features of the server
                    return new HashSet<String>(serverFeatures.keySet()).iterator();
                }
                else {
                    // Answer features of the user
                    return userFeatures.iterator();
                }
            }

            public boolean hasInfo(String name, String node, JID senderJID) {
                if (node != null) {
                    if (serverNodeProviders.get(node) != null) {
                        // Redirect the request to the disco info provider of the specified node
                        return serverNodeProviders.get(node).hasInfo(name, node, senderJID);
                    }
                    // Unknown node
                    return false;
                }
                try {
                    // True if it is an info request of the server, a registered user or an
                    // anonymous user. We now support disco of user's bare JIDs
                    return name == null || UserManager.getInstance().getUser(name) != null ||
                            SessionManager.getInstance().isAnonymousRoute(name);
                }
                catch (UserNotFoundException e) {
                    return false;
                }
            }

            public DataForm getExtendedInfo(String name, String node, JID senderJID) {
                if (node != null && serverNodeProviders.get(node) != null) {
                    // Redirect the request to the disco info provider of the specified node
                    return serverNodeProviders.get(node).getExtendedInfo(name, node, senderJID);
                }
                return null;
            }
        };
    }
}