/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.1.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.1.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2026 Wren Security
 */

package com.sun.identity.shared.encode;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Set;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CookieUtilsTest {

    @DataProvider(name = "matchingCookieDomains")
    public Object[][] matchingCookieDomains() {
        return new Object[][] {
                // host, cookie domain
                { "example.com", "example.com" },
                { "login.example.com", "example.com" },
                { "LOGIN.EXAMPLE.COM", "Example.Com" },
                { "example.com", ".example.com" },
                { "login.example.com", ".EXAMPLE.COM" },
                { "192.0.2.1", "192.0.2.1" },
                { "example.com.", "example.com" },
                { "login.example.com", "example.com." }
        };
    }

    @Test(dataProvider = "matchingCookieDomains")
    public void shouldMatchValidCookieDomains(String host, String domain) {
        Set<String> matchingDomains = CookieUtils.getMatchingCookieDomains(requestForHost(host), Set.of(domain));
        assertEquals(matchingDomains, Set.of(domain));
    }

    @DataProvider(name = "nonMatchingCookieDomains")
    public Object[][] nonMatchingCookieDomains() {
        return new Object[][] {
                // host, cookie domain
                { "notexample.com", "example.com" },
                { "foo-example.com", "example.com" },
                { "example.com.attacker.test", "example.com" },
                { "example.test", "example.com" },
                { "login.example.com", "invalid domain" }
        };
    }

    @Test(dataProvider = "nonMatchingCookieDomains")
    public void shouldRejectInvalidCookieDomains(String host, String domain) {
        Set<String> matchingDomains = CookieUtils.getMatchingCookieDomains(requestForHost(host), Set.of(domain));
        assertTrue(matchingDomains.isEmpty());
    }

    @Test
    public void shouldAlwaysIncludeHostOnlyCookieDomains() {
        Set<String> matchingDomains = CookieUtils.getMatchingCookieDomains(requestForHost("example.com"), Collections.singleton(null));
        assertEquals(matchingDomains, Collections.singleton(null));
    }

    @Test
    public void shouldPreserveEmptyDomainAsHostOnlyCookieDomain() {
        Set<String> matchingDomains = CookieUtils.getMatchingCookieDomains(requestForHost("example.com"), Set.of(""));
        assertEquals(matchingDomains, Set.of(""));
    }

    @Test
    public void shouldAlwaysIncludeHostOnlyCookieDomainsWithoutServerName() {
        Set<String> matchingDomains = CookieUtils.getMatchingCookieDomains(requestForHost(null), Collections.singleton(null));
        assertEquals(matchingDomains, Collections.singleton(null));
    }

    @Test
    public void shouldRejectCookieDomainsWithoutServerName() {
        Set<String> matchingDomains = CookieUtils.getMatchingCookieDomains(requestForHost(null), Set.of("example.com"));
        assertTrue(matchingDomains.isEmpty());
    }

    private HttpServletRequest requestForHost(String host) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServerName()).thenReturn(host);
        return request;
    }

    @DataProvider(name = "cookieHeaderNames")
    public Object[][] cookieHeaderNames() {
        return new Object[][] {
                // cookie name, header name
                { "iPlanetDirectoryPro", "iPlanetDirectoryPro" },
                { "__Host-iPlanetDirectoryPro", "iPlanetDirectoryPro" },
                { "__HOST-iPlanetDirectoryPro", "iPlanetDirectoryPro" },
                { "__Secure-iPlanetDirectoryPro", "iPlanetDirectoryPro" },
                { "__secure-iPlanetDirectoryPro", "iPlanetDirectoryPro" },
        };
    }

    @Test(dataProvider = "cookieHeaderNames")
    public void shouldGetCookieHeaderName(String cookieName, String headerName) {
        assertEquals(CookieUtils.getCookieHeaderName(cookieName), headerName);
    }

    @DataProvider(name = "responseCookies")
    public Object[][] responseCookies() {
        return new Object[][] {
            { CookieUtils.newCookie("foo", "bar"), "foo=bar; Path=/" },
            { CookieUtils.newCookie("foo", "bar", "/hello", "example.org"), "foo=bar; Path=/hello; Domain=example.org" },
            { CookieUtils.newCookie("__Host-foo", "bar", "/", "example.org"), "__Host-foo=bar; Path=/" }
        };
    }

    @Test(dataProvider = "responseCookies")
    public void shouldAddCookieToResponse(Cookie cookie, String expected) {
        HttpServletResponse response = mock(HttpServletResponse.class);
        CookieUtils.addCookieToResponse(response, cookie);
        verify(response, only()).addHeader("Set-Cookie", expected);
    }

}
