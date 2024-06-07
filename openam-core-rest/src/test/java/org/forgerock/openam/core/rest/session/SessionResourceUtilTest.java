/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2024 Wren Security.
 */

package org.forgerock.openam.core.rest.session;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.util.Collections;
import java.util.List;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.http.HttpContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SessionResourceUtilTest {

    private HttpContext context;
    private Request request;

    @BeforeMethod
    public void setUp() {
        context = mock(HttpContext.class);
        request = mock(Request.class);
    }

    @Test
    public void testGetTokenIdFromPath() {
        when(request.getResourcePath()).thenReturn("tokenIdFromPath");

        String tokenId = SessionResourceUtil.getTokenId(context, request);

        assertEquals(tokenId, "tokenIdFromPath");
    }

    @Test
    public void testGetTokenIdFromUrlParam() {
        when(request.getAdditionalParameter("tokenId")).thenReturn("tokenIdFromUrlParam");

        String tokenId = SessionResourceUtil.getTokenId(context, request);

        assertEquals(tokenId, "tokenIdFromUrlParam");
    }

    @Test
    public void testGetTokenIdFromCookie() {
        when(context.getHeader("cookie")).thenReturn(List.of("iPlanetDirectoryPro=tokenIdFromCookie"));

        String tokenId = SessionResourceUtil.getTokenId(context, request);

        assertEquals(tokenId, "tokenIdFromCookie");
    }

    @Test
    public void testGetTokenIdFromHeader() {
        when(context.getHeader("iPlanetDirectoryPro")).thenReturn(Collections.singletonList("tokenIdFromHeader"));

        String tokenId = SessionResourceUtil.getTokenId(context, request);

        assertEquals(tokenId, "tokenIdFromHeader");
    }

    @Test
    public void testGetTokenIdNotFound() {
        String tokenId = SessionResourceUtil.getTokenId(context, request);

        assertNull(tokenId);
    }

    @Test(dataProvider = "tokenDataProvider")
    public void testGetTokenIdPreferredSource(String pathToken, String urlParamToken, String cookieToken, String headerToken, String expectedToken) {
        when(request.getResourcePath()).thenReturn(pathToken);
        when(request.getAdditionalParameter("tokenId")).thenReturn(urlParamToken);
        when(context.getHeader("cookie")).thenReturn(Collections.singletonList("iPlanetDirectoryPro=" + cookieToken));
        when(context.getHeader("iPlanetDirectoryPro")).thenReturn(Collections.singletonList(headerToken));

        String tokenId = SessionResourceUtil.getTokenId(context, request);

        assertEquals(tokenId, expectedToken);
    }

    @DataProvider(name = "tokenDataProvider")
    public String[][] tokenDataProvider() {
        return new String[][] {
                {"tokenIdFromPath", "tokenIdFromUrlParam", "tokenIdFromCookie", "tokenIdFromHeader", "tokenIdFromPath"},
                {"", "tokenIdFromUrlParam", "tokenIdFromCookie", "tokenIdFromHeader", "tokenIdFromUrlParam"},
                {"", "", "tokenIdFromCookie", "tokenIdFromHeader", "tokenIdFromCookie"},
                {"", "", "", "tokenIdFromHeader", "tokenIdFromHeader"}
        };
    }
}
