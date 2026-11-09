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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2026 Wren Security.
 */
package org.forgerock.oauth2.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.oauth2.OAuth2Constants.AuthorizationScopes.AM_INTROSPECT_ALL_TOKENS;
import static org.forgerock.openam.oauth2.OAuth2Constants.IntrospectionEndpoint.ACCESS_TOKEN_TYPE;
import static org.forgerock.openam.oauth2.OAuth2Constants.IntrospectionEndpoint.REFRESH_TOKEN_TYPE;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Collections;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.OAuth2UrisFactory;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit test for {@link OAuth2TokenIntrospectionHandler}.
 *
 * @since 14.0.0
 */
public final class OAuth2TokenIntrospectionHandlerTest {

    private OAuth2TokenIntrospectionHandler handler;

    @Mock
    private TokenStore tokenStore;
    @Mock
    private OAuth2UrisFactory uriFactory;
    @Mock
    private OAuth2Request request;
    @Mock
    private AccessToken token;
    @Mock
    private OAuth2Uris oAuth2Uris;
    @Mock
    private ClientRegistration client;


    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        handler = new OAuth2TokenIntrospectionHandler(tokenStore, uriFactory);
        given(client.getClientId()).willReturn("resource-server");
        given(client.isConfidential()).willReturn(true);
    }

    @Test
    public void whenCnfIsPresentItIsReturned() throws ServerException, NotFoundException, InvalidGrantException {
        // Given
        given(tokenStore.readAccessToken(request, "abc-def-hij")).willReturn(token);
        given(token.isExpired()).willReturn(false);
        given(token.getClientId()).willReturn("some-client-id");
        given(client.getClientId()).willReturn("some-client-id");
        given(token.getRealm()).willReturn("/abc");
        given(request.getParameter("realm")).willReturn("/abc");
        given(token.getScope()).willReturn(Collections.singleton("open"));
        given(uriFactory.get(request)).willReturn(oAuth2Uris);

        JsonValue confirmationKey = json(object(field("jwk", object())));
        given(token.getConfirmationKey()).willReturn(confirmationKey);

        // When
        JsonValue json = handler.introspect(request, client, ACCESS_TOKEN_TYPE, "abc-def-hij");

        // Then
        assertThat(json.get("cnf").get("jwk").isMap()).isTrue();
    }

    @Test
    public void whenCnfIsNotPresentItIsNotReturned() throws ServerException, NotFoundException, InvalidGrantException {
        // Given
        given(tokenStore.readAccessToken(request, "abc-def-hij")).willReturn(token);
        given(token.isExpired()).willReturn(false);
        given(token.getClientId()).willReturn("some-client-id");
        given(client.getClientId()).willReturn("some-client-id");
        given(token.getRealm()).willReturn("/abc");
        given(request.getParameter("realm")).willReturn("/abc");
        given(token.getScope()).willReturn(Collections.singleton("open"));
        given(uriFactory.get(request)).willReturn(oAuth2Uris);

        JsonValue confirmationKey = json(null);
        given(token.getConfirmationKey()).willReturn(confirmationKey);

        // When
        JsonValue json = handler.introspect(request, client, ACCESS_TOKEN_TYPE, "abc-def-hij");

        // Then
        assertThat(json.isDefined("/cnf")).isFalse();
    }

    @Test(dataProvider = "tokenTypes")
    public void shouldIntrospectOwnTokenWithoutSpecialScope(String tokenType) throws Exception {
        prepareToken(tokenType);
        given(client.getClientId()).willReturn("original-client");
        given(client.isConfidential()).willReturn(false);

        JsonValue result = handler.introspect(request, client, tokenType, "target-token");

        assertThat(result.get("active").asBoolean()).isTrue();
    }

    @Test(dataProvider = "tokenTypes")
    public void shouldIntrospectOtherClientsTokenWithAllowedScope(String tokenType) throws Exception {
        prepareToken(tokenType);
        allowCrossClientIntrospection();

        JsonValue result = handler.introspect(request, client, tokenType, "target-token");

        assertThat(result.get("active").asBoolean()).isTrue();
    }

    @Test(dataProvider = "tokenTypes")
    public void shouldRejectOtherClientsTokenWithoutScope(String tokenType) throws Exception {
        prepareToken(tokenType);
        given(client.getAllowedScopes()).willReturn(Collections.singleton("profile"));

        assertThat(handler.introspect(request, client, tokenType, "target-token")).isNull();
    }

    @Test
    public void shouldRejectOtherClientsTokenWithScopeOnlyInDefaultScopes() throws Exception {
        prepareToken(ACCESS_TOKEN_TYPE);
        given(client.getAllowedScopes()).willReturn(Collections.singleton("profile"));
        given(client.getDefaultScopes()).willReturn(Collections.singleton(AM_INTROSPECT_ALL_TOKENS));

        assertThat(handler.introspect(request, client, ACCESS_TOKEN_TYPE, "target-token")).isNull();
    }

    @Test
    public void shouldRejectOtherClientsTokenWithScopeOnlyInToken() throws Exception {
        IntrospectableToken target = prepareToken(ACCESS_TOKEN_TYPE);
        given(client.getAllowedScopes()).willReturn(Collections.singleton("profile"));
        given(target.getScope()).willReturn(Collections.singleton(AM_INTROSPECT_ALL_TOKENS));

        assertThat(handler.introspect(request, client, ACCESS_TOKEN_TYPE, "target-token")).isNull();
    }

    @Test
    public void shouldRejectPublicClientWithSpecialScope() throws Exception {
        prepareToken(ACCESS_TOKEN_TYPE);
        allowCrossClientIntrospection();
        given(client.isConfidential()).willReturn(false);

        assertThat(handler.introspect(request, client, ACCESS_TOKEN_TYPE, "target-token")).isNull();
    }

    @Test
    public void shouldRejectAnotherRealmEvenWithSpecialScope() throws Exception {
        IntrospectableToken target = prepareToken(ACCESS_TOKEN_TYPE);
        allowCrossClientIntrospection();
        given(target.getRealm()).willReturn("/other");

        assertThat(handler.introspect(request, client, ACCESS_TOKEN_TYPE, "target-token")).isNull();
        given(client.getClientId()).willReturn("original-client");
        assertThat(handler.introspect(request, client, ACCESS_TOKEN_TYPE, "target-token")).isNull();
    }

    @Test
    public void shouldRejectExpiredTokenEvenWithSpecialScope() throws Exception {
        IntrospectableToken target = prepareToken(ACCESS_TOKEN_TYPE);
        allowCrossClientIntrospection();
        given(target.isExpired()).willReturn(true);

        assertThat(handler.introspect(request, client, ACCESS_TOKEN_TYPE, "target-token")).isNull();
    }

    @DataProvider
    private Object[][] tokenTypes() {
        return new Object[][] {{ACCESS_TOKEN_TYPE}, {REFRESH_TOKEN_TYPE}};
    }

    private IntrospectableToken prepareToken(String tokenType) throws Exception {
        IntrospectableToken target;
        if (ACCESS_TOKEN_TYPE.equals(tokenType)) {
            target = token;
            given(tokenStore.readAccessToken(request, "target-token")).willReturn(token);
            given(token.getConfirmationKey()).willReturn(json(null));
        } else {
            RefreshToken refreshToken = mock(RefreshToken.class);
            target = refreshToken;
            given(tokenStore.readRefreshToken(request, "target-token")).willReturn(refreshToken);
        }
        given(target.getClientId()).willReturn("original-client");
        given(target.getRealm()).willReturn("/abc");
        given(target.getScope()).willReturn(Collections.singleton("profile"));
        given(target.getResourceOwnerId()).willReturn("user-id");
        given(target.getExpiryTime()).willReturn(1800000000000L);
        given(request.getParameter("realm")).willReturn("/abc");
        given(uriFactory.get(request)).willReturn(oAuth2Uris);
        given(oAuth2Uris.getIssuer()).willReturn("https://am.example.com/oauth2/abc");
        return target;
    }

    private void allowCrossClientIntrospection() {
        given(client.getAllowedScopes()).willReturn(Collections.singleton(AM_INTROSPECT_ALL_TOKENS));
    }

}
