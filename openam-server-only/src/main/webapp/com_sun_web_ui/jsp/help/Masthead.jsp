<%--
/**
 * ident "@(#)Masthead.jsp 1.18 04/08/23 SMI"
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 */
--%>
<%@ page language="java" %>
<%@page import="com.sun.web.ui.common.CCI18N" %>
<%@page import="org.owasp.esapi.ESAPI" %>
<%@taglib uri="/WEB-INF/tld/com_iplanet_jato/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/tld/com_sun_web_ui/cc.tld" prefix="cc" %>

<%!
    private String getRequestParameter(ServletRequest request, String name, String defaultValue) {
        String value = request.getParameter(name);
        if (value == null || !ESAPI.validator().isValidInput("HTTP Parameter Value: " + value,
                value, "HTTPParameterValue", 2000, false)) {
            return defaultValue;
        }
        return value;
    }
%>

<%
    // Get query parameters.
    String windowTitle = getRequestParameter(request, "windowTitle", "");
    String mastheadTitle = getRequestParameter(request, "mastheadTitle", "");
    String mastheadAlt = getRequestParameter(request, "mastheadAlt", "");
    String pageTitle = getRequestParameter(request, "pageTitle", "help.pageTitle");
    String helpLogoWidth = getRequestParameter(request, "helpLogoWidth", "");
    String helpLogoHeight= getRequestParameter(request, "helpLogoHeight", "");
    String showCloseButton = getRequestParameter(request, "showCloseButton", "true");

    // Set default value for close button.
    if (!(showCloseButton.equalsIgnoreCase("false")))
	showCloseButton = "true";
%>

<jato:useViewBean className="com.sun.web.ui.servlet.help.MastheadViewBean">

<!-- Header -->
<cc:header name="Header"
 pageTitle="<%=windowTitle %>"
 copyrightYear="2004"
 baseName="com.sun.web.ui.resources.Resources"
 bundleID="helpBundle">


<form>
<!-- Secondary Masthead -->
<div class="HlpMst">
<cc:secondarymasthead name="Masthead" src="<%=mastheadTitle %>" alt="<%=mastheadAlt %>" bundleID="helpBundle" width="<%=helpLogoWidth %>" height="<%=helpLogoHeight %>" />
</div>

<!-- Page Title -->
<div class="HlpTtl">
<cc:pagetitle name="PageTitle" bundleID="helpBundle"
 pageTitleText="<%=pageTitle %>"
 showPageTitleSeparator="true"
 showPageButtonsTop="<%=showCloseButton %>"
 showPageButtonsBottom="false" />
</div>

</form>

</cc:header>
</jato:useViewBean>
