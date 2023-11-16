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
 * Copyright 2023 Wren Security
 */
package org.wrensecurity.wrenam.authentication.modules.duo;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import java.util.Base64;
import org.junit.jupiter.api.Test;

@WireMockTest
public class DuoClientTest {

    //~ Common test parameters
    private static final String INTEGRATION_KEY = "intg";
    private static final String USERNAME = "user1";
    private static final String TRANSACTION_ID = "transaction-id1";

    @Test
    public void testInitAuth(WireMockRuntimeInfo runtimeInfo) {
        String baseUrl = "/auth/v2/auth";
        // Stub HTTP endpoint
        stubFor(post(baseUrl).willReturn(ok().withBodyFile("duo-endpoint-auth-init-response.json")));
        // Perform HTTP request
        DuoClient client = new DuoClient(new DuoClientConfig(runtimeInfo.getHttpBaseUrl(), INTEGRATION_KEY, "sec"));
        // Verify performed request
        assertEquals(TRANSACTION_ID, client.initAuth(USERNAME));
        verify(1, postRequestedFor(urlEqualTo(baseUrl))
                .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                .withHeader("Date", matching(".*"))
                .withRequestBody(containing("username=" + USERNAME))
                .withRequestBody(containing("async=true"))
                .andMatching(request -> checkAuthorizationHeader(request)));

    }

    @Test
    public void testAuthStatus(WireMockRuntimeInfo runtimeInfo) throws Exception {
        String baseUrl = "/auth/v2/auth_status";
        // Stub HTTP endpoint
        stubFor(get(urlPathEqualTo(baseUrl)).willReturn(ok().withBodyFile("duo-endpoint-auth-status-response.json")));
        // Perform HTTP request
        DuoClient client = new DuoClient(new DuoClientConfig(runtimeInfo.getHttpBaseUrl(), INTEGRATION_KEY, "sec"));
        // Verify performed request
        assertEquals(DuoAuthStatus.ALLOW, client.getAuthStatus(TRANSACTION_ID).get());
        verify(1, getRequestedFor(urlPathEqualTo(baseUrl))
                .withHeader("Date", matching(".*"))
                .withQueryParam("txid", equalTo(TRANSACTION_ID))
                .andMatching(request -> checkAuthorizationHeader(request)));

    }

    /**
     * Check authorization header of the specified request.
     */
    private MatchResult checkAuthorizationHeader(Request request) {
        if (!request.containsHeader("Authorization")) {
            MatchResult.noMatch();
        }
        String value = request.getHeader("Authorization");
        if (!value.startsWith("Basic ")) {
            MatchResult.noMatch();
        }
        String credentials = new String(Base64.getDecoder().decode(value.substring(6)));
        return MatchResult.of(credentials.startsWith(INTEGRATION_KEY));
    }

}
