<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%--
  -	$RCSfile$
  -	$Revision: 9934 $
  -	$Date: 2008-02-18 23:21:33 -0500 (Mon, 18 Feb 2008) $
--%>

<%@ page import="net.emiva.util.ParamUtils,
                 net.emiva.crossfire.PresenceManager,
                 net.emiva.crossfire.user.*"
    
%>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<%-- Define Administration Bean --%>
<jsp:useBean id="ad" class="net.emiva.util.WebManager"  />
<% ad.init(request, response, session, application, out ); %>


<c:set var="username" value="${param.username}" />
<c:set var="tabName" value="${pageScope.tab}" />
<jsp:useBean id="tabName" type="java.lang.String" />


<%  // Get params
    String uname = ParamUtils.getParameter(request,"username");

    // Load the user
    User foundUser = ad.getUserManager().getUser(uname);

    // Get a presence manager
    PresenceManager presenceManager = ad.getPresenceManager();
%>

<table class="emiva-tabs" cellpadding="0" cellspacing="0" border="0" width="100%">
<tr>
<c:set var="tabCount" value="1" />

    <td class="emiva-<%= (("props".equals(tabName)) ? "selected-" : "") %>tab" width="1%" nowrap>
        <a href="user-properties.jsp?username=<c:out value="${username}"/>"><fmt:message key="user.tabs.properties" /></a>
    </td>
    <td class="emiva-tab-spacer" width="1%"><img src="images/blank.gif" width="5" height="1" border="0" alt=""></td>

<c:set var="tabCount" value="${tabCount + 1}" />

    <td class="emiva-<%= (("edit".equals(tabName)) ? "selected-" : "") %>tab" width="1%" nowrap>
        <a href="user-edit-form.jsp?username=<c:out value="${username}"/>"><fmt:message key="user.tabs.edit" /></a>
    </td>
    <td class="emiva-tab-spacer" width="1%"><img src="images/blank.gif" width="5" height="1" border="0" alt=""></td>

<c:set var="tabCount" value="${tabCount + 1}" />

    <%  // Only show the message tab if the user is online
        if (presenceManager.isAvailable(foundUser)) {
    %>

        <td class="emiva-<%= (("message".equals(tabName)) ? "selected-" : "") %>tab" width="1%" nowrap>
            <a href="user-message.jsp?username=<c:out value="${username}"/>"><fmt:message key="user.tabs.send" /></a>
        </td>
        <td class="emiva-tab-spacer" width="1%"><img src="images/blank.gif" width="5" height="1" border="0" alt=""></td>

        <c:set var="tabCount" value="${tabCount + 1}" />


    <%  } %>

    <td class="emiva-<%= (("pass".equals(tabName)) ? "selected-" : "") %>tab" width="1%" nowrap>
        <a href="user-password.jsp?username=<c:out value="${username}"/>"><fmt:message key="user.tabs.change_pwd" /></a>
    </td>
    <td class="emiva-tab-spacer" width="1%"><img src="images/blank.gif" width="5" height="1" border="0" alt=""></td>

<c:set var="tabCount" value="${tabCount + 1}" />

    <td class="emiva-<%= (("delete".equals(tabName)) ? "selected-" : "") %>tab" width="1%" nowrap>
        <a href="user-delete.jsp?username=<c:out value="${username}"/>"><fmt:message key="user.tabs.delete_user" /></a>
    </td>
    <td class="emiva-tab-spacer" width="1%"><img src="images/blank.gif" width="5" height="1" border="0" alt=""></td>
<c:set var="width" value="${100-(tabCount*2)}" />
    <td class="emiva-tab-spring" width="<c:out value="${width}" />%" align="right" nowrap>
        &nbsp;
    </td>
</tr>
<tr>
    <td class="emiva-tab-bar" colspan="99">
        &nbsp;
    </td>
</tr>
</table>
<table bgcolor="#dddddd" cellpadding="0" cellspacing="0" border="0" width="100%">
<tr><td width="1%"><img src="images/blank.gif" width="1" height="1" border="0" alt=""></td></tr>
</table>
<table bgcolor="#eeeeee" cellpadding="0" cellspacing="0" border="0" width="100%">
<tr><td width="1%"><img src="images/blank.gif" width="1" height="1" border="0" alt=""></td></tr>
</table>