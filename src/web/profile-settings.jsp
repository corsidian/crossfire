<%--
  -	$RCSfile$
  -	$Revision: $
  -	$Date: $
  -
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

<%@ page import="org.b5chat.crossfire.core.util.Globals" %>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<html>
    <head>
        <title><fmt:message key="profile-settings.title"/></title>
        <meta name="pageID" content="profile-settings"/>
    </head>
    <body>
    <p>
    <fmt:message key="profile-settings.info"/>
    </p>

    <form action="profile-settings.jsp" method="post">
        <!--<div class="b5chat-contentBoxHeader">

        </div>-->
        <div class="b5chat-contentBox" style="-moz-border-radius: 3px;">
            <table cellpadding="3" cellspacing="3" border="0">
            <tbody>
                <tr>
                    <td width="1%" nowrap>
                        <input type="radio" readonly
                        checked>
                    </td>
                    <td width="99%">
                        <b><fmt:message key="setup.profile.default" /></b> - <fmt:message key="setup.profile.default_description" />
                    </td>
                </tr>
            </tbody>
            </table>
        </div>
    </form>

</body>
</html>

