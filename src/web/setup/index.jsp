<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>

<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<%--
  -	$RCSfile$
  -	$Revision: 2873 $
  -	$Date: 2005-09-23 10:54:57 -0700 (Fri, 23 Sep 2005) $
--%>

<%@ page import="org.b5chat.util.Globals,
                 org.b5chat.util.ParamUtils,
                 org.b5chat.crossfire.XMPPServer" %>
<%@ page import="java.util.HashMap"%>
<%@ page import="java.util.Locale"%>
<%@ page import="java.util.Map"%>

<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<%! // Global vars, methods, etc

    static final String emiva_HOME = "emiva_home";
    static final String emiva_LICENSE = "emiva_license_file";
    static final String emiva_LICENSE_TEXT = "emiva_license_text";
    static final String emiva_DEPENDENCY = "emiva_dependency";
    static final String emiva_CONFIG_FILE = "emiva_config_file";
%>

<%
	// Redirect if we've already run setup:
	if (!XMPPServer.getInstance().isSetupMode()) {
        response.sendRedirect("setup-completed.jsp");
        return;
    }
%>

<%@ include file="setup-env-check.jspf" %>

<%  // Get parameters
    String localeCode = ParamUtils.getParameter(request,"localeCode");
    boolean save = request.getParameter("save") != null;

    Map errors = new HashMap();

    if (save) {
        Locale newLocale = null;
        if (localeCode != null) {
            newLocale = LocaleUtils.localeCodeToLocale(localeCode.trim());
            if (newLocale == null) {
                errors.put("localeCode","");
            }
            else {
                Globals.setLocale(newLocale);
                // redirect
                response.sendRedirect("setup-host-settings.jsp");
                return;
            }
        }
    }

    Locale locale = Globals.getLocale();
%>

<html>
<head>
<title><fmt:message key="setup.index.title" /></title>
<meta name="currentStep" content="0"/>
</head>
<body>


	<h1>
	<fmt:message key="setup.index.title" />
	</h1>

	<p>
	<fmt:message key="setup.index.info">
		<fmt:param value="<%= LocaleUtils.getLocalizedString("title") %>" />
	</fmt:message>
	</p>


	<!-- BEGIN emiva-contentBox -->
	<div class="emiva-contentBox">

	<h2><fmt:message key="setup.index.choose_lang" /></h2>

	<form action="index.jsp" name="sform">
<%  boolean usingPreset = false;
    Locale[] locales = Locale.getAvailableLocales();
    for (int i=0; i<locales.length; i++) {
        usingPreset = locales[i].equals(locale);
        if (usingPreset) { break; }
    }
%>
		<div id="emiva-setup-language">
			<p>
			<label for="loc01">
			<input type="radio" name="localeCode" value="cs_CZ" <%= ("cs_CZ".equals(locale.toString()) ? "checked" : "") %> id="loc01" />
			<b>Czech</b> (cs_CZ)
			</label><br>

			<label for="loc02">
			<input type="radio" name="localeCode" value="de" <%= ("de".equals(locale.toString()) ? "checked" : "") %> id="loc02" />
			<b>Deutsch</b> (de)
			</label><br>

			<label for="loc03">
			<input type="radio" name="localeCode" value="en" <%= ("en".equals(locale.toString()) ? "checked" : "") %> id="loc03" />
			<b>English</b> (en)
			</label><br>

			<label for="loc04">
			<input type="radio" name="localeCode" value="es" <%= ("es".equals(locale.toString()) ? "checked" : "") %> id="loc04" />
			<b>Espa&ntilde;ol</b> (es)
			</label><br>

			<label for="loc05">
			<input type="radio" name="localeCode" value="fr" <%= ("fr".equals(locale.toString()) ? "checked" : "") %> id="loc05" />
			<b>Fran&ccedil;ais</b> (fr)
			</label><br>

			<label for="loc06">
			<input type="radio" name="localeCode" value="nl" <%= ("nl".equals(locale.toString()) ? "checked" : "") %> id="loc06" />
			<b>Nederlands</b> (nl)
			</label><br>

			<label for="loc07">
			<input type="radio" name="localeCode" value="pl_PL" <%= ("pl_PL".equals(locale.toString()) ? "checked" : "") %> id="loc07" />
			<b>Polski</b> (pl_PL)
			</label><br>

			<label for="loc08">
			<input type="radio" name="localeCode" value="pt_BR" <%= ("pt_BR".equals(locale.toString()) ? "checked" : "") %> id="loc08" />
			<b>Portugu&ecirc;s Brasileiro</b> (pt_BR)
			</label><br>

			<label for="loc09">
			<input type="radio" name="localeCode" value="ru_RU" <%= ("ru_RU".equals(locale.toString()) ? "checked" : "") %>  id="loc09" />
            <b>&#x420;&#x443;&#x441;&#x441;&#x43A;&#x438;&#x439;</b> (ru_RU)
			</label><br>

			<label for="loc10">
			<input type="radio" name="localeCode" value="sk" <%= ("sk".equals(locale.toString()) ? "checked" : "") %> id="loc10" />
			<b>Sloven&#269;ina</b> (sk)
			</label><br>

			<label for="loc11">
			<input type="radio" name="localeCode" value="zh_CN" <%= ("zh_CN".equals(locale.toString()) ? "checked" : "") %> id="loc11" />
            <img src="../images/setup_language_zh_CN.gif" border="0" align="top" />
            <b>Simplified Chinese</b> (zh_CN)
			</label><br>
			</p>
		</div>

		<div align="right">
			<input type="Submit" name="save" value="<fmt:message key="global.continue" />" id="emiva-setup-save" border="0">
		</div>
	</form>

	</div>
	<!-- END emiva-contentBox -->


</body>
</html>
