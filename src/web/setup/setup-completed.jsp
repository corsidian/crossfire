<%--
  -	$Revision: 985 $
  -	$Date: 2005-02-18 10:35:44 -0800 (Fri, 18 Feb 2005) $
--%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<html>
<head>
<title><fmt:message key="short.title" /> <fmt:message key="setup.completed.setup" /></title>
<link rel="stylesheet" type="text/css" href="../style/setup.css">
</head>

<body style="background-image: none;">



<!-- BEGIN B5Chat-header -->
<div id="B5Chat-header">
	<div id="B5Chat-logo" title="crossfire"></div>
	<div id="B5Chat-header-text"><fmt:message key="setup.title" /></div>
</div>
<!-- END B5Chat-header -->

<!-- BEGIN B5Chat-body -->
<div id="B5Chat-body" style="left: 0px; padding: 30px 50px 30px 50px;">


<table cellpadding="0" cellspacing="0" border="0" width="100%">
<tr valign="top">

    <td width="98%">

        <h1>
        <fmt:message key="setup.completed.run" />
        </h1>

        <p>
        <fmt:message key="setup.completed.run_info" />
        <a href="index.jsp"><fmt:message key="short.title" /> <fmt:message key="setup.completed.run_info1" /></a>.
        <fmt:message key="setup.completed.run_info2" />
        </p>

	    <br><br>
		    <div class="B5Chat_setup_launchAdmin" align="center">
			    <a href="../index.jsp"><fmt:message key="setup.finished.login" /></a>
		    </div>


    </td>
</tr>
</table>
</div>
<!-- END B5Chat-body -->



<!-- BEGIN B5Chat-footer -->
<div id="B5Chat-footer"></div>
<!-- END B5Chat-footer -->






</body>
</html>