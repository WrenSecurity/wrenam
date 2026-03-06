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
 * Copyright 2026 Wren Security.
 */
package org.forgerock.openidconnect;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.openam.oauth2.OAuth2Constants.JWTTokenParams.LEGACY_OPS;
import static org.forgerock.openam.oauth2.OAuth2Constants.JWTTokenParams.OPS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.guice.core.InjectorConfiguration;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.adapters.TokenAdapter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.utils.OpenAMSettings;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class CheckSessionTest extends GuiceTestCase {

    private static final String CLIENT_ID = "test-client";
    private static final String CLIENT_SECRET = "test-secret";
    private static final String CLIENT_SESSION_URI = "https://client.example.com";

    private SSOTokenManager ssoTokenManager;
    private ClientRegistrationStore clientRegistrationStore;
    private CTSPersistentStore cts;
    private TokenAdapter<JsonValue> tokenAdapter;

    @BeforeClass
    public static void setupGuice() {
        InjectorConfiguration.setGuiceModuleLoader(clazz -> new HashSet<>());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void configure(Binder binder) {
        ssoTokenManager = mock(SSOTokenManager.class);
        clientRegistrationStore = mock(ClientRegistrationStore.class);
        cts = mock(CTSPersistentStore.class);
        tokenAdapter = mock(TokenAdapter.class);

        binder.bind(SSOTokenManager.class).toInstance(ssoTokenManager);
        binder.bind(OpenAMSettings.class).toInstance(mock(OpenAMSettings.class));
        binder.bind(SigningManager.class).toInstance(new SigningManager());
        binder.bind(ClientRegistrationStore.class).toInstance(clientRegistrationStore);
        binder.bind(CTSPersistentStore.class).toInstance(cts);
        binder.bind(Key.get(new TypeLiteral<TokenAdapter<JsonValue>>() { },
                Names.named(OAuth2Constants.CoreTokenParams.OAUTH_TOKEN_ADAPTER)))
                .toInstance(tokenAdapter);
    }

    private ClientRegistration mockClientRegistration() throws Exception {
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        given(clientRegistration.getClientSecret()).willReturn(CLIENT_SECRET);
        given(clientRegistrationStore.get(eq(CLIENT_ID), any(OAuth2Request.class))).willReturn(clientRegistration);
        return clientRegistration;
    }

    private HttpServletRequest createRequestWithIdToken(String clientId, String secret) {
        return createRequestWithIdToken(clientId, secret, null);
    }

    private HttpServletRequest createRequestWithIdToken(String clientId, String secret, String opsId) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(request.getHeader("Referer")).willReturn("https://example.com/page?id_token=" + buildSignedJwt(clientId, secret, opsId));
        return request;
    }

    private String buildSignedJwt(String clientId, String secret, String opsId) {
        SigningHandler signingHandler = new SigningManager()
                .newHmacSigningHandler(secret.getBytes(StandardCharsets.UTF_8));
        JwtBuilderFactory factory = new JwtBuilderFactory();
        var claimsBuilder = factory.claims();
        if (clientId != null) {
            claimsBuilder.aud(Collections.singletonList(clientId));
        }
        if (opsId != null) {
            claimsBuilder.claim(OPS, opsId);
        }
        return factory.jws(signingHandler)
                .headers().alg(JwsAlgorithm.HS256).done()
                .claims(claimsBuilder.build())
                .build();
    }

    @Test
    public void getClientSessionURI_validJwt_returnsClientURI() throws Exception {
        ClientRegistration clientRegistration = mockClientRegistration();
        given(clientRegistration.getClientSessionURI()).willReturn(CLIENT_SESSION_URI);
        assertThat(new CheckSession().getClientSessionURI(createRequestWithIdToken(CLIENT_ID, CLIENT_SECRET)))
                .isEqualTo(CLIENT_SESSION_URI);
    }

    @Test
    public void getClientSessionURI_invalidSignature_returnsEmpty() throws Exception {
        mockClientRegistration();
        assertThat(new CheckSession().getClientSessionURI(createRequestWithIdToken(CLIENT_ID, "wrong-secret")))
                .isEmpty();
    }

    @Test
    public void getClientSessionURI_nullClientId_returnsEmpty() throws Exception {
        assertThat(new CheckSession().getClientSessionURI(createRequestWithIdToken(null, CLIENT_SECRET)))
                .isEmpty();
    }

    @Test(expectedExceptions = InvalidClientException.class)
    public void getClientSessionURI_wrongClientId_throwsInvalidClientException() throws Exception {
        given(clientRegistrationStore.get(eq("wrong-client-id"), any(OAuth2Request.class))).willThrow(InvalidClientException.class);
        new CheckSession().getClientSessionURI(createRequestWithIdToken("wrong-client-id", CLIENT_SECRET));
    }

    @Test
    public void getValidSession_validJwtAndSession_returnsTrue() throws Exception {
        String opsId = "test-ops-id";
        String sessionId = "test-session-id";
        mockClientRegistration();
        Token token = mock(Token.class);
        given(cts.read(opsId)).willReturn(token);
        given(tokenAdapter.fromToken(token)).willReturn(new JsonValue(Collections.singletonMap(LEGACY_OPS, sessionId)));
        SSOToken ssoToken = mock(SSOToken.class);
        given(ssoTokenManager.createSSOToken(sessionId)).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken)).willReturn(true);

        assertThat(new CheckSession().getValidSession(createRequestWithIdToken(CLIENT_ID, CLIENT_SECRET, opsId))).isTrue();
    }

    @Test
    public void getValidSession_invalidSignature_returnsFalse() throws Exception {
        mockClientRegistration();
        assertThat(new CheckSession().getValidSession(createRequestWithIdToken(CLIENT_ID, "wrong-secret")))
                .isFalse();
    }

}
