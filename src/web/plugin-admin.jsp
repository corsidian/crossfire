<%--
  - Copyright (C) 2005-2008 B5Chat Community. All rights reserved.
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
                 org.b5chat.crossfire.XmppServer,
                 org.b5chat.crossfire.core.container.Plugin,
                 org.b5chat.crossfire.core.container.PluginManager"
        %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Comparator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.io.*" %>
<%@ page import="org.b5chat.crossfire.core.util.Globals" %>
<%@ page import="org.b5chat.crossfire.core.util.Log" %>
<%@ page import="org.apache.commons.fileupload.FileItemFactory" %>
<%@ page import="org.apache.commons.fileupload.disk.DiskFileItemFactory" %>
<%@ page import="org.apache.commons.fileupload.servlet.ServletFileUpload" %>
<%@ page import="org.apache.commons.fileupload.FileItem" %>
<%@ page import="org.apache.commons.fileupload.FileUploadException" %>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<jsp:useBean id="webManager" class="org.b5chat.crossfire.core.util.WebManager"  />
<% webManager.init(request, response, session, application, out ); %>

<%
    String deletePlugin = ParamUtils.getParameter(request, "deleteplugin");
    String reloadPlugin = ParamUtils.getParameter(request, "reloadplugin");
    boolean showReadme = ParamUtils.getBooleanParameter(request, "showReadme", false);
    boolean showChangelog = ParamUtils.getBooleanParameter(request, "showChangelog", false);
    boolean uploadPlugin = request.getParameter("uploadplugin") != null;
    String url = request.getParameter("url");
    Boolean uploadEnabled = Globals.getBooleanProperty("plugins.upload.enabled", true);

    final PluginManager pluginManager = webManager.getXMPPServer().getPluginManager();

    List<Plugin> plugins = new ArrayList<Plugin>(pluginManager.getPlugins());

    if (plugins != null) {
        Collections.sort(plugins, new Comparator<Plugin>() {
            public int compare(Plugin p1, Plugin p2) {
                return pluginManager.getName(p1).compareTo(pluginManager.getName(p2));
            }
        });
    }

    if (deletePlugin != null) {
        File pluginDir = pluginManager.getPluginDirectory(pluginManager.getPlugin(deletePlugin));
        File pluginJar = new File(pluginDir.getParent(), pluginDir.getName() + ".jar");
        // Also try the .war extension.
        if (!pluginJar.exists()) {
            pluginJar = new File(pluginDir.getParent(), pluginDir.getName() + ".war");
        }
        pluginJar.delete();
        pluginManager.unloadPlugin(pluginDir.getName());
        // Log the event
        webManager.logEvent("deleted plugin "+deletePlugin, null);
        response.sendRedirect("plugin-admin.jsp?deletesuccess=true");
        return;
    }

    if (reloadPlugin != null) {
        for (Plugin plugin : plugins) {
            File pluginDir = pluginManager.getPluginDirectory(plugin);
            if (reloadPlugin.equals(pluginDir.getName())) {
                pluginManager.unloadPlugin(reloadPlugin);
                // Log the event
                webManager.logEvent("reloaded plugin "+reloadPlugin, null);
                response.sendRedirect("plugin-admin.jsp?reloadsuccess=true");
                return;
            }
        }
    }

    if (uploadEnabled && uploadPlugin) {
        Boolean installed = false;

        // Create a factory for disk-based file items
        FileItemFactory factory = new DiskFileItemFactory();

        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);

        try {
            // Parse the request
            List items = upload.parseRequest(request);

            for (Object objItem : items) {
                FileItem item = (FileItem)objItem;
                String fileName = item.getName();
                if (fileName != null) {
                    InputStream is = item.getInputStream();
                    if (is != null) {
                        installed = XMPPServer.getInstance().getPluginManager().installPlugin(is, fileName);
                        if (!installed) {
                            Log.error("Plugin manager failed to install plugin: " + fileName);
                        }
                        is.close();
                        // Log the event
                        webManager.logEvent("uploaded plugin "+fileName, null);
                    }
                    else {
                        Log.error("Unable to open file stream for uploaded file: " + fileName);
                    }
                }
                else {
                    Log.error("No filename specified for file upload.");
                }
            }
        }
        catch (FileUploadException e) {
            Log.error("Unable to upload plugin file.", e);
        }
        if (installed) {
            response.sendRedirect("plugin-admin.jsp?uploadsuccess=true");
            return;
        } else {
            response.sendRedirect("plugin-admin.jsp?uploadsuccess=false");
            return;
        }
    }
%>

<% if (showReadme) {
    String pluginName = ParamUtils.getParameter(request, "plugin");
    Plugin plugin = pluginManager.getPlugin(pluginName);
    if (plugin != null) {
        File readme = new File(pluginManager.getPluginDirectory(plugin), "readme.html");
        if (readme.exists()) {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(readme));
                String line;
                while ((line = in.readLine()) != null) {
%>
<%= line %>
<%
                    }
                }
                catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                finally {
                    if (in != null) {
                        try {
                            in.close();
                        }
                        catch (Exception e) {
                        }
                    }
                }
            }
        }
        return;
    }
%>
<% if (showChangelog) {
    String pluginName = ParamUtils.getParameter(request, "plugin");
    Plugin plugin = pluginManager.getPlugin(pluginName);
    if (plugin != null) {
        File changelog = new File(pluginManager.getPluginDirectory(plugin), "changelog.html");
        if (changelog.exists()) {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(changelog));
                String line;
                while ((line = in.readLine()) != null) {
%>
<%= line %>
<%
                    }
                }
                catch (IOException ioe) {

                }
                finally {
                    if (in != null) {
                        try {
                            in.close();
                        }
                        catch (Exception e) {
                        }
                    }
                }
            }
        }
        return;
    }
%>

<html>
<head>
<title><fmt:message key="plugin.admin.title"/></title>
<meta name="pageID" content="plugin-settings"/>
<meta name="helpPage" content="manage_system_plugins.html"/>
<script src="dwr/engine.js" type="text/javascript"></script>
<script src="dwr/util.js" type="text/javascript"></script>
<script src="dwr/interface/downloader.js" type="text/javascript"></script>

<script type="text/javascript" >
    DWREngine.setErrorHandler(handleError);

    function handleError(error) {
    }
</script>

<style type="text/css">

.textfield {
    font-size: 11px;
    font-family: verdana;
    padding: 3px 2px;
    background: #efefef;
}

.text {
    font-size: 11px;
    font-family: verdana, arial, helvetica, sans-serif;
}

.small-label {
    font-size: 11px;
    font-weight: bold;
    font-family: verdana, arial, helvetica, sans-serif;
}

.small-label-link {
    font-size: 11px;
    font-weight: bold;
    font-family: verdana;
    text-decoration: underline;
}

.light-gray-border {
    border-color: #ccc;
    border-style: solid;
    border-width: 1px 1px 1px 1px;
    padding: 5px;
	-moz-border-radius: 3px;
}

.light-gray-border-bottom {
    border-color: #dcdcdc;
    border-style: solid;
    border-width: 0px 0px 1px 0px;
}

.table-header {
    text-align: left;
    font-family: verdana, arial, helvetica, sans-serif;
    font-size: 8pt;
    font-weight: bold;
    border-color: #ccc;
    border-style: solid;
    border-width: 1px 0 1px 0;
    padding: 5px;
}

.table-header-left {
    text-align: left;
    font-family: verdana, arial, helvetica, sans-serif;
    font-size: 8pt;
    font-weight: bold;
    border-color: #ccc;
    border-style: solid;
    border-width: 1px 0 1px 1px;
    padding: 5px;

}

.table-header-right {
    text-align: left;
    font-family: verdana, arial, helvetica, sans-serif;
    font-size: 8pt;
    font-weight: bold;
    border-color: #ccc;
    border-style: solid;
    border-width: 1px 1px 1px 0;
    padding: 5px;
}

.table-font {
    font-family: verdana, arial, helvetica, sans-serif;
    font-size: 8pt;
}

.update {
    font-family: verdana, arial, helvetica, sans-serif;
    font-size: 8pt;
    background: #E7FBDE;
    border-color: #73CB73;
    border-style: solid;
    border-width: 0 1px 1px 1px;
    padding: 5px;
}

.update-bottom {
    text-align: left;
    font-family: verdana, arial, helvetica, sans-serif;
    font-size: 8pt;
    font-weight: bold;
    background: #E7FBDE;
    border-color: #73CB73;
    border-style: solid;
    border-width: 0 0 1px 0;
    padding: 5px;
}

.update-bottom-left {
    text-align: left;
    font-family: verdana, arial, helvetica, sans-serif;
    font-size: 8pt;
    font-weight: bold;
    background: #E7FBDE;
    border-color: #73CB73;
    border-style: solid;
    border-width: 0 0 1px 1px;
    padding: 5px;
}

.update-bottom-right {
    text-align: left;
    font-family: verdana, arial, helvetica, sans-serif;
    font-size: 8pt;
    font-weight: bold;
    background: #E7FBDE;
    border-color: #73CB73;
    border-style: solid;
    border-width: 0 1px 1px 0;
    padding: 5px;
}

.update-top {
    text-align: left;
    font-family: verdana, arial, helvetica, sans-serif;
    font-size: 9pt;
    background: #E7FBDE;
    border-color: #73CB73;
    border-style: solid;
    border-width: 1px 0px 0px 0px;
    padding: 5px;
}

.update-right {
    text-align: left;
    font-family: verdana, arial, helvetica, sans-serif;
    font-size: 8pt;
    font-weight: bold;
    background: #E7FBDE;
    border-color: #73CB73;
    border-style: solid;
    border-width: 1px 1px 0px 0px;
    padding: 5px;
}

.line-bottom-border {
    text-align: left;
    font-family: verdana, arial, helvetica, sans-serif;
    font-size: 9pt;
    border-color: #e3e3e3;
    border-style: solid;
    border-width: 0px 0px 1px 0px;
    padding: 5px;
}
</style>


<script type="text/javascript">
    function download(url, hashCode) {
        document.getElementById(hashCode + "-row").style.display = 'none';
        document.getElementById(hashCode + "-update").style.display = '';
        downloader.downloadPlugin(downloadComplete, url);
    }

    function downloadComplete(update) {
        document.getElementById(update.hashCode + "-row").style.display = 'none';
        document.getElementById(update.hashCode + "-update").style.display = '';
        document.getElementById(update.hashCode + "-image").innerHTML = '<img src="images/success-16x16.gif" border="0" alt=""/>';
        document.getElementById(update.hashCode + "-text").innerHTML = '<fmt:message key="plugin.admin.update.complete" />';
    }
</script>
</head>

<body>

<% if ("true".equals(request.getParameter("deletesuccess"))) { %>

<div class="success">
   <fmt:message key="plugin.admin.deleted_success"/>
</div>
<br>

<% }
else if ("false".equals(request.getParameter("deletesuccess"))) { %>

<div class="error">
    <fmt:message key="plugin.admin.deleted_failure"/>
</div>
<br>

<% } %>

<% if ("true".equals(request.getParameter("reloadsuccess"))) { %>

<div class="success">
   <fmt:message key="plugin.admin.reload_success"/>
</div>
<br>

<% } %>

<% if ("true".equals(request.getParameter("uploadsuccess"))) { %>

<div class="success">
   <fmt:message key="plugin.admin.uploaded_success"/>
</div>
<br>

<% }
else if ("false".equals(request.getParameter("uploadsuccess"))) { %>

<div class="error">
    <fmt:message key="plugin.admin.uploaded_failure"/>
</div>
<br>

<% } %>

<p>
    <fmt:message key="plugin.admin.info"/>
</p>

<p>

<div class="light-gray-border" style="padding:10px;">
<table cellpadding="0" cellspacing="0" border="0" width="100%">
 <tr style="background:#eee;">

    <td nowrap colspan="3" class="table-header-left"><fmt:message key="plugin.admin.name"/></td>
    <td nowrap class="table-header"><fmt:message key="plugin.admin.description"/></td>
    <td nowrap class="table-header"><fmt:message key="plugin.admin.version"/></td>
    <td nowrap class="table-header"><fmt:message key="plugin.admin.author"/></td>
    <td nowrap class="table-header"><fmt:message key="plugin.admin.restart"/></td>
    <td nowrap class="table-header-right"><fmt:message key="global.delete"/></td>
</tr>

<tbody>


<%
    // If only the admin plugin is installed, show "none".
    if (plugins.size() == 1) {
%>
<tr>
    <td align="center" colspan="8" style="padding:5px;"><fmt:message key="plugin.admin.no_plugin"/></td>
</tr>
<%
    }

    int count = 0;
    for (Plugin plugin : plugins) {
        String dirName = pluginManager.getPluginDirectory(plugin).getName();
        // Skip the admin plugin.
        if (!"admin".equals(dirName)) {
            count++;
            String pluginName = pluginManager.getName(plugin);
            String pluginDescription = pluginManager.getDescription(plugin);
            String pluginAuthor = pluginManager.getAuthor(plugin);
            String pluginVersion = pluginManager.getVersion(plugin);
            File pluginDir = pluginManager.getPluginDirectory(plugin);
            File icon = new File(pluginDir, "logo_small.png");
            if (!icon.exists()) {
                icon = new File(pluginDir, "logo_small.gif");
            }
%>

<tr valign="top">
    <td width="1%" class="line-bottom-border">
        <% if (icon.exists()) { %>
        <img src="geticon?plugin=<%= URLEncoder.encode(pluginDir.getName(), "utf-8") %>&showIcon=true&decorator=none" width="16" height="16" alt="Plugin">
        <% }
        else { %>
        <img src="images/plugin-16x16.gif" width="16" height="16" alt="Plugin">
        <% } %>
    </td>
    <td width="20%" nowrap valign="top" class="line-bottom-border">
        <%= (pluginName != null ? pluginName : dirName) %> &nbsp;
        <%

            boolean readmeExists = new File(pluginDir, "readme.html").exists();
            boolean changelogExists = new File(pluginDir, "changelog.html").exists();
        %>


    </td>
    <td nowrap valign="top" class="line-bottom-border">
        <p><% if (readmeExists) { %>
            <a href="plugin-admin.jsp?plugin=<%= URLEncoder.encode(pluginDir.getName(), "utf-8") %>&showReadme=true&decorator=none"
                    ><img src="images/doc-readme-16x16.gif" width="16" height="16" border="0" alt="README"></a>
            <% }
            else { %> &nbsp; <% } %>
            <% if (changelogExists) { %>
            <a href="plugin-admin.jsp?plugin=<%= URLEncoder.encode(pluginDir.getName(), "utf-8") %>&showChangelog=true&decorator=none"
                    ><img src="images/doc-changelog-16x16.gif" width="16" height="16" border="0" alt="changelog"></a>
            <% }
            else { %> &nbsp; <% } %></p>
    </td>
    <td width="60%" valign="top" class="line-bottom-border">
        <%= pluginDescription != null ? pluginDescription : "" %>
    </td>
    <td width="5%" align="center" valign="top" class="line-bottom-border">
        <p><%= pluginVersion != null ? pluginVersion : "" %></p>

    </td>
    <td width="15%" nowrap valign="top" class="line-bottom-border">
        <%= pluginAuthor != null ? pluginAuthor : "" %>  &nbsp;
    </td>
    <td width="1%" align="center" valign="top" class="line-bottom-border">
        <a href="plugin-admin.jsp?reloadplugin=<%= dirName %>"
           title="<fmt:message key="plugin.admin.click_reload" />"
                ><img src="images/refresh-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key="global.refresh" />"></a>
    </td>
    <td width="1%" align="center" valign="top" class="line-bottom-border">
        <a href="#" onclick="if (confirm('<fmt:message key="plugin.admin.confirm" />')) { location.replace('plugin-admin.jsp?deleteplugin=<%= dirName %>'); } "
           title="<fmt:message key="global.click_delete" />"
                ><img src="images/delete-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key="global.delete" />"></a>
    </td>
</tr>

<tr><td></td></tr>

<!-- End of update section -->
<%
        }
    }
%>
</tbody>
</table>
</div>

<% if (uploadEnabled) { %>
<br /><br />

<div>
    <h3><fmt:message key="plugin.admin.upload_plugin" /></h3>
    <p><fmt:message key="plugin.admin.upload_plugin.info" /></p>
    <form action="plugin-admin.jsp?uploadplugin" enctype="multipart/form-data" method="post">
        <input type="file" name="uploadfile" />
        <input type="submit" value="<fmt:message key="plugin.admin.upload_plugin" />" />
    </form>
</div>
<% } %>

</body>
</html>