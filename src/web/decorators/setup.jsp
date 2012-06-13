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

<%@ page import="org.b5chat.util.LocaleUtils"%>
<%@ page import="java.beans.PropertyDescriptor"%>
<%@ page import="java.io.File"%>
<%@ page import="org.b5chat.database.DbConnectionManager"%>
<%@ page import="java.sql.Connection"%>
<%@ page import="java.util.Map"%>
<%@ page import="java.sql.Statement"%>
<%@ page import="java.sql.SQLException"%>
<%@ page import="org.b5chat.admin.AdminConsole" %>

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
            	// See if the emiva db schema is installed.
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

<!-- BEGIN emiva-main -->
<div id="main">

    <!-- BEGIN emiva-header -->
    <div id="emiva-header">
        <div id="emiva-logo">
            <a href="/index.jsp"><img src="/images/login_logo.gif" alt="crossfire" width="179" height="53" /></a>
        </div>
        <div id="emiva-userstatus">
            <%= AdminConsole.getAppName() %> <%= AdminConsole.getVersionString() %><br/>
        </div>
        <div id="emiva-nav">
            <div id="emiva-nav-left"></div>
            <ul>
                <li><a><fmt:message key="setup.title"/></a></li>
            </ul>
            <div id="emiva-nav-right"></div>
        </div>
        <div id="emiva-subnav">
            &nbsp;
        </div>
    </div>
    <!-- END emiva-header -->


    <div id="emiva-main">
    <table cellpadding="0" cellspacing="0" border="0" width="100%">
    <tbody>
        <tr valign="top">
            <td width="1%">
                <div id="emiva-sidebar-container">
                    <div id="emiva-sidebar-box">


<!-- BEGIN emiva-sidebar -->
                        <div id="emiva-sidebar">
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
                                <ul id="emiva-sidebar-progress">
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
<!-- END emiva-sidebar -->

                    </div>
                </div>
            </td>
            <td width="99%" id="emiva-content">

<!-- BEGIN emiva-body -->

                <div id="emiva-main-content">
                    <decorator:body/>
                </div>

<!-- END emiva-body -->
            </td>
        </tr>
    </tbody>
    </table>
    </div>

</div>
<!-- END emiva-main -->

<!-- BEGIN emiva-footer -->
    <div id="emiva-footer">
        <div class="emiva-footer-copyright">
            Built by the <a href="http://www.emiva.net">EMIVA</a> community
        </div>
    </div>
<!-- END emiva-footer -->

</body>
</html>