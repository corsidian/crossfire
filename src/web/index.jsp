<%--
  -	$Revision: 11592 $
  -	$Date: 2010-02-01 10:46:59 -0500 (Mon, 01 Feb 2010) $
  -
  - Copyright (C) 2004-2008 EMIVA Community. All rights reserved.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
--%>

<%@ page import="org.apache.mina.transport.socket.nio.SocketAcceptor"%>
<%@ page import="net.emiva.admin.AdminConsole"%>
<%@ page import="net.emiva.crossfire.*" %>
<%@ page import="net.emiva.crossfire.container.AdminConsolePlugin" %>
<%@ page import="net.emiva.crossfire.net.SSLConfig" %>
<%@ page import="net.emiva.crossfire.session.LocalClientSession" %>
<%@ page import="net.emiva.crossfire.session.LocalConnectionMultiplexerSession" %>
<%@ page import="net.emiva.crossfire.spi.ConnectionManagerImpl" %>
<%@ page import="net.emiva.util.*" %>
<%@ page import="java.net.InetSocketAddress" %>
<%@ page import="java.net.SocketAddress" %>
<%@ page import="java.net.URL" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="java.util.List" %>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<%-- Define page bean for header and sidebar --%>
<jsp:useBean id="pageinfo" scope="request" class="net.emiva.admin.AdminPageBean" />

<%  // Simple logout code
    if ("true".equals(request.getParameter("logout"))) {
        session.removeAttribute("emiva.admin.authToken");
        response.sendRedirect("index.jsp");
        return;
    }
%>

<%-- Define Administration Bean --%>
<jsp:useBean id="webManager" class="net.emiva.util.WebManager"  />
<% webManager.init(request, response, session, application, out); %>

<% // Get parameters //
    boolean serverOn = (webManager.getXMPPServer() != null);

    String interfaceName = EMIVAGlobals.getXMLProperty("network.interface");

    ConnectionManagerImpl connectionManager = ((ConnectionManagerImpl) XMPPServer.getInstance().getConnectionManager());
    SocketAcceptor socketAcceptor = connectionManager.getSocketAcceptor();
    SocketAcceptor sslSocketAcceptor = connectionManager.getSSLSocketAcceptor();
    SocketAcceptor multiplexerSocketAcceptor = connectionManager.getMultiplexerSocketAcceptor();
    ServerPort serverPort = null;
    ServerPort componentPort = null;
    AdminConsolePlugin adminConsolePlugin =
            (AdminConsolePlugin) XMPPServer.getInstance().getPluginManager().getPlugin("admin");

    // Search for s2s and external component ports info
    for (ServerPort port : XMPPServer.getInstance().getServerInfo().getServerPorts()) {
        if (port.getType() == ServerPort.Type.server) {
            serverPort = port;
        } else if (port.getType() == ServerPort.Type.component) {
            componentPort = port;
        }
    }
%>

<html>
    <head>
        <title><fmt:message key="index.title"/></title>
        <meta name="pageID" content="server-settings"/>
        <meta name="helpPage" content="about_the_server.html"/>
    </head>
    <body>

<style type="text/css">
.bar TD {
    padding : 0;
}
#emiva-latest-activity .emiva-bottom-line {
	padding-top: 10px;
    border-bottom : 1px #e8a400 solid;
	}
#emiva-latest-activity {
    border: 1px #E8A400 solid;
    background-color: #FFFBE2;
	font-family: Lucida Grande, Arial, Helvetica, sans-serif;
	font-size: 9pt;
    padding: 0 10px 10px 10px;
    margin-bottom: 10px;
    min-height: 280px;
    -moz-border-radius: 4px;
    width: 95%;
    margin-right: 20px;
	}
#emiva-latest-activity h4 {
	font-size: 13pt;
	margin: 15px 0 4px 0;
	}
#emiva-latest-activity h5 {
	font-size: 9pt;
	font-weight: normal;
    margin: 15px 0 5px 5px;
	padding: 0;
	}
#emiva-latest-activity .emiva-blog-date {
    font-size: 8pt;
    white-space: nowrap;
	}
#emiva-latest-activity .emiva-feed-icon {
    float: right;
    padding-top: 10px;
	}
.info-header {
    background-color: #eee;
    font-size: 10pt;
}
.info-table {
    margin-right: 12px;
}
.info-table .c1 {
    text-align: right;
    vertical-align: top;
    color: #666;
    font-weight: bold;
    font-size: 9pt;
    white-space: nowrap;
}
.info-table .c2 {
    font-size: 9pt;
    width: 90%;
}
</style>

<p>
<fmt:message key="index.title.info" />
</p>
<table border="0" width="100%">
    <td valign="top">

        <!-- <div class="emiva-table"> -->
        <table border="0" cellpadding="2" cellspacing="2" width="100%" class="info-table">
        <thead>
            <tr>
                <th colspan="2" align="left" class="info-header"><fmt:message key="index.properties" /></th>
            </tr>
        </thead>
        <tbody>

            <%  if (serverOn) { %>

                 <tr>
                    <td class="c1"><fmt:message key="index.uptime" /></td>
                    <td class="c2">
                        <%
                            long now = System.currentTimeMillis();
                            long lastStarted = webManager.getXMPPServer().getServerInfo().getLastStarted().getTime();
                            long uptime = now - lastStarted;
                            String uptimeDisplay = StringUtils.getElapsedTime(uptime);
                        %>

                        <%  if (uptimeDisplay != null) { %>
                            <%= uptimeDisplay %> -- started
                        <%  } %>

                        <%= EMIVAGlobals.formatDateTime(webManager.getXMPPServer().getServerInfo().getLastStarted()) %>
                    </td>
                </tr>

            <%  } %>

            <tr>
                <td class="c1"><fmt:message key="index.version" /></td>
                <td class="c2">
                    <%= AdminConsole.getAppName() %>
                    <%= AdminConsole.getVersionString() %>
                </td>
            </tr>
            <tr>
                <td class="c1"><fmt:message key="index.home" /></td>
                <td class="c2">
                    <%= EMIVAGlobals.getHomeDirectory() %>
                </td>
            </tr>
            <tr>
                <td class="c1">
                    <fmt:message key="index.server_name" />
                </td>
                <td class="c2">
                    <% try { %>
                    <% if (!CertificateManager.isRSACertificate(SSLConfig.getKeyStore(), XMPPServer.getInstance().getServerInfo().getXMPPDomain())) {%>
                    <img src="images/warning-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key="index.certificate-warning" />" title="<fmt:message key="index.certificate-warning" />">&nbsp;
                    <% } %>
                    <% } catch (Exception e) { %>
                    <img src="images/error-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key="index.certificate-error" />" title="<fmt:message key="index.certificate-error" />">&nbsp;
                    <% } %>
                    ${webManager.serverInfo.XMPPDomain}
                </td>
            </tr>
            <tr><td>&nbsp;</td></tr>
        </tbody>
        <thead>
            <tr>
                <th colspan="2" align="left" class="info-header"><fmt:message key="index.environment" /></th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="c1"><fmt:message key="index.jvm" /></td>
                <td class="c2">
                    <%
                        String vmName = System.getProperty("java.vm.name");
                        if (vmName == null) {
                            vmName = "";
                        }
                        else {
                            vmName = " -- " + vmName;
                        }
                    %>
                    <%= System.getProperty("java.version") %> <%= System.getProperty("java.vendor") %><%= vmName %>
                </td>
            </tr>
            <tr>
                <td class="c1"><fmt:message key="index.app" /></td>
                <td class="c2">
                    <%= application.getServerInfo() %>
                </td>
            </tr>
            <tr>
                <td class="c1">
                    <fmt:message key="index.host_name" />
                </td>
                <td class="c2">
                    ${webManager.serverInfo.hostname}
                </td>
            </tr>
            <tr>
                <td class="c1"><fmt:message key="index.os" /></td>
                <td class="c2">
                    <%= System.getProperty("os.name") %> / <%= System.getProperty("os.arch") %>
                </td>
            </tr>
            <tr>
                <td class="c1"><fmt:message key="index.local" /></td>
                <td class="c2">
                    <%= EMIVAGlobals.getLocale() %> / <%= EMIVAGlobals.getTimeZone().getDisplayName(EMIVAGlobals.getLocale()) %>
                    (<%= (EMIVAGlobals.getTimeZone().getRawOffset()/1000/60/60) %> GMT)
                </td>
            </tr>
            <tr>
                <td class="c1"><fmt:message key="index.memory" /></td>
                <td>
                <%    // The java runtime
                    Runtime runtime = Runtime.getRuntime();

                    double freeMemory = (double)runtime.freeMemory()/(1024*1024);
                    double maxMemory = (double)runtime.maxMemory()/(1024*1024);
                    double totalMemory = (double)runtime.totalMemory()/(1024*1024);
                    double usedMemory = totalMemory - freeMemory;
                    double percentFree = ((maxMemory - usedMemory)/maxMemory)*100.0;
                    double percentUsed = 100 - percentFree;
                    int percent = 100-(int)Math.round(percentFree);

                    DecimalFormat mbFormat = new DecimalFormat("#0.00");
                    DecimalFormat percentFormat = new DecimalFormat("#0.0");
                %>

                <table cellpadding="0" cellspacing="0" border="0" width="300">
                <tr valign="middle">
                    <td width="99%" valign="middle">
                        <div class="bar">
                        <table cellpadding="0" cellspacing="0" border="0" width="100%" style="border:1px #666 solid;">
                        <tr>
                            <%  if (percent == 0) { %>

                                <td width="100%"><img src="images/percent-bar-left.gif" width="100%" height="8" border="0" alt=""></td>

                            <%  } else { %>

                                <%  if (percent >= 90) { %>

                                    <td width="<%= percent %>%" background="images/percent-bar-used-high.gif"
                                        ><img src="images/blank.gif" width="1" height="8" border="0" alt=""></td>

                                <%  } else { %>

                                    <td width="<%= percent %>%" background="images/percent-bar-used-low.gif"
                                        ><img src="images/blank.gif" width="1" height="8" border="0" alt=""></td>

                                <%  } %>
                                <td width="<%= (100-percent) %>%" background="images/percent-bar-left.gif"
                                    ><img src="images/blank.gif" width="1" height="8" border="0" alt=""></td>
                            <%  } %>
                        </tr>
                        </table>
                        </div>
                    </td>
                    <td width="1%" nowrap>
                        <div style="padding-left:6px;" class="c2">
                        <%= mbFormat.format(usedMemory) %> MB of <%= mbFormat.format(maxMemory) %> MB (<%= percentFormat.format(percentUsed) %>%) used
                        </div>
                    </td>
                </tr>
                </table>
                </td>
            </tr>
        </tbody>
        </table>
        <!-- </div> -->
    </td>
</table>

<br>

<div id="emiva-title"><fmt:message key="index.server_port" /></div>
<div class="emiva-table">
<table cellpadding="0" cellspacing="0" border="0" width="100%">
<thead>
    <tr>
        <th width="80"><fmt:message key="ports.interface" /></th>
        <th width="1"><fmt:message key="ports.port" /></th>
        <th width="1">&nbsp;</th>
        <th width="130"><fmt:message key="ports.type" /></th>
        <th><fmt:message key="ports.description" /></th>
    </tr>
</thead>
<tbody>
    <% if (socketAcceptor != null) {
        for (SocketAddress socketAddress : socketAcceptor.getManagedServiceAddresses()) {
            InetSocketAddress address = (InetSocketAddress) socketAddress;
    %>
    <tr>
        <td><%= "0.0.0.0".equals(address.getHostName()) ? LocaleUtils.getLocalizedString("ports.all_ports") : address.getHostName() %></td>
        <td><%= address.getPort() %></td>
        <% try { %>
        <% if (!CertificateManager.isRSACertificate(SSLConfig.getKeyStore(), XMPPServer.getInstance().getServerInfo().getXMPPDomain()) || LocalClientSession.getTLSPolicy() == net.emiva.crossfire.Connection.TLSPolicy.disabled) { %>
            <td><img src="images/blank.gif" width="1" height="1" alt=""/></td>
        <% } else { %>
            <td><img src="images/lock.gif" width="16" height="16" border="0" alt=""/></td>
        <% } %>
        <% } catch (Exception e) { %>
            <td><img src="images/blank.gif" width="1" height="1" alt=""/></td>
        <% } %>
        <td><fmt:message key="ports.client_to_server" /></td>
        <td><fmt:message key="ports.client_to_server.desc">
            <fmt:param value="<a href='ssl-settings.jsp'>" />
            <fmt:param value="</a>" />
            </fmt:message>
        </td>
    </tr>
    <% } } %>
    <% if (sslSocketAcceptor != null) {
        for (SocketAddress socketAddress : sslSocketAcceptor.getManagedServiceAddresses()) {
            InetSocketAddress address = (InetSocketAddress) socketAddress;
    %>
    <tr>
        <td><%= "0.0.0.0".equals(address.getHostName()) ? LocaleUtils.getLocalizedString("ports.all_ports") : address.getHostName() %></td>
        <td><%= address.getPort() %></td>
        <td><img src="images/lock.gif" width="16" height="16" border="0" alt=""/></td>
        <td><fmt:message key="ports.client_to_server" /></td>
        <td><fmt:message key="ports.client_to_server.desc_old_ssl">
            <fmt:param value="<a href='ssl-settings.jsp'>" />
            <fmt:param value="</a>" />
            </fmt:message>
        </td>
    </tr>
    <% } } %>
    <%
        if (serverPort != null) {
    %>
    <tr>
        <td><%= interfaceName == null ? LocaleUtils.getLocalizedString("ports.all_ports") : serverPort.getIPAddress() %></td>
        <td><%= serverPort.getPort() %></td>
        <% if (EMIVAGlobals.getBooleanProperty("xmpp.server.tls.enabled", true)) { %>
            <td><img src="images/lock.gif" width="16" height="16" border="0" alt=""/></td>
        <% } else { %>
            <td><img src="images/blank.gif" width="1" height="1" alt=""/></td>
        <% } %>
        <td><fmt:message key="ports.server_to_server" /></td>
        <td><fmt:message key="ports.server_to_server.desc">
            <fmt:param value="<a href='server2server-settings.jsp'>" />
            <fmt:param value="</a>" />
            </fmt:message>
        </td>
        <td>
</td>
    </tr>
    <% } %>
    <% if (multiplexerSocketAcceptor != null) {
        for (SocketAddress socketAddress : multiplexerSocketAcceptor.getManagedServiceAddresses()) {
            InetSocketAddress address = (InetSocketAddress) socketAddress;
    %>
    <tr>
        <td><%= "0.0.0.0".equals(address.getHostName()) ? LocaleUtils.getLocalizedString("ports.all_ports") : address.getHostName() %></td>
        <td><%= address.getPort() %></td>
        <% if (LocalConnectionMultiplexerSession.getTLSPolicy() == net.emiva.crossfire.Connection.TLSPolicy.disabled) { %>
            <td><img src="images/blank.gif" width="1" height="1" alt=""></td>
        <% } else { %>
            <td><img src="images/lock.gif" width="16" height="16" border="0" alt=""/></td>
        <% } %>
        <td><fmt:message key="ports.connection_manager" /></td>
        <td><fmt:message key="ports.connection_manager.desc">
            <fmt:param value="<a href='connection-managers-settings.jsp'>" />
            <fmt:param value="</a>" />
            </fmt:message>
        </td>
    </tr>
    <% } } %>
    <%
        if (componentPort != null) {
    %>
    <tr>
        <td><%= interfaceName == null ? LocaleUtils.getLocalizedString("ports.all_ports") : componentPort.getIPAddress() %></td>
        <td><%= componentPort.getPort() %></td>
        <td><img src="images/blank.gif" width="1" height="1" alt=""></td>
        <td><fmt:message key="ports.external_components" /></td>
        <td><fmt:message key="ports.external_components.desc">
            <fmt:param value="<a href='external-components-settings.jsp'>" />
            <fmt:param value="</a>" />
            </fmt:message>
        </td>
    </tr>
    <% } %>
    <tr>
        <td><%= adminConsolePlugin.getBindInterface() == null ? LocaleUtils.getLocalizedString("ports.all_ports") : adminConsolePlugin.getBindInterface() %></td>
        <td><%= adminConsolePlugin.getAdminUnsecurePort() %></td>
        <td><img src="images/blank.gif" width="1" height="1" alt=""></td>
        <td><fmt:message key="ports.admin_console" /></td>
        <td><fmt:message key="ports.admin_console.desc_unsecured" /></td>
    </tr>
    <%
        if (adminConsolePlugin.getAdminSecurePort() > 0) {
    %>
    <tr>
        <td><%= adminConsolePlugin.getBindInterface() == null ? LocaleUtils.getLocalizedString("ports.all_ports") : adminConsolePlugin.getBindInterface() %></td>
        <td><%= adminConsolePlugin.getAdminSecurePort() %></td>
        <td><img src="images/lock.gif" width="16" height="16" border="0" alt=""/></td>
        <td><fmt:message key="ports.admin_console" /></td>
        <td><fmt:message key="ports.admin_console.desc_secured" /></td>
    </tr>
    <% } %>
</tbody>
</table>
</div>
<br>

<form action="server-props.jsp">
<input type="submit" value="<fmt:message key="global.edit_properties" />">
</form>

    </body>
</html>