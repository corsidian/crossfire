<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%--
  -	$RCSfile$
  -	$Revision: 1772 $
  -	$Date: 2005-08-11 12:56:15 -0700 (Thu, 11 Aug 2005) $
--%>

<%@ page import="org.b5chat.crossfire.database.DbConnectionManager,
                 org.b5chat.crossfire.database.DefaultConnectionProvider,
                 org.b5chat.crossfire.core.util.ClassUtils,
                 org.b5chat.crossfire.core.util.Globals,
                 org.b5chat.crossfire.core.util.Log,
                 org.b5chat.crossfire.core.util.ParamUtils,
                 org.b5chat.crossfire.XmppServer,
                 java.io.File,
                 java.lang.Double,
                 java.lang.Exception,
                 java.lang.Integer,
                 java.lang.String"
%>
<%@ page import="java.lang.Throwable"%>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>

<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<%
	// Redirect if we've already run setup:
	if (!XMPPServer.getInstance().isSetupMode()) {
        response.sendRedirect("setup-completed.jsp");
        return;
    }
%>

<%!
    boolean testConnection(Map<String,String> errors) {
        boolean success = true;
        Connection con = null;
        try {
            con = DbConnectionManager.getConnection();
            if (con == null) {
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
        catch (SQLException ex) {
            success = false;
            errors.put("general","A connection to the database could not be "
                + "made. View the error message by opening the "
                + "\"" + File.separator + "logs" + File.separator + "error.log\" log "
                + "file, then go back to fix the problem.");

        }
        finally {
            try {
        	    con.close();
            } catch (Exception ignored) {}
        }
        return success;
    }
%>


<%  // Get parameters
    String driver = ParamUtils.getParameter(request,"driver");
    String serverURL = ParamUtils.getParameter(request,"serverURL");
    String username = ParamUtils.getParameter(request,"username",true);
    String password = ParamUtils.getParameter(request,"password",true);
    int minConnections = ParamUtils.getIntParameter(request,"minConnections",-1);
    int maxConnections = ParamUtils.getIntParameter(request,"maxConnections",-1);
    double connectionTimeout = ParamUtils.getDoubleParameter(request,"connectionTimeout",0.0);

    boolean doContinue = request.getParameter("continue") != null;

    // handle a continue request
    Map<String,String> errors = new HashMap<String,String>();
    if (doContinue) {
        // Error check
        if (driver == null || "sun.jdbc.odbc.JdbcOdbcDriver".equals(driver)
                || "com.internetcds.jdbc.tds.Driver".equals(driver))
        {
            errors.put("driver","Please enter a valid JDBC driver class.");
        }
        else {
            try {
                ClassUtils.forName(driver);
            }
            catch (Throwable t) {
                errors.put("driver","Unable to load the specified JDBC driver. Please verify the " +
                        "name of the driver is correct and that the driver is in the classpath " +
                        "of this server (usually the 'lib' directory). If you add a driver to " +
                        "your classpath you will neeed to restart the server.");
            }
        }
        if (serverURL == null) {
            errors.put("serverURL", "Please enter a valid JDBC URL.");
        }
        if (minConnections < 3) {
            errors.put("minConnections","The minimum connection pool size is three connections.");
        }
        if (maxConnections < minConnections) {
            errors.put("maxConnections","The maximum number of connections cannot be less than the minimum.");
        }
        if (connectionTimeout <= 0.0) {
            errors.put("connectionTimeout","Please enter a valid connection timeout value.");
        }

        // if there were no errors, continue
        if (errors.size() == 0) {
            // set properties, test connection, etc

            // Force the standard b5chat connection provider to be used by deleting the current setting:
            Globals.setXMLProperty("connectionProvider.className",
                    "org.b5chat.crossfire.database.DefaultConnectionProvider");
            DefaultConnectionProvider conProvider = new DefaultConnectionProvider();
            try {
                conProvider.setDriver(driver);
                conProvider.setConnectionTimeout(connectionTimeout);
                conProvider.setMinConnections(minConnections);
                conProvider.setMaxConnections(maxConnections);
                conProvider.setServerURL(serverURL);
                conProvider.setUsername(username);
                conProvider.setPassword(password);
                conProvider.setTestSQL(DbConnectionManager.getTestSQL(driver));

                Globals.setXMLProperty("database.defaultProvider.driver", driver);
                Globals.setXMLProperty("database.defaultProvider.serverURL", serverURL);
                Globals.setXMLProperty("database.defaultProvider.username", username);
                Globals.setXMLProperty("database.defaultProvider.password", password);
                Globals.setXMLProperty("database.defaultProvider.testSQL", DbConnectionManager.getTestSQL(driver));

                Globals.setXMLProperty("database.defaultProvider.minConnections",
                        Integer.toString(minConnections));
                Globals.setXMLProperty("database.defaultProvider.maxConnections",
                        Integer.toString(maxConnections));
                Globals.setXMLProperty("database.defaultProvider.connectionTimeout",
                Double.toString(connectionTimeout));
            }
            catch (Exception e) {
                errors.put("general","Setting connection properties failed - please see the error "
                        + "log located in home/logs for more details.");
                Log.error(e);
            }
            // No errors setting the properties, so test the connection
            DbConnectionManager.setConnectionProvider(conProvider);
            if (testConnection(errors)) {
                // Success, move on
                response.sendRedirect("setup-profile-settings.jsp");
                return;
            }
        }
    }

    if (!doContinue) {
        // reset values of jdbc driver from props file
        driver = Globals.getXMLProperty("database.defaultProvider.driver");
        serverURL = Globals.getXMLProperty("database.defaultProvider.serverURL");
        username = Globals.getXMLProperty("database.defaultProvider.username");
        password = Globals.getXMLProperty("database.defaultProvider.password");
        try {
            minConnections = Integer.parseInt(
                    Globals.getXMLProperty("database.defaultProvider.minConnections"));
        }
        catch (Exception e) {
            minConnections = 5;
        }
        try {
            maxConnections = Integer.parseInt(
                    Globals.getXMLProperty("database.defaultProvider.maxConnections"));
        }
        catch (Exception e) {
            maxConnections = 25;
        }
        try {
            connectionTimeout = Double.parseDouble(
                    Globals.getXMLProperty("database.defaultProvider.connectionTimeout"));
        }
        catch (Exception e) {
            connectionTimeout = 1.0;
        }
    }
%>

<html>
<head>
    <title><fmt:message key="setup.datasource.standard.title" /></title>
    <meta name="currentStep" content="2"/>
</head>
<body>

	<h1>
	<fmt:message key="setup.datasource.standard.title" />
	</h1>

	<p>
	<fmt:message key="setup.datasource.standard.info" /> <fmt:message key="title" />.
	</p>

	<p>
	<b><fmt:message key="setup.datasource.standard.info2" /> </b><fmt:message key="setup.datasource.standard.info3" /> <tt>[crossfire_HOME]/resources/database</tt>.
	</p>

<%  if (errors.size() > 0) { %>
    <div class="error">
    <%  if (errors.get("general") != null) { %>

        <%= errors.get("general") %>

    <%  } else { %>

        <fmt:message key="setup.datasource.standard.failed_connect" />

    <%  } %>
    </div>
<%  } %>



	<!-- BEGIN b5chat-contentBox -->
	<div class="b5chat-contentBox">


<%  // DB preset data
    List<String[]> presets = new ArrayList<String []>();
    presets.add(new String[]{"MySQL","com.mysql.jdbc.Driver","jdbc:mysql://[host-name]:3306/[database-name]"});
    presets.add(new String[]{"Oracle","oracle.jdbc.driver.OracleDriver","jdbc:oracle:thin:@[host-name]:1521:[SID]"});
    presets.add(new String[]{"Microsoft SQLServer","net.sourceforge.jtds.jdbc.Driver","jdbc:jtds:sqlserver://[host-name]/[database-name];appName=b5chat"});
    presets.add(new String[]{"PostgreSQL","org.postgresql.Driver","jdbc:postgresql://[host-name]:5432/[database-name]"});
    presets.add(new String[]{"IBM DB2","com.ibm.db2.jcc.DB2Driver","jdbc:db2://[host]:50000/[database-name]"});
%>
<script language="JavaScript" type="text/javascript">
var data = new Array();
<%  for (int i=0; i<presets.size(); i++) {
        String[] data = presets.get(i);
%>
    data[<%= i %>] = new Array('<%= data[0] %>','<%= data[1] %>','<%= data[2] %>');
<%  } %>
function populate(i) {
    document.dbform.driver.value=data[i][1];
    document.dbform.serverURL.value=data[i][2];
}
var submitted = false;
function checkSubmit() {
    if (!submitted) {
        submitted = true;
        return true;
    }
    return false;
}
</script>

<form action="setup-datasource-standard.jsp" method="post" name="dbform" onsubmit="return checkSubmit();">

<table cellpadding="3" cellspacing="2" border="0">
<tr>
	<td nowrap align="right"><fmt:message key="setup.datasource.standard.label" />:</td>
    <td>
        <select size="1" name="presets" onchange="populate(this.options[this.selectedIndex].value)">
            <option value=""><fmt:message key="setup.datasource.standard.pick_database" />
            <%  for (int i=0; i<presets.size(); i++) {
                    String[] data = presets.get(i);
            %>
                <option value="<%= i %>"> &#149; <%= data[0] %>
            <%  } %>
        </select>
    </td>
</tr>
<tr valign="top">
    <td nowrap align="right">
        <fmt:message key="setup.datasource.standard.jdbc" />
    </td>
    <td>
        <input type="text" name="driver" size="50" maxlength="150"
         value="<%= ((driver != null) ? driver : "") %>">
        <span class="b5chat-setup-helpicon" onmouseover="domTT_activate(this, event, 'content', '<fmt:message key="setup.datasource.standard.jdbc_info" />', 'styleClass', 'b5chatTooltip', 'trail', true, 'delay', 300, 'lifetime', 8000);"></span>
        <%  if (errors.get("driver") != null) { %>
            <span class="b5chat-error-text">
            <%= errors.get("driver") %>
            </span>
        <%  } %>
    </td>
</tr>
<tr valign="top">
    <td nowrap align="right">
        <fmt:message key="setup.datasource.standard.url" />
    </td>
    <td>
        <input type="text" name="serverURL" size="50" maxlength="250"
         value="<%= ((serverURL != null) ? serverURL : "") %>">
	    <span class="b5chat-setup-helpicon" onmouseover="domTT_activate(this, event, 'content', '<fmt:message key="setup.datasource.standard.valid_url" />', 'styleClass', 'b5chatTooltip', 'trail', true, 'delay', 300, 'lifetime', 8000);"></span>
        <%  if (errors.get("serverURL") != null) { %>
            <span class="b5chat-error-text">
            <%= errors.get("serverURL") %>
            </span>
        <%  } %>
    </td>
</tr>
<tr><td colspan="2">&nbsp;</td></tr>
<tr valign="top">
    <td nowrap align="right">
        <fmt:message key="setup.datasource.standard.username" />
    </td>
    <td>
        <input type="text" name="username" size="20" maxlength="50"
         value="<%= ((username != null) ? username : "") %>">
        <span class="b5chat-setup-helpicon" onmouseover="domTT_activate(this, event, 'content', '<fmt:message key="setup.datasource.standard.username_info" />', 'styleClass', 'b5chatTooltip', 'trail', true, 'delay', 300, 'lifetime', 8000);"></span>
        <%  if (errors.get("username") != null) { %>
            <span class="b5chat-error-text">
            <%= errors.get("username") %>
            </span>
        <%  } %>
    </td>
</tr>
<tr valign="top">
    <td nowrap align="right">
        <fmt:message key="setup.datasource.standard.password" />
    </td>
    <td>
        <input type="password" name="password" size="20" maxlength="50"
         value="<%= ((password != null) ? password : "") %>">
        <span class="b5chat-setup-helpicon" onmouseover="domTT_activate(this, event, 'content', '<fmt:message key="setup.datasource.standard.password_info" />', 'styleClass', 'b5chatTooltip', 'trail', true, 'delay', 300, 'lifetime', 8000);"></span>
        <%  if (errors.get("password") != null) { %>
            <span class="b5chat-error-text">
            <%= errors.get("password") %>
            </span>
        <%  } %>
    </td>
</tr>
<tr><td colspan="2">&nbsp;</td></tr>
<tr valign="top">
    <td nowrap align="right">
        <%--<fmt:message key="setup.datasource.standard.connect" />--%>
        Minimum Connections:
    </td>
    <td>
	    <input type="text" name="minConnections" size="5" maxlength="5" value="<%= ((minConnections != -1) ? ""+minConnections : "") %>">
        <span class="b5chat-setup-helpicon" onmouseover="domTT_activate(this, event, 'content', '<fmt:message key="setup.datasource.standard.pool" />', 'styleClass', 'b5chatTooltip', 'trail', true, 'delay', 300, 'lifetime', 8000);"></span>
        <%  if (errors.get("minConnections") != null) { %>
            <span class="b5chat-error-text">
            <%= errors.get("minConnections") %>
            </span>
        <%  } %>
    </td>
</tr>
<tr valign="top">
    <td nowrap align="right">
        <%--<fmt:message key="setup.datasource.standard.connect" />--%>
        Maximum Connections:
    </td>
    <td>
	    <input type="text" name="maxConnections" size="5" maxlength="5" value="<%= ((maxConnections != -1) ? ""+maxConnections : "") %>">
        <span class="b5chat-setup-helpicon" onmouseover="domTT_activate(this, event, 'content', '<fmt:message key="setup.datasource.standard.pool" />', 'styleClass', 'b5chatTooltip', 'trail', true, 'delay', 300, 'lifetime', 8000);"></span>
        <%  if (errors.get("maxConnections") != null) { %>
            <span class="b5chat-error-text">
            <%= errors.get("maxConnections") %>
            </span>
        <%  } %>
    </td>
</tr>
<tr valign="top">
    <td nowrap align="right">
        <fmt:message key="setup.datasource.standard.timeout" />
    </td>
    <td>
        <input type="text" name="connectionTimeout" size="5" maxlength="5"
         value="<%= connectionTimeout %>"> <span style="display: block; float: left; padding: 2px 5px 0px 2px;">Days</span>
        <span class="b5chat-setup-helpicon" onmouseover="domTT_activate(this, event, 'content', '<fmt:message key="setup.datasource.standard.timeout_info" />', 'styleClass', 'b5chatTooltip', 'trail', true, 'delay', 300, 'lifetime', 8000);"></span>
        <%  if (errors.get("connectionTimeout") != null) { %>
            <span class="b5chat-error-text">
            <%= errors.get("connectionTimeout") %>
            </span>
        <%  } %>
    </td>
</tr>
</table>

<br>

		<div align="right"><div class="b5chat-description" style="padding-bottom:10px;">
			<fmt:message key="setup.datasource.standard.note" /></div>
			<input type="Submit" name="continue" value="<fmt:message key="global.continue" />" id="b5chat-setup-save" border="0">
		</div>
	</form>

	</div>
	<!-- END b5chat-contentBox -->


</body>
</html>