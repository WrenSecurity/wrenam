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
package org.forgerock.oauth2.core;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.oauth2.OAuth2Constants.IntrospectionEndpoint.ACCESS_TOKEN_TYPE;
import static org.forgerock.openam.oauth2.OAuth2Constants.IntrospectionEndpoint.TOKEN;
import static org.forgerock.openam.oauth2.OAuth2Constants.IntrospectionEndpoint.TOKEN_TYPE_HINT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.openam.oauth2.OAuth2UrisFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TokenIntrospectionServiceTest {

    private static final String ENDPOINT = "https://am.example.com/am/oauth2/realms/root/realms/alpha/introspect";

    private ClientAuthenticator authenticator;
    private ClientRegistration client;
    private OAuth2Request request;
    private TokenIntrospectionHandler handler;
    private OAuth2UrisFactory urisFactory;
    private TokenIntrospectionService service;

    @BeforeMethod
    void setUp() throws Exception {
        request = mock(OAuth2Request.class);
        given(request.getParameter(TOKEN)).willReturn("target-token");
        given(request.getParameter(TOKEN_TYPE_HINT)).willReturn(ACCESS_TOKEN_TYPE);

        authenticator = mock(ClientAuthenticator.class);
        client = mock(ClientRegistration.class);
        given(authenticator.authenticate(request, ENDPOINT)).willReturn(client);
        urisFactory = mock(OAuth2UrisFactory.class);
        OAuth2Uris uris = mock(OAuth2Uris.class);
        given(urisFactory.get(request)).willReturn(uris);
        given(uris.getIntrospectionEndpoint()).willReturn(ENDPOINT);

        handler = mock(TokenIntrospectionHandler.class);
        service = new TokenIntrospectionService(authenticator, singleton(handler), urisFactory);
    }

    /**
     * Checks that the handler receives the authenticated client rather than the registration from
     * the request.
     */
    @Test
    void shouldUseRegistrationReturnedByAuthentication() throws Exception {
        given(request.getClientRegistration()).willReturn(mock(ClientRegistration.class));
        JsonValue response = json(object(field("active", true)));
        given(handler.introspect(request, client, ACCESS_TOKEN_TYPE, "target-token"))
                .willReturn(response);

        assertThat(service.introspect(request)).isSameAs(response);

        verify(authenticator).authenticate(request, ENDPOINT);
        verify(handler).introspect(request, client, ACCESS_TOKEN_TYPE, "target-token");
    }

    @Test
    void shouldNotInvokeHandlersWhenAuthenticationFails() throws Exception {
        InvalidClientException failure = mock(InvalidClientException.class);
        given(authenticator.authenticate(request, ENDPOINT)).willThrow(failure);

        assertThatThrownBy(() -> service.introspect(request)).isSameAs(failure);
        verify(handler, never()).introspect(any(), any(), any(), any());
    }
}
