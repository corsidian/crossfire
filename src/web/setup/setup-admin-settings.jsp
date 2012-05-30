<%--
  -	$RCSfile$
  -	$Revision: 1410 $
  -	$Date: 2005-05-26 23:00:40 -0700 (Thu, 26 May 2005) $
--%>

<%@ page import="net.emiva.crossfire.XMPPServer,
                 net.emiva.crossfire.auth.AuthFactory,
                 net.emiva.crossfire.user.User,
                 net.emiva.crossfire.user.UserManager,
                 net.emiva.util.EMIVAGlobals,
                 net.emiva.util.ParamUtils" %>
<%@ page import="net.emiva.util.StringUtils"%>
<%@ page import="org.xmpp.packet.JID"%>
<%@ page import="javax.servlet.http.HttpSession" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.*" %>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<%
	// Redirect if we've already run setup:
	if (!XMPPServer.getInstance().isSetupMode()) {
        response.sendRedirect("setup-completed.jsp");
        return;
    }
%>

<%! // Global vars, methods, etc
    void setSetupFinished(HttpSession session) {
        EMIVAGlobals.setXMLProperty("setup","true");
    }
%>

<%
    // Get parameters
    String username = ParamUtils.getParameter(request, "username");
    String password = ParamUtils.getParameter(request, "password");
    String email = ParamUtils.getParameter(request, "email");
    String newPassword = ParamUtils.getParameter(request, "newPassword");
    String newPasswordConfirm = ParamUtils.getParameter(request, "newPasswordConfirm");

    boolean doContinue = request.getParameter("continue") != null;
    boolean doSkip = request.getParameter("doSkip") != null;
    boolean doTest = request.getParameter("test") != null;

    boolean addAdmin = request.getParameter("addAdministrator") != null;
    boolean deleteAdmins = request.getParameter("deleteAdmins") != null;

    @SuppressWarnings("unchecked")
    Map<String,String> xmppSettings = (Map<String,String>)session.getAttribute("xmppSettings");
    String domain = xmppSettings.get("xmpp.domain");

    // Handle a skip request
    if (doSkip) {
        // assume the admin account is setup, so we're done:
        setSetupFinished(session);
        // redirect
        response.sendRedirect("setup-finished.jsp");
        return;
    }

    // Error checks
    Map<String, String> errors = new HashMap<String, String>();
    if (doContinue) {
        if (password == null) {
            errors.put("password", "password");
        }
        if (email == null) {
            errors.put("email", "email");
        }
        if (newPassword == null) {
            errors.put("newPassword", "newPassword");
        }
        if (newPasswordConfirm == null) {
            errors.put("newPasswordConfirm", "newPasswordConfirm");
        }
        if (newPassword != null && newPasswordConfirm != null
                && !newPassword.equals(newPasswordConfirm)) {
            errors.put("match", "match");
        }
        // if no errors, continue:
        if (errors.size() == 0) {
            try {
                User adminUser = UserManager.getInstance().getUser("admin");
                adminUser.setPassword(newPassword);
                if (email != null) {
                    adminUser.setEmail(email);
                }
                Date now = new Date();
                adminUser.setCreationDate(now);
                adminUser.setModificationDate(now);

                // setup is finished, indicate so:
                setSetupFinished(session);
                // All good so redirect
                response.sendRedirect("setup-finished.jsp");
                return;
            }
            catch (Exception e) {
                //System.err.println("Could not find UserManager");
                e.printStackTrace();
                errors.put("general", "There was an unexpected error encountered when "
                        + "setting the new admin information. Please check your error "
                        + "logs and try to remedy the problem.");
            }
        }
    }

    if (addAdmin && !doTest) {
        final String admin = request.getParameter("administrator");
        if (admin != null) {
            if (errors.isEmpty()) {
                String currentList = xmppSettings.get("admin.authorizedJIDs");
                final List users = new ArrayList(StringUtils.stringToCollection(currentList));
                users.add(new JID(admin.toLowerCase(), domain, null).toBareJID());

                String userList = StringUtils.collectionToString(users);
                xmppSettings.put("admin.authorizedJIDs", userList);
            }
        } else {
            errors.put("administrator", "");
        }
    }

    if (deleteAdmins) {
        String[] params = request.getParameterValues("remove");
        String currentAdminList = xmppSettings.get("admin.authorizedJIDs");
        Collection<String> adminCollection = StringUtils.stringToCollection(currentAdminList);
        List temporaryUserList = new ArrayList<String>(adminCollection);
        final int no = params != null ? params.length : 0;
        for (int i = 0; i < no; i++) {
            temporaryUserList.remove(params[i]);
        }

        String newUserList = StringUtils.collectionToString(temporaryUserList);
        if (temporaryUserList.size() == 0) {
            xmppSettings.put("admin.authorizedJIDs", "");
        } else {
            xmppSettings.put("admin.authorizedJIDs", newUserList);
        }
    }

    // This handles the case of reverting back to default settings from LDAP/Clearspace. Will
    // add admin to the authorizedJIDs list if the authorizedJIDs list contains
    // entries.
    if (!doTest) {
        String currentAdminList = xmppSettings.get("admin.authorizedJIDs");
        List<String> adminCollection = new ArrayList<String>(StringUtils.stringToCollection(currentAdminList));
        if ((!adminCollection.isEmpty() && !adminCollection.contains("admin")) ||
                xmppSettings.get("admin.authorizedJIDs") != null) {
            adminCollection.add(new JID("admin", domain, null).toBareJID());
            xmppSettings.put("admin.authorizedJIDs",
                    StringUtils.collectionToString(adminCollection));
        }
    }

    // Save the updated settings
    session.setAttribute("xmppSettings", xmppSettings);

%>
<html>
<head>
    <title><fmt:message key="setup.admin.settings.account" /></title>
    <meta name="currentStep" content="4"/>
</head>
<body>


	<h1>
	<fmt:message key="setup.admin.settings.account" />
	</h1>

    <p>
	<fmt:message key="setup.admin.settings.info" />
	</p>

<%  if (errors.size() > 0) { %>

    <div class="error">
    <%  if (errors.get("general") != null) { %>

        <%= errors.get("general") %>

    <%  } else if (errors.get("administrator") != null) { %>

        <fmt:message key="setup.admin.settings.username-error" />

    <%  } else { %>

        <fmt:message key="setup.admin.settings.error" />

    <%  } %>
    </div>

<%  } %>


	<!-- BEGIN emiva-contentBox -->
	<div class="emiva-contentBox">


<script language="JavaScript" type="text/javascript">
var clicked = false;
function checkClick() {
    if (!clicked) {
        clicked = true;
        return true;
    }
    return false;
}
</script>

<form action="setup-admin-settings.jsp" name="acctform" method="post" onsubmit="return checkClick();">

<table cellpadding="3" cellspacing="2" border="0">

<%
    // If the current password is "admin", don't show the text box for them to type
    // the current password. This makes setup simpler for first-time users.
    String currentPass = null;
    try {
        currentPass = AuthFactory.getPassword("admin");
    }
    catch (Exception e) {
        // Ignore.
    }
    if ("admin".equals(currentPass)) {
%>
<input type="hidden" name="password" value="admin">
<%
    }
    else {
%>

<tr valign="top">
    <td class="emiva-label">
        <fmt:message key="setup.admin.settings.current_password" />
    </td>
    <td>
        <input type="password" name="password" size="20" maxlength="50"
         value="<%= ((password!=null) ? password : "") %>"><br>

        <%  if (errors.get("password") != null) { %>
            <span class="emiva-error-text">
            <fmt:message key="setup.admin.settings.current_password_error" />
            </span>
        <%  } else { %>
            <span class="emiva-description">
            <fmt:message key="setup.admin.settings.current_password_description" />
            </span>
        <% } %>
    </td>
</tr>

<%  } %>

<%
    // Get the current email address, if there is one.
    String currentEmail = "";
    try {
        User adminUser = UserManager.getInstance().getUser("admin");
        if (adminUser.getEmail() != null) {
            currentEmail = adminUser.getEmail();
        }
    }
    catch (Exception e) {
        // Ignore.
    }
%>

<tr valign="top">
    <td class="emiva-label" align="right">
        <fmt:message key="setup.admin.settings.email" />
    </td>
    <td>
        <input type="text" name="email" size="40" maxlength="150"
         value="<%= ((email!=null) ? email : currentEmail) %>"><br>

        <%  if (errors.get("email") != null) { %>
            <span class="emiva-error-text">
            <fmt:message key="setup.admin.settings.email_error" />
            </span>
        <%  } else { %>
            <span class="emiva-description">
            <fmt:message key="setup.admin.settings.email_description" />
            </span>
        <% } %>
    </td>
</tr>
<tr valign="top">
    <td class="emiva-label" align="right">
        <fmt:message key="setup.admin.settings.new_password" />
    </td>
    <td>
        <input type="password" name="newPassword" size="20" maxlength="50"
         value="<%= ((newPassword!=null) ? newPassword : "") %>"><br>

        <%  if (errors.get("newPassword") != null) { %>
            <span class="emiva-error-text">
            <fmt:message key="setup.admin.settings.valid_new_password" />
            </span>
        <%  } else if (errors.get("match") != null) { %>
            <span class="emiva-error-text">
            <fmt:message key="setup.admin.settings.not_new_password" />
            </span>
        <%  } %>
    </td>
</tr>
<tr valign="top">
    <td class="emiva-label" align="right">
        <fmt:message key="setup.admin.settings.confirm_password" />
    </td>
    <td>
        <input type="password" name="newPasswordConfirm" size="20" maxlength="50"
         value="<%= ((newPasswordConfirm!=null) ? newPasswordConfirm : "") %>"><br>
        <%  if (errors.get("newPasswordConfirm") != null) { %>
            <span class="emiva-error-text">
            <fmt:message key="setup.admin.settings.valid_confirm" />
            </span>
        <%  } %>
    </td>
</tr>
</table>

<br>
		<div align="right">
			<input type="submit" name="doSkip" value="<fmt:message key="setup.admin.settings.skip_this_step" />" id="emiva-setup-skip" border="0">
			<input type="Submit" name="continue" value="<fmt:message key="global.continue" />" id="emiva-setup-save" border="0">
		</div>

	</form>
	</div>
	<!-- END emiva-contentBox -->


<script language="JavaScript" type="text/javascript">
<!--
document.acctform.newPassword.focus();
//-->
</script>


</body>
</html>
