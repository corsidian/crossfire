<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<%--
  -	$RCSfile$
  -	$Revision: 1410 $
  -	$Date: 2005-05-26 23:00:40 -0700 (Thu, 26 May 2005) $
--%>

<%@ page import="net.emiva.util.ParamUtils,
                 net.emiva.util.Globals,
                 net.emiva.database.EmbeddedConnectionProvider,
                 net.emiva.database.DbConnectionManager,
                 net.emiva.database.IConnectionProvider,
                 java.util.*" %>
<%@ page import="java.io.File"%>
<%@ page import="java.sql.Connection"%>
<%@ page import="java.sql.Statement"%>
<%@ page import="java.sql.SQLException"%>
<%@ page import="net.emiva.util.LocaleUtils"%>
<%@ page import="net.emiva.util.ClassUtils"%>
<%@ page import="net.emiva.crossfire.XmppServer"%>

<%
	// Redirect if we've already run setup:
	if (!XMPPServer.getInstance().isSetupMode()) {
        response.sendRedirect("setup-completed.jsp");
        return;
    }
%>

<%! // Global vars

    static final String STANDARD = "standard";
    static final String JNDI = "jndi";
    static final String EMBEDDED = "embedded";

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

<%
    boolean embeddedMode = false;
    try {
        ClassUtils.forName("net.emiva.crossfire.core.starter.ServerStarter");
        embeddedMode = true;
    }
    catch (Exception ignored) {}

    // Get parameters
    String mode = ParamUtils.getParameter(request,"mode");
    boolean next = ParamUtils.getBooleanParameter(request,"next");

    // handle a mode redirect
    Map<String,String> errors = new HashMap<String,String>();
    if (next) {
        if (STANDARD.equals(mode)) {
            response.sendRedirect("setup-datasource-standard.jsp");
            return;
        }
        else if (JNDI.equals(mode)) {
            if (!embeddedMode) {
                response.sendRedirect("setup-datasource-jndi.jsp");
                return;
            }
        }
        else if (EMBEDDED.equals(mode)) {
            // Set the classname of the provider in the config file:
            Globals.setXMLProperty("connectionProvider.className",
                    "net.emiva.database.EmbeddedConnectionProvider");
            ConnectionProvider conProvider = new EmbeddedConnectionProvider();
            DbConnectionManager.setConnectionProvider(conProvider);
            if (testConnection(errors)) {
                // Redirect
                response.sendRedirect("setup-profile-settings.jsp");
                return;
            }
        }
    }

    // Defaults
    if (mode == null) {
        // If the "embedded-database" directory exists, select to the embedded db as the default.
        if (new File(Globals.getHomeDirectory(), "embedded-db").exists()) {
            mode = EMBEDDED;
        }
        // Otherwise default to standard.
        else {
            mode = STANDARD;
        }
    }
%>

<html>
<head>
    <title><fmt:message key="setup.datasource.settings.title" /></title>
    <meta name="currentStep" content="2"/>
</head>
<body>

	<h1>
	<fmt:message key="setup.datasource.settings.title" />
	</h1>

	<p>
	<fmt:message key="setup.datasource.settings.info">
	    <fmt:param value="<%= LocaleUtils.getLocalizedString("short.title") %>" />
	</fmt:message>
	</p>

<%  if (errors.size() > 0) { %>

    <p class="emiva-error-text">
    <%= errors.get("general") %>
    </p>

<%  } %>

	<!-- BEGIN emiva-contentBox -->
	<div class="emiva-contentBox">

		<form action="setup-datasource-settings.jsp">

<input type="hidden" name="next" value="true">

<table cellpadding="3" cellspacing="2" border="0">
<tr>
    <td align="center" valign="top">
        <input type="radio" name="mode" value="<%= STANDARD %>" id="rb02"
         <%= ((STANDARD.equals(mode)) ? "checked" : "") %>>
    </td>
    <td>
        <label for="rb02"><b><fmt:message key="setup.datasource.settings.connect" /></b></label>
        <br><fmt:message key="setup.datasource.settings.connect_info" />
    </td>
</tr>

<%  if (!embeddedMode) { %>

    <tr>
        <td align="center" valign="top">
            <input type="radio" name="mode" value="<%= JNDI %>" id="rb03"
             <%= ((JNDI.equals(mode)) ? "checked" : "") %>>
        </td>
        <td>
            <label for="rb03"><b><fmt:message key="setup.datasource.settings.jndi" /></b></label>
            <br><fmt:message key="setup.datasource.settings.jndi_info" />
        </td>
    </tr>

<%  } %>

<tr>
    <td align="center" valign="top">
        <input type="radio" name="mode" value="<%= EMBEDDED %>" id="rb01"
         <%= ((EMBEDDED.equals(mode)) ? "checked" : "") %>>
    </td>
    <td>
        <label for="rb01"><b><fmt:message key="setup.datasource.settings.embedded" /></b></label>
        <br><fmt:message key="setup.datasource.settings.embedded_info" />
    </td>
</tr>
</table>

<br><br>


		<div align="right">
			<input type="Submit" name="continue" value="<fmt:message key="global.continue" />" id="emiva-setup-save" border="0">
		</div>
	</form>

	</div>
	<!-- END emiva-contentBox -->


</body>
</html>