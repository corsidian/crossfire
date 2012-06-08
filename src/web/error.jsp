<%--
  -	$RCSfile$
  -	$Revision: 10128 $
  -	$Date: 2008-03-26 13:15:36 -0400 (Wed, 26 Mar 2008) $
--%>

<%@ page import="java.io.*,
                 net.emiva.util.ParamUtils,
                 net.emiva.util.Globals,
                 net.emiva.crossfire.auth.UnauthorizedException,
                 net.emiva.crossfire.user.UserNotFoundException,
                 net.emiva.crossfire.group.GroupNotFoundException"
    isErrorPage="true"
%>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<fmt:setBundle basename="crossfire_i18n"/>
<%  boolean debug = "true".equals(Globals.getProperty("skin.default.debug"));
    if (debug) {
        exception.printStackTrace();
    }
%>

<%  if (exception instanceof UnauthorizedException) { %>

    <p>
    <fmt:message key="error.admin_privileges" />
    </p>

<%  } else if (exception instanceof UserNotFoundException) {
        String username = ParamUtils.getParameter(request,"username");
%>
        <p>
        <%  if (username == null) { %>
            <fmt:message key="error.requested_user_not_found" />
        <%  } else { %>
            <fmt:message key="error.specific_user_not_found">
                <fmt:param value="${username}" />
            </fmt:message>
        <%  } %>
        </p>

<%  } else if (exception instanceof GroupNotFoundException) { %>

    <p>
    <fmt:message key="error.not_found_group" />
    </p>
    
<%  } %>

<%  if (exception != null) {
        StringWriter sout = new StringWriter();
        PrintWriter pout = new PrintWriter(sout);
        exception.printStackTrace(pout);
%>
    <fmt:message key="error.exception" />
    <pre>
<%= sout.toString() %>
    </pre>

<%  } %>