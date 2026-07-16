/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: CookieUtils.java,v 1.6 2009/10/02 00:08:26 ericow Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 * Portions Copyrighted 2025-2026 Wren Security
 */

package com.sun.identity.shared.encode;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Implements utility methods for handling Cookie.
 */
public class CookieUtils {

    static final String SECURE_COOKIE_PREFIX = "__Secure-";

    static final String HOST_COOKIE_PREFIX = "__Host-";

    static boolean secureCookie =
        (SystemPropertiesManager.get(Constants.AM_COOKIE_SECURE) != null) &&
        (SystemPropertiesManager.get(Constants.AM_COOKIE_SECURE).
            equalsIgnoreCase("true"));

    static boolean cookieHttpOnly =
        (SystemPropertiesManager.get(Constants.AM_COOKIE_HTTPONLY) != null) &&
        (SystemPropertiesManager.get(Constants.AM_COOKIE_HTTPONLY).
            equalsIgnoreCase("true"));

    static boolean cookieEncoding =
        (SystemPropertiesManager.get(Constants.AM_COOKIE_ENCODE) != null) &&
        (SystemPropertiesManager.get(Constants.AM_COOKIE_ENCODE)
            .equalsIgnoreCase("true"));

    static String amCookieName = SystemPropertiesManager.get(
        Constants.AM_COOKIE_NAME);
    static String amPCookieName = SystemPropertiesManager.get(
        Constants.AM_PCOOKIE_NAME);
    static String cdssoCookiedomain = SystemPropertiesManager.get(
        Constants.SERVICES_CDSSO_COOKIE_DOMAIN);
    static String fedCookieName = SystemPropertiesManager.get(
        Constants.FEDERATION_FED_COOKIE_NAME);

    private static Set<String> cookieDomains = null;

    private static int defAge = -1;

    static Debug debug = Debug.getInstance("amCookieUtils");

    /**
     * Gets property value of "com.iplanet.am.cookie.name"
     *
     * @return the property value of "com.iplanet.am.cookie.name"
     */
    public static String getAmCookieName() {
        return amCookieName;
    }

    /**
     * Get name of a HTTP header that can be used as substitute value holder for a cookie
     * with the given name.
     *
     * @param cookieName name of the cookie
     * @return suitable HTTP header name
     */
    public static String getCookieHeaderName(String cookieName) {
        String lowercaseName = cookieName.toLowerCase();
        if (lowercaseName.startsWith(SECURE_COOKIE_PREFIX.toLowerCase())) {
            return cookieName.substring(SECURE_COOKIE_PREFIX.length());
        }
        if (lowercaseName.startsWith(HOST_COOKIE_PREFIX.toLowerCase())) {
            return cookieName.substring(HOST_COOKIE_PREFIX.length());
        }
        return cookieName;
    }

    /**
     * Returns property value of "com.iplanet.am.pcookie.name"
     *
     * @return the property value of "com.iplanet.am.pcookie.name"
     */
    public static String getAmPCookieName() {
        return amPCookieName;
    }

    /**
     * Returns property value of "com.iplanet.services.cdsso.cookiedomain"
     *
     * @return the property value of "com.iplanet.services.cdsso.cookiedomain"
     */
    public static Set<String> getCdssoCookiedomain() {
        if (cookieDomains != null) {
            return cookieDomains;
        }

        Set<String> cookieDomains = new HashSet<>();
        if (cdssoCookiedomain == null || cdssoCookiedomain.length() < 1) {
            return Collections.emptySet();
        }

        StringTokenizer st = new StringTokenizer(cdssoCookiedomain, ",");
        while (st.hasMoreTokens()) {
            String token = st.nextToken().trim();
            if (token.length() > 0) {
                cookieDomains.add(token);
            }
        }

        return cookieDomains.isEmpty() ? Collections.emptySet() : cookieDomains;
    }

    /**
     * Returns property value of "com.sun.identity.federation.fedCookieName"
     *
     * @return the property value of "com.sun.identity.federation.fedCookieName"
     */
    public static String getFedCookieName() {
        return fedCookieName;
    }

    /**
     * Returns property value of "com.iplanet.am.cookie.secure"
     *
     * @return the property value of "com.iplanet.am.cookie.secure"
     */
    public static boolean isCookieSecure() {
        return secureCookie;
    }

    /**
     * Returns property value of "com.sun.identity.cookie.httponly"
     *
     * @return the property value of "com.sun.identity.cookie.httponly"
     */
    public static boolean isCookieHttpOnly() {
        return cookieHttpOnly;
    }

    /**
     * Returns value of cookie that has mached name in servlet request
     *
     * @param req HTTP Servlet Request.
     * @param name Name in servlet request
     * @return value of that name of cookie
     */
    public static String getCookieValueFromReq(
        HttpServletRequest req,
        String name
    ) {
        String cookieValue = null;
        Cookie cookie = getCookieFromReq(req, name);
        if (cookie != null) {
            cookieValue = getCookieValue(cookie);
        } else {
            debug.message("No Cookie is in the request");
        }
        return cookieValue;
    }

    /**
     * Gets cookie object that has mached name in servlet request
     *
     * @param req HTTP Servlet Request.
     * @param name Name in servlet request
     * @return value of that name of cookie
     */
    public static Cookie getCookieFromReq(HttpServletRequest req, String name) {
        Cookie cookies[] = req.getCookies();
        if (cookies != null) {
            for (int nCookie = 0; nCookie < cookies.length; nCookie++) {
                if (cookies[nCookie].getName().equalsIgnoreCase(name)) {
                    return cookies[nCookie];
                }
            }
        }
        return null;
    }

    /**
     * Returns normalized value of cookie
     *
     * @param cookie Cookie object.
     * @return normalized value of cookie.
     */
    public static String getCookieValue(Cookie cookie) {
        String cookieValue = checkDoubleQuote(cookie.getValue());

        // Check property value and it decode value
        // Bea, IBM
        if (cookieValue != null && cookieEncoding) {
            return URLEncDec.decode(cookieValue);
        }
        return cookieValue;
    }

    /**
     * Gets Array of cookie in servlet request.
     *
     * @param req HTTP Servlet Request.
     */
    public static Cookie[] getCookieArrayFromReq(HttpServletRequest req) {
        Cookie cookies[] = req.getCookies();

        if (!cookieEncoding) {
            return cookies;
        }

        if (cookies != null) {
            for (int nCookie = 0; nCookie < cookies.length; nCookie++) {
                String cookieValue = checkDoubleQuote(cookies[nCookie]
                        .getValue());
                if (cookieValue != null) {
                    cookies[nCookie].setValue(URLEncDec.decode(cookieValue));
                }
            }
        }
        return cookies;
    }

    /**
     * This method creates Map from the name values of cookies
     * present in the given <code>HttpServletRequest</code>
     *
     * @param request reference to <code>HttpServletRequest</code>
     * @return Map containing name value pairs from cookies present
     */
    public static Map<String, String> getRequestCookies(HttpServletRequest request) {
        Map<String, String> cookieMap = new HashMap<String, String>();
        if (request != null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null && cookies.length > 0) {
                for (Cookie nextCookie : cookies) {
                    String name = nextCookie.getName();
                    String value = nextCookie.getValue();
                    cookieMap.put(name, value);
                }
            }
        }
        return cookieMap;
    }

    /**
     * Returns a cookie with a specified name and value.
     *
     * @param name Name of the cookie.
     *
     * @param value Value of the cookie.
     *
     * @return constructed cookie.
     */
    public static Cookie newCookie(String name, String value) {
        return newCookie(name, value, defAge, null, null);
    }

    /**
     * Returns a cookie with a specified name and value and sets the maximum
     * age of the cookie in seconds.
     *
     * @param name Name of the cookie
     * @param value Value of the cookie
     * @param maxAge Maximum age of the cookie in seconds; if negative, means
     *        the cookie is not stored; if zero, deletes the cookie.
     * @return constructed cookie
     */
    public static Cookie newCookie(String name, String value, int maxAge) {
        return newCookie(name, value, maxAge, null, null);
    }

    /**
     * Returns a cookie with a specified name and value and sets a path for
     * the cookie to which the client should return the cookie.
     *
     * @param name Name of the cookie
     * @param value Value of the cookie
     * @param path Path
     * @return constructed cookie
     */
    public static Cookie newCookie(String name, String value, String path) {
        return newCookie(name, value, defAge, path, null);
    }

    /**
     * Returns a cookie with a specified name and value and sets a path for
     * the cookie to which the client should return the cookie and sets the
     * domain within which this cookie should be presented.
     *
     * @param name Name of the cookie
     * @param value Value of the cookie
     * @param path Path
     * @param domain Domain name within which this cookie is visible; form is
     *        according to <code>RFC 2109</code>
     * @return constructed cookie
     */
    public static Cookie newCookie(
        String name,
        String value,
        String path,
        String domain
    ) {
        return newCookie(name, value, defAge, path, domain);
    }

    /**
     * Returns a cookie with a specified name and value and sets the maximum
     * age of the cookie in seconds and sets a path for the cookie to which the
     * client should return the cookie and sets the domain within which this
     * cookie should be presented.
     *
     * @param name Name of the cookie
     * @param value Value of the cookie
     * @param maxAge Maximum age of the cookie in seconds; if negative, means
     *        the cookie is not stored; if zero, deletes the cookie.
     * @param path Path
     * @param domain Domain name within which this cookie is visible; form is
     *        according to <code>RFC 2109</code>
     * @return constructed cookie
     */
    public static Cookie newCookie(
        String name,
        String value,
        int maxAge,
        String path,
        String domain
    ) {
        Cookie cookie = null;

        // Based on property value it does url encoding.
        // BEA, IBM
        if (cookieEncoding && value != null) {
            cookie = new Cookie(name, URLEncDec.encode(value));
        } else {
            cookie = new Cookie(name, value);
        }

        cookie.setMaxAge(maxAge);

        if ((path != null) && (path.length() > 0)) {
            cookie.setPath(path);
        } else {
            cookie.setPath("/");
        }

        if (domain != null && domain.length() > 0) {
            // Ignore domain when the name has __Host- prefix
            if (!name.toLowerCase().startsWith(HOST_COOKIE_PREFIX.toLowerCase())) {
                cookie.setDomain(domain);
            }
        }

        cookie.setHttpOnly(isCookieHttpOnly());
        cookie.setSecure(isCookieSecure());
        return cookie;
    }

    /**
     * Check cookie value whether it has double quote or not. Remove start /
     * ending double quote from cookie and returns cookie value only.
     *
     * @param cookie Value of the Cookie
     * @return cookie value without double quote
     */
    public static String checkDoubleQuote(String cookie) {
        String double_quote = "\"";
        if ((cookie != null) && cookie.startsWith(double_quote)
                && cookie.endsWith(double_quote)) {
            int last = cookie.length() - 1;
            cookie = cookie.substring(1, last);
        }
        return cookie;
    }

    /**
     * Add cookie to {@link HttpServletResponse}.
     *
     * @param response HTTP response to update. Never null.
     * @param cookie Cookie to add. Never null.
     */
    public static void addCookieToResponse(HttpServletResponse response, Cookie cookie) {
        addCookieToResponse(response, cookie, false);
    }

    /**
     * Add cookie to {@link HttpServletResponse}.
     *
     * @param response HTTP response to update. Can be null.
     * @param cookie Cookie to add. Never null.
     * @param crossDomain Whether to set the SameSite cookie attribute to <code>None</code>.
     */
    public static void addCookieToResponse(HttpServletResponse response, Cookie cookie, boolean crossDomain) {
        if (cookie == null) {
            return; // This can unfortunately happen (see AuthClientUtils)
        }

        String cookieString = renderSetCookieValue(cookie, crossDomain);
        if (debug.messageEnabled()) {
            debug.message("CookieUtils:addCookieToResponse adds " + cookieString);
        }
        response.addHeader("Set-Cookie", cookieString);
    }

    /**
     * Render <code>Set-Cookie</code> header valut.
     *
     * @param cookie Cookie to render. Never null.
     * @param crossDomain Whether to set the SameSite cookie attribute to <code>None</code>.
     * @return Rendered <code>Set-Cookie</code> header value.
     */
    public static String renderSetCookieValue(Cookie cookie, boolean crossDomain) {
        StringBuilder sb = new StringBuilder(150)
                .append(cookie.getName()).append("=").append(cookie.getValue());

        String path = cookie.getPath();
        if (path != null && path.length() > 0) {
            sb.append("; Path=").append(path);
        } else {
            sb.append("; Path=/");
        }

        String domain = cookie.getDomain();
        if (domain != null && domain.length() > 0) {
            sb.append("; Domain=").append(domain);
        }

        int age = cookie.getMaxAge();
        if (age > -1) {
            sb.append("; Max-Age=").append(age);
            // Support for Expires as < IE 8 does not support max-age
            ZonedDateTime expires = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(age);
            sb.append("; Expires=").append(DateTimeFormatter.RFC_1123_DATE_TIME.format(expires));
        }

        if (cookie.isHttpOnly()) {
            sb.append("; HttpOnly");
        }

        if (cookie.getSecure()) {
            sb.append("; Secure");
            if (crossDomain) {
                sb.append("; SameSite=None");
            }
        }

        return sb.toString();
    }

    /**
     * Matches the provided cookie domains against the current request's domain and returns the resulting set of
     * matching cookie domains.
     *
     * @param request The HTTP request.
     * @param cookieDomains The configured cookie domains to match against.
     * @return The set of matching cookie domains. May contain null.
     */
    public static Set<String> getMatchingCookieDomains(HttpServletRequest request, Collection<String> cookieDomains) {
        String host = normalizeDomain(request.getServerName());
        Set<String> domains = new HashSet<>();

        for (String domain : cookieDomains) {
            if (domain == null || domain.isEmpty()) {
                domains.add(domain);
                continue;
            }

            String normalizedDomain = normalizeDomain(domain);
            if (host != null && (host.equals(normalizedDomain) || host.endsWith("." + normalizedDomain))) {
                domains.add(domain);
            }
        }
        return domains;
    }

    private static String normalizeDomain(String domain) {
        if (domain == null) {
            return null;
        }

        String normalized = domain.toLowerCase(Locale.ROOT);
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
