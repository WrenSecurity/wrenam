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
package org.forgerock.openidconnect;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.Constants;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.CsrfProtection;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.CsrfException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.mockito.InOrder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class EndSessionServiceTest {

    private static final String CLIENT_ID = "test-client";
    private static final String CLIENT_SECRET = "test-secret";
    private static final String REALM = "/";
    private static final String UNIVERSAL_ID = "id=demo,ou=user,dc=example,dc=com";

    private EndSessionParametersValidator parametersValidator;
    private OpenIDConnectProvider openIDConnectProvider;
    private SSOTokenManager ssoTokenManager;
    private ClientRegistrationStore clientRegistrationStore;
    private CsrfProtection csrfProtection;
    private OAuth2ProviderSettingsFactory providerSettingsFactory;
    private OAuth2ProviderSettings providerSettings;
    private OAuth2Request request;
    private EndSessionService service;

    @BeforeMethod
    public void setup() {
        parametersValidator = mock(EndSessionParametersValidator.class);
        openIDConnectProvider = mock(OpenIDConnectProvider.class);
        ssoTokenManager = mock(SSOTokenManager.class);
        clientRegistrationStore = mock(ClientRegistrationStore.class);
        csrfProtection = mock(CsrfProtection.class);
        providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        providerSettings = mock(OAuth2ProviderSettings.class);
        request = mock(OAuth2Request.class);
        service = new EndSessionService(parametersValidator, openIDConnectProvider, ssoTokenManager,
                clientRegistrationStore, csrfProtection, providerSettingsFactory);
    }

    private EndSessionParameters endSessionParameters(String idTokenHint) {
        return endSessionParameters(idTokenHint, null);
    }

    private EndSessionParameters endSessionParameters(String idTokenHint, String clientId) {
        HttpServletRequest http = mock(HttpServletRequest.class);
        given(http.getParameter(OAuth2Constants.Params.END_SESSION_ID_TOKEN_HINT)).willReturn(idTokenHint);
        given(http.getParameter(OAuth2Constants.Params.CLIENT_ID)).willReturn(clientId);
        return EndSessionParameters.from(http);
    }

    private JwtClaimsSetBuilder claims() {
        return new JwtBuilderFactory().claims();
    }

    private JwtClaimsSetBuilder azpClaims() {
        return claims().claim(OAuth2Constants.JWTTokenParams.AZP, CLIENT_ID);
    }

    private String buildHmacIdToken(String secret, JwtClaimsSet claims) {
        SigningHandler signingHandler = new SigningManager()
                .newHmacSigningHandler(secret.getBytes(StandardCharsets.UTF_8));
        return new JwtBuilderFactory().jws(signingHandler)
                .headers().alg(JwsAlgorithm.HS256).done()
                .claims(claims)
                .build();
    }

    /**
     * Builds the JWT manually, bypassing the type validation of {@code JwtClaimsSetBuilder}.
     */
    private String buildRawIdToken(String claimsJson) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8))
                + "." + encoder.encodeToString(claimsJson.getBytes(StandardCharsets.UTF_8))
                + ".bm90LWEtc2lnbmF0dXJl";
    }

    private SSOToken sessionWithId(String sessionId) {
        SSOToken ssoToken = mock(SSOToken.class);
        // SSOTokenID exposes the value only through toString(), which Mockito cannot stub reliably.
        given(ssoToken.getTokenID()).willReturn(new SSOTokenID() {
            @Override
            public String toString() {
                return sessionId;
            }

            @Override
            public boolean equals(Object other) {
                return other instanceof SSOTokenID && sessionId.equals(other.toString());
            }

            @Override
            public int hashCode() {
                return sessionId.hashCode();
            }
        });
        return ssoToken;
    }

    @Test
    public void validate_delegatesToParametersValidator() throws Exception {
        EndSessionParameters endSessionParameters = endSessionParameters(null);
        service.validate(request, endSessionParameters);
        verify(parametersValidator).validate(request, endSessionParameters);
    }

    @Test
    public void endSession_destroysSessionIdentifiedByOpsClaim() throws Exception {
        String idToken = buildHmacIdToken(CLIENT_SECRET, azpClaims().claim(OAuth2Constants.JWTTokenParams.OPS, "ops-id").build());

        service.endSession(request, endSessionParameters(idToken));

        verify(openIDConnectProvider).destroySession("ops-id");
        // The ID Token must be exposed on the OAuth2 request so the access audit filter can attribute the logout.
        verify(request).setToken(eq(OpenIdConnectToken.class), any(OpenIdConnectToken.class));
    }

    @Test
    public void endSession_withoutOpsClaim_fallsBackToLegacyOpsClaim() throws Exception {
        String idToken = buildHmacIdToken(CLIENT_SECRET,
                azpClaims().claim(OAuth2Constants.JWTTokenParams.LEGACY_OPS, "legacy-ops-id").build());

        service.endSession(request, endSessionParameters(idToken));

        verify(openIDConnectProvider).destroySession("legacy-ops-id");
    }

    @Test
    public void currentSession_activeSession_returnsSession() throws Exception {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        SSOToken ssoToken = mock(SSOToken.class);
        given(ssoTokenManager.createSSOToken(servletRequest)).willReturn(ssoToken);
        assertThat(service.getCurrentSession(servletRequest)).isEqualTo(Optional.of(ssoToken));
    }

    @Test
    public void currentSession_noSession_returnsEmpty() throws Exception {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        given(ssoTokenManager.createSSOToken(servletRequest)).willThrow(new SSOException("no session"));
        assertThat(service.getCurrentSession(servletRequest)).isEqualTo(Optional.empty());
    }

    @Test
    public void canSkipLogoutConfirmation_trustedCurrentSession_returnsTrue() throws Exception {
        SSOToken session = sessionWithId("session-id");
        ClientRegistration client = mock(ClientRegistration.class);
        String idToken = buildHmacIdToken(CLIENT_SECRET, azpClaims()
                .claim(OAuth2Constants.JWTTokenParams.OPS, "ops-id").build());
        given(providerSettingsFactory.get(request)).willReturn(providerSettings);
        given(clientRegistrationStore.get(CLIENT_ID, request)).willReturn(client);
        given(openIDConnectProvider.isOpsTokenForSession("ops-id", session)).willReturn(true);

        assertThat(service.canSkipLogoutConfirmation(request, endSessionParameters(idToken), session)).isTrue();
    }

    @Test
    public void canSkipLogoutConfirmation_providerRequiresConfirmation_returnsFalse() throws Exception {
        SSOToken session = sessionWithId("session-id");
        ClientRegistration client = mock(ClientRegistration.class);
        String idToken = buildHmacIdToken(CLIENT_SECRET, azpClaims()
                .claim(OAuth2Constants.JWTTokenParams.OPS, "ops-id").build());
        given(providerSettingsFactory.get(request)).willReturn(providerSettings);
        given(providerSettings.isLogoutConfirmationRequired()).willReturn(true);
        given(clientRegistrationStore.get(CLIENT_ID, request)).willReturn(client);

        assertThat(service.canSkipLogoutConfirmation(request, endSessionParameters(idToken), session)).isFalse();

        verify(openIDConnectProvider, never()).isOpsTokenForSession(any(String.class), any(SSOToken.class));
    }

    @Test
    public void canSkipLogoutConfirmation_clientRequiresConfirmation_returnsFalse() throws Exception {
        SSOToken session = sessionWithId("session-id");
        ClientRegistration client = mock(ClientRegistration.class);
        String idToken = buildHmacIdToken(CLIENT_SECRET, azpClaims()
                .claim(OAuth2Constants.JWTTokenParams.OPS, "ops-id").build());
        given(providerSettingsFactory.get(request)).willReturn(providerSettings);
        given(clientRegistrationStore.get(CLIENT_ID, request)).willReturn(client);
        given(client.isLogoutConfirmationRequired()).willReturn(true);

        assertThat(service.canSkipLogoutConfirmation(request, endSessionParameters(idToken), session)).isFalse();

        verify(openIDConnectProvider, never()).isOpsTokenForSession(any(String.class), any(SSOToken.class));
    }

    @Test
    public void canSkipLogoutConfirmation_tokenForDifferentSession_returnsFalse() throws Exception {
        SSOToken session = sessionWithId("session-id");
        ClientRegistration client = mock(ClientRegistration.class);
        String idToken = buildHmacIdToken(CLIENT_SECRET, azpClaims()
                .claim(OAuth2Constants.JWTTokenParams.OPS, "ops-id").build());
        given(providerSettingsFactory.get(request)).willReturn(providerSettings);
        given(clientRegistrationStore.get(CLIENT_ID, request)).willReturn(client);

        assertThat(service.canSkipLogoutConfirmation(request, endSessionParameters(idToken), session)).isFalse();
    }

    @Test
    public void canSkipLogoutConfirmation_attachesClientRegistrationBeforeResolvingSettings() throws Exception {
        SSOToken session = sessionWithId("session-id");
        ClientRegistration client = mock(ClientRegistration.class);
        String idToken = buildHmacIdToken(CLIENT_SECRET, azpClaims()
                .claim(OAuth2Constants.JWTTokenParams.OPS, "ops-id").build());
        given(providerSettingsFactory.get(request)).willReturn(providerSettings);
        given(clientRegistrationStore.get(CLIENT_ID, request)).willReturn(client);

        service.canSkipLogoutConfirmation(request, endSessionParameters(idToken), session);

        // The provider settings depend on the kind of client, which the end session endpoint does not authenticate.
        InOrder inOrder = inOrder(request, providerSettingsFactory);
        inOrder.verify(request).setClientRegistration(client);
        inOrder.verify(providerSettingsFactory).get(request);
    }

    @Test
    public void canSkipLogoutConfirmation_idTokenWithoutOpsClaim_returnsFalse() throws Exception {
        SSOToken session = sessionWithId("session-id");
        String idToken = buildHmacIdToken(CLIENT_SECRET, azpClaims().build());

        assertThat(service.canSkipLogoutConfirmation(request, endSessionParameters(idToken), session)).isFalse();

        verify(clientRegistrationStore, never()).get(any(String.class), eq(request));
    }

    @Test
    public void canSkipLogoutConfirmation_withoutHintOrSession_returnsFalse() throws Exception {
        assertThat(service.canSkipLogoutConfirmation(request, endSessionParameters(null), sessionWithId("session-id")))
                .isFalse();
        assertThat(service.canSkipLogoutConfirmation(request,
                endSessionParameters(buildHmacIdToken(CLIENT_SECRET, azpClaims().build())), null)).isFalse();

        verify(providerSettingsFactory, never()).get(any(OAuth2Request.class));
    }

    @Test
    public void canSkipLogoutConfirmation_legacyOpsClaim_isSupported() throws Exception {
        SSOToken session = sessionWithId("session-id");
        ClientRegistration client = mock(ClientRegistration.class);
        String idToken = buildHmacIdToken(CLIENT_SECRET, azpClaims()
                .claim(OAuth2Constants.JWTTokenParams.LEGACY_OPS, "legacy-ops-id").build());
        given(providerSettingsFactory.get(request)).willReturn(providerSettings);
        given(clientRegistrationStore.get(CLIENT_ID, request)).willReturn(client);
        given(openIDConnectProvider.isOpsTokenForSession("legacy-ops-id", session)).willReturn(true);

        assertThat(service.canSkipLogoutConfirmation(request, endSessionParameters(idToken), session)).isTrue();
    }

    @Test
    public void logout_destroysSessionToken() throws Exception {
        SSOToken ssoToken = mock(SSOToken.class);
        service.logout(ssoToken);
        verify(ssoTokenManager).destroyToken(ssoToken);
    }

    @Test
    public void logout_destroyFailure_isSwallowed() throws Exception {
        SSOToken ssoToken = mock(SSOToken.class);
        willThrow(new SSOException("destroy failed")).given(ssoTokenManager).destroyToken(ssoToken);

        // The session may have expired between validation and destruction; logout is best-effort and must not fail.
        service.logout(ssoToken);

        verify(ssoTokenManager).destroyToken(ssoToken);
    }

    @Test
    public void resolveCsrfToken_activeSession_returnsSessionId() {
        assertThat(service.resolveCsrfToken(sessionWithId("session-123")))
                .isEqualTo(Optional.of("session-123"));
    }

    @Test
    public void resolveCsrfToken_noSession_returnsEmpty() {
        assertThat(service.resolveCsrfToken(null)).isEqualTo(Optional.empty());
    }

    @Test
    public void verifyCsrf_validRequest_passes() throws Exception {
        SSOToken session = mock(SSOToken.class);

        service.verifyCsrf(request, session);

        verify(csrfProtection).isCsrfAttack(request);
    }

    @Test(expectedExceptions = CsrfException.class)
    public void verifyCsrf_attack_throwsCsrf() throws Exception {
        SSOToken session = mock(SSOToken.class);
        given(csrfProtection.isCsrfAttack(request)).willReturn(true);

        service.verifyCsrf(request, session);
    }

    @Test
    public void verifyCsrf_noSession_skipsCsrfProtection() throws Exception {
        service.verifyCsrf(request, null);

        verify(csrfProtection, never()).isCsrfAttack(request);
    }

    @DataProvider
    public Object[][] resolvableClients() {
        return new Object[][] {
                { "azp in token", buildHmacIdToken(CLIENT_SECRET, azpClaims().build()), null },
                { "single audience without azp", buildHmacIdToken(CLIENT_SECRET, claims().aud(List.of(CLIENT_ID)).build()), null },
                // RFC 7519 allows the aud claim to be a single string instead of an array.
                { "string audience", buildRawIdToken("{\"aud\":\"" + CLIENT_ID + "\"}"), null },
                { "explicit client_id parameter", null, CLIENT_ID },
                { "malformed hint falls back to client_id parameter", "not-a-jwt", CLIENT_ID },
                { "non-string audience falls back to client_id parameter", buildRawIdToken("{\"aud\":42}"), CLIENT_ID },
        };
    }

    @Test(dataProvider = "resolvableClients")
    public void resolveClientName_resolvableClient_returnsDisplayName(String description, String idToken, String clientId) throws Exception {
        ClientRegistration client = mock(ClientRegistration.class);
        given(clientRegistrationStore.get(eq(CLIENT_ID), eq(REALM), isNull())).willReturn(client);
        given(client.getDisplayName(eq(Locale.ENGLISH))).willReturn("My Application");
        assertThat(service.resolveClientName(REALM, endSessionParameters(idToken, clientId), Locale.ENGLISH))
                .isEqualTo(Optional.of("My Application"));
    }

    @Test
    public void resolveClientName_blankDisplayName_fallsBackToClientId() throws Exception {
        ClientRegistration client = mock(ClientRegistration.class);
        given(clientRegistrationStore.get(eq(CLIENT_ID), eq(REALM), isNull())).willReturn(client);
        given(client.getDisplayName(eq(Locale.ENGLISH))).willReturn(" ");
        given(client.getClientId()).willReturn(CLIENT_ID);
        String idToken = buildHmacIdToken(CLIENT_SECRET, azpClaims().build());
        assertThat(service.resolveClientName(REALM, endSessionParameters(idToken), Locale.ENGLISH))
                .isEqualTo(Optional.of(CLIENT_ID));
    }

    @Test
    public void resolveClientName_noClient_returnsEmpty() throws Exception {
        assertThat(service.resolveClientName(REALM, endSessionParameters(null), Locale.ENGLISH))
                .isEqualTo(Optional.empty());
    }

    @Test
    public void resolveUserName_subClaim_returnsSubject() {
        String idToken = buildHmacIdToken(CLIENT_SECRET,
                claims().claim(OAuth2Constants.JWTTokenParams.SUB, "demo").build());
        assertThat(service.resolveUserName(endSessionParameters(idToken), null))
                .isEqualTo(Optional.of("demo"));
    }

    @Test
    public void resolveUserName_noHint_fallsBackToSessionIdentity() throws Exception {
        SSOToken ssoToken = mock(SSOToken.class);
        given(ssoToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn(UNIVERSAL_ID);
        assertThat(service.resolveUserName(endSessionParameters(null), ssoToken))
                .isEqualTo(Optional.of("demo"));
    }

    @Test
    public void resolveUserName_nonStringSubClaim_fallsBackToSessionIdentity() throws Exception {
        SSOToken ssoToken = mock(SSOToken.class);
        given(ssoToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn(UNIVERSAL_ID);
        assertThat(service.resolveUserName(endSessionParameters(buildRawIdToken("{\"sub\":42}")), ssoToken))
                .isEqualTo(Optional.of("demo"));
    }

    @Test
    public void resolveUserName_blankSubClaim_fallsBackToSessionIdentity() throws Exception {
        SSOToken ssoToken = mock(SSOToken.class);
        given(ssoToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn(UNIVERSAL_ID);
        String idToken = buildHmacIdToken(CLIENT_SECRET,
                claims().claim(OAuth2Constants.JWTTokenParams.SUB, " ").build());

        assertThat(service.resolveUserName(endSessionParameters(idToken), ssoToken))
                .isEqualTo(Optional.of("demo"));
    }

    @Test
    public void resolveUserName_noHintAndNoSession_returnsEmpty() {
        assertThat(service.resolveUserName(endSessionParameters(null), null))
                .isEqualTo(Optional.empty());
    }
}
