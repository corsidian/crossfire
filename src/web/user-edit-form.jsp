<%--
  -	$Revision: 11592 $
  -	$Date: 2010-02-01 10:46:59 -0500 (Mon, 01 Feb 2010) $
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

<%@ page import="org.b5chat.crossfire.core.util.ParamUtils,
                 org.b5chat.crossfire.xmpp.user.*,
                 java.net.URLEncoder"
    errorPage="error.jsp"
%><%@ page import="org.xmpp.packet.JID"%>
<%@ page import="org.b5chat.crossfire.security.SecurityAuditManager" %>
<%@ page import="org.b5chat.crossfire.core.util.StringUtils" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="org.b5chat.crossfire.core.admin.AdminManager" %>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.b5chat.crossfire.core.util.WebManager" />
<% webManager.init(request, response, session, application, out ); %>

<%  // Get parameters
    boolean save = ParamUtils.getBooleanParameter(request,"save");
    boolean success = ParamUtils.getBooleanParameter(request,"success");
    String username = ParamUtils.getParameter(request,"username");
    String name = ParamUtils.getParameter(request,"name");
    String email = ParamUtils.getParameter(request,"email");
    boolean isAdmin = ParamUtils.getBooleanParameter(request,"isadmin");
    Map<String, String> errors = new HashMap<String, String>();

    // Handle a cancel
    if (request.getParameter("cancel") != null) {
        response.sendRedirect("user-properties.jsp?username=" + URLEncoder.encode(username, "UTF-8"));
        return;
    }

    // Load the user object
    User user = webManager.getUserManager().getUser(username);

    // Handle a save
    if (save) {
        // If provider requires email, validate
        if (UserManager.getUserProvider().isEmailRequired()) {
            if (!StringUtils.isValidEmailAddress(email)) {
                errors.put("email","");
            }
        }
        // If provider requires name, validate
        if (UserManager.getUserProvider().isNameRequired()) {
            if (name == null || name.equals("")) {
                errors.put("name","");
            }
        }

        if (errors.size() == 0) {
            user.setEmail(email);
            user.setName(name);

            if (!AdminManager.getAdminProvider().isReadOnly()) {
                boolean isCurrentAdmin = AdminManager.getInstance().isUserAdmin(user.getUsername(), false);
                if (isCurrentAdmin && !isAdmin) {
                    AdminManager.getInstance().removeAdminAccount(user.getUsername());
                }
                else if (!isCurrentAdmin && isAdmin) {
                    AdminManager.getInstance().addAdminAccount(user.getUsername());
                }
            }

            if (!SecurityAuditManager.getSecurityAuditProvider().blockUserEvents()) {
                // Log the event
                webManager.logEvent("edited user "+username, "set name = "+name+", email = "+email+", admin = "+isAdmin);
            }

            // Changes good, so redirect
            response.sendRedirect("user-properties.jsp?editsuccess=true&username=" + URLEncoder.encode(username, "UTF-8"));
            return;
        }
    }
%>

<html>
    <head>
        <title><fmt:message key="user.edit.form.title"/></title>
        <meta name="subPageID" content="user-properties"/>
        <meta name="extraParams" content="<%= "username="+URLEncoder.encode(username, "UTF-8") %>"/>
    </head>
    <body>
<%  if (!errors.isEmpty()) { %>

    <div class="b5chat-error">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr>
            <td class="b5chat-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0" alt=""/></td>
            <td class="b5chat-icon-label">

            <% if (errors.get("name") != null) { %>
                <fmt:message key="user.create.invalid_name" />
            <% } else if (errors.get("email") != null) { %>
                <fmt:message key="user.create.invalid_email" />
            <% } %>
            </td>
        </tr>
    </tbody>
    </table>
    </div>
    <br>

<%  } else if (success) { %>

    <div class="b5chat-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="b5chat-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="b5chat-icon-label">
        <fmt:message key="user.edit.form.update" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } %>

<p>
<fmt:message key="user.edit.form.info" />
</p>

<form action="user-edit-form.jsp">

<input type="hidden" name="username" value="<%= username %>">
<input type="hidden" name="save" value="true">

<fieldset>
    <legend><fmt:message key="user.edit.form.property" /></legend>
    <div>
    <table cellpadding="3" cellspacing="0" border="0" width="100%">
    <tbody>
        <tr>
            <td class="c1">
                <fmt:message key="user.create.username" />:
            </td>
            <td>
                <%= JID.unescapeNode(user.getUsername()) %>
            </td>
        </tr>
        <tr>
            <td class="c1">
                <fmt:message key="user.create.name" />: <%= UserManager.getUserProvider().isNameRequired() ? "*" : "" %>
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="name"
                 value="<%= user.getName() %>">
            </td>
        </tr>
        <tr>
            <td class="c1">
                <fmt:message key="user.create.email" />: <%= UserManager.getUserProvider().isEmailRequired() ? "*" : "" %>
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="email"
                 value="<%= ((user.getEmail()!=null) ? user.getEmail() : "") %>">
            </td>
        </tr>
        <% if (!AdminManager.getAdminProvider().isReadOnly()) { %>
        <tr>
            <td class="c1">
                <fmt:message key="user.create.isadmin" />
            </td>
            <td>
                <input type="checkbox" name="isadmin"<%= AdminManager.getInstance().isUserAdmin(user.getUsername(), false) ? " checked='checked'" : "" %>>
                (<fmt:message key="user.create.admin_info"/>)
            </td>
        </tr>
        <% } %>
    </tbody>
    </table>
    </div>

</fieldset>

<br><br>

<input type="submit" value="<fmt:message key="global.save_properties" />">
<input type="submit" name="cancel" value="<fmt:message key="global.cancel" />">

</form>

<br/>

<span class="b5chat-description">
* <fmt:message key="user.create.requied" />
</span>

    </body>
</html>