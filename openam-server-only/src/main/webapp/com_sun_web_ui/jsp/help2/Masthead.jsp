<%--
/**
 * ident "@(#)Masthead.jsp 1.8 04/08/23 SMI"
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 */
--%>
<%@page language="java" %>
<%@page import="com.sun.web.ui.common.CCI18N" %>
<%@page import="org.owasp.esapi.ESAPI" %>
<%@taglib uri="/WEB-INF/tld/com_iplanet_jato/jato.tld" prefix="jato"%>
<%@taglib uri="/WEB-INF/tld/com_sun_web_ui/cc.tld" prefix="cc"%>

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
    String helpLogoHeight = getRequestParameter(request, "helpLogoHeight", "");
%>

<jato:useViewBean className="com.sun.web.ui.servlet.help2.MastheadViewBean">

<!-- Header -->
<cc:header
 name="Header"
 pageTitle="<%=windowTitle %>"
 styleClass="HlpMstTtlBdy"
 baseName="com.sun.web.ui.resources.Resources"
 bundleID="help2Bundle">

<cc:form name="mastheadForm" method="post">

<!-- Secondary Masthead -->
<cc:secondarymasthead
 name="Masthead"
 src="<%=mastheadTitle %>"
 alt="<%=mastheadAlt %>"
 bundleID="help2Bundle"
 width="<%=helpLogoWidth %>"
 height="<%=helpLogoHeight %>" />

<!-- Page Title -->
<cc:pagetitle name="PageTitle" bundleID="help2Bundle"
 pageTitleText="<%=pageTitle %>"
 showPageTitleSeparator="true"
 showPageButtonsTop="true"
 showPageButtonsBottom="false" />

</cc:form>
</cc:header>
</jato:useViewBean>
