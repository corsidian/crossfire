<%--
  -	$Revision: 2701 $
  -	$Date: 2005-08-19 16:48:22 -0700 (Fri, 19 Aug 2005) $
  -
  - Copyright (C) 2004-2008 B5Chat Community. All rights reserved.
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

<%@ page import="org.b5chat.crossfire.core.util.LocaleUtils"%>
<%@ page import="java.beans.PropertyDescriptor"%>
<%@ page import="java.io.File"%>
<%@ page import="org.b5chat.crossfire.database.DbConnectionManager"%>
<%@ page import="java.sql.Connection"%>
<%@ page import="java.util.Map"%>
<%@ page import="java.sql.Statement"%>
<%@ page import="java.sql.SQLException"%>
<%@ page import="org.b5chat.crossfire.plugin.admin.AdminConsole" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<%@ taglib uri="http://www.opensymphony.com/sitemesh/decorator" prefix="decorator" %>
<%@ taglib uri="http://www.opensymphony.com/sitemesh/page" prefix="page" %>

<decorator:usePage id="decoratedPage" />
<%
    // Check to see if the sidebar should be shown; default to true unless the page specifies
    // that it shouldn't be.
    String sidebar = decoratedPage.getProperty("meta.showSidebar");
    if (sidebar == null) {
        sidebar = "true";
    }
    boolean showSidebar = Boolean.parseBoolean(sidebar);
    int currentStep = decoratedPage.getIntProperty("meta.currentStep");
%>

<%
    String preloginSidebar = (String) session.getAttribute("prelogin.setup.sidebar");
    if (preloginSidebar == null) {
        preloginSidebar = "false";
    }
    boolean showPreloginSidebar = Boolean.parseBoolean(preloginSidebar);
%>

<%!
    final PropertyDescriptor getPropertyDescriptor(PropertyDescriptor[] pd, String name) {
        for (PropertyDescriptor aPd : pd) {
            if (name.equals(aPd.getName())) {
                return aPd;
            }
        }
        return null;
    }

    boolean testConnection(Map<String,String> errors) {
        boolean success = true;
        Connection con = null;
        try {
            con = DbConnectionManager.getConnection();
            if (con == null) {
                success = false;
                errors.put("general","A connection to the database could not be "
                    + "made. View the error message by opening the "
                    + "\"" + File.separator + "logs" + File.separator + "error.log\" log "
                    + "file, then go back to fix the problem.");
            }
            else {
            	// See if the b5chat db schema is installed.
            	try {
            		Statement stmt = con.createStatement();
            		// Pick an arbitrary table to see if it's there.
            		stmt.executeQuery("SELECT * FROM ofID");
            		stmt.close();
            	}
            	catch (SQLException sqle) {
                    success = false;
                    sqle.printStackTrace();
                    errors.put("general","The crossfire database schema does not "
                        + "appear to be installed. Follow the installation guide to "
                        + "fix this error.");
            	}
            }
        }
        catch (Exception ignored) {}
        finally {
            try {
        	    con.close();
            } catch (Exception ignored) {}
        }
        return success;
    }
%>

<html>
<head>
    <title><fmt:message key="title" /> <fmt:message key="setup.title" />: <decorator:title /></title>

    <style type="text/css" title="setupStyle" media="screen">
        @import "../style/global.css";
        @import "../style/setup.css";
        @import "../style/lightbox.css";
    </style>

    <script language="JavaScript" type="text/javascript" src="../js/prototype.js"></script>
    <script language="JavaScript" type="text/javascript" src="../js/scriptaculous.js"></script>
    <script language="JavaScript" type="text/javascript" src="../js/lightbox.js"></script>
    <script language="javascript" type="text/javascript" src="../js/tooltips/domLib.js"></script>
    <script language="javascript" type="text/javascript" src="../js/tooltips/domTT.js"></script>
    <script language="javascript" type="text/javascript" src="../js/setup.js"></script>
    <decorator:head />
</head>

<body onload="<decorator:getProperty property="body.onload" />">

<!-- BEGIN b5chat-main -->
<div id="main">

    <!-- BEGIN b5chat-header -->
    <div id="b5chat-header">
        <div id="b5chat-logo">
            <a href="/index.jsp"><img src="/images/login_logo.gif" alt="crossfire" width="179" height="53" /></a>
        </div>
        <div id="b5chat-userstatus">
            <%= AdminConsole.getAppName() %> <%= AdminConsole.getVersionString() %><br/>
        </div>
        <div id="b5chat-nav">
            <div id="b5chat-nav-left"></div>
            <ul>
                <li><a><fmt:message key="setup.title"/></a></li>
            </ul>
            <div id="b5chat-nav-right"></div>
        </div>
        <div id="b5chat-subnav">
            &nbsp;
        </div>
    </div>
    <!-- END b5chat-header -->


    <div id="b5chat-main">
    <table cellpadding="0" cellspacing="0" border="0" width="100%">
    <tbody>
        <tr valign="top">
            <td width="1%">
                <div id="b5chat-sidebar-container">
                    <div id="b5chat-sidebar-box">


<!-- BEGIN b5chat-sidebar -->
                        <div id="b5chat-sidebar">
                            <%  if (showSidebar) {
                                    String[] names;
                                    String[] links;
                                    if (showPreloginSidebar) {
                                        names = new String[] {
                                                LocaleUtils.getLocalizedString((String) session.getAttribute("prelogin.setup.sidebar.title"))
                                        };
                                        links = new String[] {
                                                (String) session.getAttribute("prelogin.setup.sidebar.link")
                                        };
                                    } else {
                                        names = new String[] {
                                             LocaleUtils.getLocalizedString("setup.sidebar.language"),
                                             LocaleUtils.getLocalizedString("setup.sidebar.settings"),
                                             LocaleUtils.getLocalizedString("setup.sidebar.datasource"),
                                             LocaleUtils.getLocalizedString("setup.sidebar.profile"),
                                             LocaleUtils.getLocalizedString("setup.sidebar.admin")
                                         };
                                         links = new String[] {
                                             "index.jsp",
                                             "setup-host-settings.jsp",
                                             "setup-datasource-settings.jsp",
                                             "setup-profile-settings.jsp",
                                             "setup-admin-settings.jsp"
                                         };
                                    }
                                    %>
                                <ul id="b5chat-sidebar-progress">
                                    <%  if (!showPreloginSidebar) { %>
                                    <li class="category"><fmt:message key="setup.sidebar.title" /></li>
                                    <li><img src="../images/setup_sidebar_progress<%= currentStep %>.gif" alt="" width="142" height="13" border="0"></li>
                                    <%  } %>
                                    <%  for (int i=0; i<names.length; i++) { %>
                                        <%  if (currentStep < i) { %>
                                        <li><a href="<%= links[i] %>"><%= names[i] %></a></li>
                                        <%  } else if (currentStep == i) { %>
                                        <li class="currentlink"><a href="<%= links[i] %>"><%= names[i] %></a></li>
                                        <%  } else { %>
                                        <li class="completelink"><a href="<%= links[i] %>"><%= names[i] %></a></li>
                                        <%  } %>
                                    <%  } %>
                                </ul>

                            <%  } %>


                        </div>
<!-- END b5chat-sidebar -->

                    </div>
                </div>
            </td>
            <td width="99%" id="b5chat-content">

<!-- BEGIN b5chat-body -->

                <div id="b5chat-main-content">
                    <decorator:body/>
                </div>

<!-- END b5chat-body -->
            </td>
        </tr>
    </tbody>
    </table>
    </div>

</div>
<!-- END b5chat-main -->

<!-- BEGIN b5chat-footer -->
    <div id="b5chat-footer">
        <div class="b5chat-footer-copyright">
            Built by the <a href="http://www.b5chat.org">B5Chat</a> community
        </div>
    </div>
<!-- END b5chat-footer -->

</body>
</html>