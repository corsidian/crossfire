 <%--
  -	$RCSfile$
  -	$Revision: 7860 $
  -	$Date: 2007-04-02 19:26:20 -0400 (Mon, 02 Apr 2007) $
--%>
<%@ page import="org.b5chat.crossfire.plugin.admin.AdminConsole,
                 org.b5chat.crossfire.core.util.LocaleUtils"%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<jsp:useBean id="pageinfo" scope="request" class="org.b5chat.crossfire.plugin.admin.AdminPageBean" />

<%-- Define Administration Bean --%>
<jsp:useBean id="admin" class="org.b5chat.crossfire.core.util.WebManager"  />
<% admin.init(request, response, session, application, out ); %>

<%  String path = request.getContextPath();

    // Title of this page
    String title = AdminConsole.getAppName() + " " +LocaleUtils.getLocalizedString("error.serverdown.title");
    pageinfo.setTitle(title);
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">

<html>
<head>
 <title><%= AdminConsole.getAppName() %> <fmt:message key="error.serverdown.admin_console" /><%= (pageinfo.getTitle() != null ? (": "+pageinfo.getTitle()) : "") %></title>
 <meta http-equiv="content-type" content="text/html; charset=UTF-8">
 <link rel="stylesheet" type="text/css" href="<%= path %>/style/global.css">
</head>

<body>

<div id="b5chat-header">
<table cellpadding="0" cellspacing="0" width="100%" border="0">
<tbody>
    <tr>
     <td>
         <img src="<%= path %>/<%= AdminConsole.getLogoImage() %>" border="0" alt="<%= AdminConsole.getAppName() %> <fmt:message key="error.serverdown.admin_console" />">
     </td>
     <td align="right">
         <table cellpadding="0" cellspacing="0" border="0">
         <tr>
             <td>&nbsp;</td>
             <td class="info">
                 <nobr><%= AdminConsole.getAppName() %> <%= AdminConsole.getVersionString() %></nobr>
             </td>
         </tr>
         </table>
     </td>
    </tr>
</tbody>
</table>
</div>

<div id="b5chat-main">
<table cellpadding="0" cellspacing="0" border="0" width="100%">
<tbody>
    <tr valign="top">
        <td width="1%">
            <div id="b5chat-sidebar">
                <img src="<%= path %>/images/blank.gif" width="5" height="1" border="0" alt="">
            </div>
        </td>
        <td width="99%" id="b5chat-content">

        <div id="b5chat-title">
            <%= title %>
        </div>

        <p>
        <%= AdminConsole.getAppName() %> <fmt:message key="error.serverdown.is_down" />
        </p>

        <ol>
            <li>
                <fmt:message key="error.serverdown.start" />
            </li>
            <li>
                <a href="index.jsp"><fmt:message key="error.serverdown.login" /></a>.
            </li>
        </ol>

        </td>
    </tr>
</tbody>
</table>
</div>

</body>
</html>