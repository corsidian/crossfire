<%@ page import="org.b5chat.crossfire.core.util.LocaleUtils" %>
<%@ page import="org.b5chat.crossfire.core.util.ParamUtils, org.b5chat.crossfire.xmpp.user.UserNotFoundException, org.xmpp.packet.JID" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.Map" %>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<%
    String username = ParamUtils.getParameter(request, "username");
    String password = ParamUtils.getParameter(request, "password");
%>