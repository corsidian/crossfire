<%--
  -	$Revision: 2701 $
  -	$Date: 2005-08-19 16:48:22 -0700 (Fri, 19 Aug 2005) $
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

<%@ page import="net.emiva.util.StringUtils,
                 net.emiva.admin.AdminConsole,
                 net.emiva.util.LocaleUtils"
    errorPage="../error.jsp"
%><%@ page import="org.xmpp.packet.JID"%>

<%@ taglib uri="admin" prefix="admin" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<%@ taglib uri="http://www.opensymphony.com/sitemesh/decorator" prefix="decorator" %>
<%@ taglib uri="http://www.opensymphony.com/sitemesh/page" prefix="page" %>

<jsp:useBean id="info" scope="request" class="net.emiva.admin.AdminPageBean" />

<jsp:useBean id="webManager" class="net.emiva.util.WebManager"  />
<% webManager.init(request, response, session, application, out); %>

<decorator:usePage id="decoratedPage" />

<%
    String path = request.getContextPath();
    // Decorated pages will typically must set a pageID and optionally set a subPageID
    // and extraParams. Store these values as request attributes so that the tab and sidebar
    // handling tags can get at the data.
    request.setAttribute("pageID", decoratedPage.getProperty("meta.pageID"));
    request.setAttribute("subPageID", decoratedPage.getProperty("meta.subPageID"));
    request.setAttribute("extraParams", decoratedPage.getProperty("meta.extraParams"));

    // Message HTML can be passed in:
    String message = decoratedPage.getProperty("page.message");
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">

<fmt:setBundle basename="crossfire_i18n"/>
<html>
<head>
    <title><%= AdminConsole.getAppName() %> <fmt:message key="login.title" />: <decorator:title /></title>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <link rel="stylesheet" type="text/css" href="<%= path %>/style/global.css">
    <script language="JavaScript" type="text/javascript" src="<%= path %>/js/prototype.js"></script>
    <script language="JavaScript" type="text/javascript" src="<%= path %>/js/scriptaculous.js"></script>
    <script language="JavaScript" type="text/javascript" src="<%= path %>/js/cookies.js"></script>
    <script language="JavaScript" type="text/javascript">

    </script>
    <script type="text/javascript" src="<%= path %>/js/behaviour.js"></script>
    <script type="text/javascript">
    // Add a nice little rollover effect to any row in a emiva-table object. This will help
    // visually link left and right columns.
    /*
    var myrules = {
        '.emiva-table TBODY TR' : function(el) {
            el.onmouseover = function() {
                this.style.backgroundColor = '#ffffee';
            }
            el.onmouseout = function() {
                this.style.backgroundColor = '#ffffff';
            }
        }
    };
    Behaviour.register(myrules);
    */
    </script>
    <decorator:head />
</head>

<body id="emiva-body">

<!-- BEGIN main -->
<div id="main">

    <div id="emiva-header">
        <div id="emiva-logo">
            <a href="/index.jsp"><img src="/images/login_logo.gif" alt="crossfire" width="179" height="53" /></a>
        </div>
        <div id="emiva-userstatus">
            <%= AdminConsole.getAppName() %> <%= AdminConsole.getVersionString() %><br/>
            <fmt:message key="admin.logged_in_as"><fmt:param value="<%= "<strong>"+StringUtils.escapeHTMLTags(JID.unescapeNode(webManager.getUser().getUsername()))+"</strong>" %>"/></fmt:message> - <a href="<%= path %>/index.jsp?logout=true"><%= LocaleUtils.getLocalizedString("global.logout") %></a>
        </div>
        <div id="emiva-nav">
            <div id="emiva-nav-left"></div>
            <admin:tabs css="" currentcss="currentlink">
            <a href="[url]" title="[description]" onmouseover="self.status='[description]';return true;" onmouseout="self.status='';return true;">[name]</a>
            </admin:tabs>
            <div id="emiva-nav-right"></div>
        </div>
        <div id="emiva-subnav">
            <admin:subnavbar css="" currentcss="current">
                <a href="[url]" title="[description]"
                  onmouseover="self.status='[description]';return true;" onmouseout="self.status='';return true;"
                  >[name]</a>
            </admin:subnavbar>
        </div>
    </div>

    <div id="emiva-main">
    <table cellpadding="0" cellspacing="0" border="0" width="100%">
    <tbody>
        <tr valign="top">
            <td width="1%">
                <div id="emiva-sidebar-container">
                    <div id="emiva-sidebar-box">
                        <div id="emiva-sidebar">
                            <admin:sidebar css="" currentcss="currentlink" headercss="category">
                                <a href="[url]" title="[description]"
                                  onmouseover="self.status='[description]';return true;" onmouseout="self.status='';return true;"
                                  >[name]</a>
                                 <admin:subsidebar css="" currentcss="currentlink">
                                    <a href="[url]" title="[description]"
                                     onmouseover="self.status='[description]';return true;" onmouseout="self.status='';return true;"
                                     >[name]</a>
                                 </admin:subsidebar>
                            </admin:sidebar>
                            <br>
                            <img src="<%= path %>/images/blank.gif" width="150" height="1" border="0" alt="">
                        </div>
                    </div>
                </div>
            </td>
            <td width="99%" id="emiva-content">


                <%  if (message != null) { %>

                    <%= message %>

                <%  } %>

                <h1>
                    <decorator:title default="&nbsp;"/>
                </h1>

                <div id="emiva-main-content">
                    <decorator:body/>
                </div>
            </td>
        </tr>
    </tbody>
    </table>
    </div>

</div>
<!-- END main -->

<!-- BEGIN footer -->
	<div id="emiva-footer">
        <div class="emiva-footer-nav">
            <admin:tabs css="" currentcss="currentlink" justlinks="true">
            <a href="[url]" title="[description]" onmouseover="self.status='[description]';return true;" onmouseout="self.status='';return true;">[name]</a>
            </admin:tabs>
        </div>
        <div class="emiva-footer-copyright">
            Built by the <a href="http://www.emiva.net">EMIVA</a> community
        </div>
    </div>
<!-- END footer -->

</body>
</html>
