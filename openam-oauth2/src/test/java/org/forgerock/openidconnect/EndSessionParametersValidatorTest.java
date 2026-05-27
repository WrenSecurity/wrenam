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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.RelativeRedirectUriException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class EndSessionParametersValidatorTest {

    private static final String CLIENT_ID = "test-client";
    private static final String CLIENT_SECRET = "test-secret";
    private static final String REGISTERED_REDIRECT_URI = "https://rp.example.com/loggedout";

    private OpenIdConnectClientRegistrationStore clientRegistrationStore;
    private OAuth2ProviderSettingsFactory providerSettingsFactory;
    private OAuth2Request request;
    private EndSessionParametersValidator validator;

    @BeforeMethod
    public void setup() {
        clientRegistrationStore = mock(OpenIdConnectClientRegistrationStore.class);
        providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        request = mock(OAuth2Request.class);
        validator = new EndSessionParametersValidator(clientRegistrationStore, providerSettingsFactory);
    }

    private OpenIdConnectClientRegistration mockClientRegistration() throws Exception {
        return mockClientRegistration(JwsAlgorithm.HS256);
    }

    private OpenIdConnectClientRegistration mockClientRegistration(JwsAlgorithm algorithm) throws Exception {
        OpenIdConnectClientRegistration client = mock(OpenIdConnectClientRegistration.class);
        given(client.getClientSecret()).willReturn(CLIENT_SECRET);
        given(client.getIDTokenSignedResponseAlgorithm()).willReturn(algorithm.toString());
        given(clientRegistrationStore.get(eq(CLIENT_ID), any(OAuth2Request.class))).willReturn(client);
        return client;
    }

    private EndSessionParameters endSessionParameters(String idTokenHint) {
        return endSessionParameters(idTokenHint, null, null);
    }

    private EndSessionParameters endSessionParameters(String idTokenHint, String clientId, String redirectUri) {
        HttpServletRequest http = mock(HttpServletRequest.class);
        given(http.getParameter(OAuth2Constants.Params.END_SESSION_ID_TOKEN_HINT)).willReturn(idTokenHint);
        given(http.getParameter(OAuth2Constants.Params.CLIENT_ID)).willReturn(clientId);
        given(http.getParameter(OAuth2Constants.Params.POST_LOGOUT_REDIRECT_URI)).willReturn(redirectUri);
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

    private String buildNoneAlgIdToken() {
        return new JwtBuilderFactory().jws(new SigningManager().newNopSigningHandler())
                .headers().alg(JwsAlgorithm.NONE).done()
                .claims(azpClaims().build())
                .build();
    }

    /**
     * Builds the JWT manually, bypassing the type validation of {@code JwtClaimsSetBuilder}.
     */
    private String buildRawIdToken(String claimsJson) {
        return buildRawIdToken("{\"alg\":\"HS256\",\"typ\":\"JWT\"}", claimsJson);
    }

    private String buildRawIdToken(String headerJson, String claimsJson) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8))
                + "." + encoder.encodeToString(claimsJson.getBytes(StandardCharsets.UTF_8))
                + ".bm90LWEtc2lnbmF0dXJl";
    }

    @Test
    public void validate_withoutIdTokenHint_passes() throws Exception {
        // The id_token_hint is only RECOMMENDED. A request without it is still valid.
        validator.validate(request, endSessionParameters(null));
    }

    @Test
    public void validate_withoutIdTokenHint_ignoresPostLogoutRedirectUri() throws Exception {
        validator.validate(request, endSessionParameters(null, null, REGISTERED_REDIRECT_URI));
    }

    @DataProvider
    public Object[][] rejectedIdTokens() {
        return new Object[][] {
                { "malformed hint", "not-a-jwt", null },
                { "non-string sub claim", buildRawIdToken("{\"sub\":42}"), null },
                { "no client in token", buildHmacIdToken(CLIENT_SECRET, claims().build()), null },
                { "client_id mismatch", buildHmacIdToken(CLIENT_SECRET, azpClaims().build()), "other-client" },
                { "invalid signature", buildHmacIdToken("wrong-secret", azpClaims().build()), null },
                { "none algorithm", buildNoneAlgIdToken(), null },
                { "non-base64 signature", buildHmacIdToken(CLIENT_SECRET, azpClaims().build()) + "%%%", null },
                { "unsupported algorithm", buildRawIdToken("{\"alg\":\"XX256\",\"typ\":\"JWT\"}", "{\"azp\":\"" + CLIENT_ID + "\"}"), null },
        };
    }

    @Test(dataProvider = "rejectedIdTokens", expectedExceptions = BadRequestException.class)
    public void validate_rejectedIdToken_throwsBadRequest(String description, String idToken, String clientId) throws Exception {
        mockClientRegistration();
        validator.validate(request, endSessionParameters(idToken, clientId, null));
    }

    @Test
    public void validate_validHmacSignature_passes() throws Exception {
        mockClientRegistration();
        validator.validate(request,
                endSessionParameters(buildHmacIdToken(CLIENT_SECRET, azpClaims().build()), CLIENT_ID, null));
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void validate_hmacSignatureForRsaClient_throwsBadRequest() throws Exception {
        mockClientRegistration(JwsAlgorithm.RS256);
        validator.validate(request, endSessionParameters(buildHmacIdToken(CLIENT_SECRET, azpClaims().build())));
    }

    @Test
    public void validate_singleAudienceWithoutAzp_passes() throws Exception {
        mockClientRegistration();
        JwtClaimsSet claims = claims().aud(List.of(CLIENT_ID)).build();
        validator.validate(request, endSessionParameters(buildHmacIdToken(CLIENT_SECRET, claims)));
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void validate_multipleAudiencesWithoutAzp_throwsBadRequest() throws Exception {
        JwtClaimsSet claims = claims().aud(List.of(CLIENT_ID, "other-client")).build();
        validator.validate(request, endSessionParameters(buildHmacIdToken(CLIENT_SECRET, claims)));
    }

    @Test
    public void validate_validRsaSignature_passes() throws Exception {
        mockClientRegistration(JwsAlgorithm.RS256);
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        given(providerSettingsFactory.get(request)).willReturn(providerSettings);
        given(providerSettings.getSigningKeyPair(JwsAlgorithm.RS256)).willReturn(keyPair);
        String idToken = new JwtBuilderFactory().jws(new SigningManager().newRsaSigningHandler(keyPair.getPrivate()))
                .headers().alg(JwsAlgorithm.RS256).done()
                .claims(azpClaims().build())
                .build();
        validator.validate(request, endSessionParameters(idToken));
    }

    @Test
    public void validate_validEcdsaSignature_passes() throws Exception {
        mockClientRegistration(JwsAlgorithm.ES256);
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = generator.generateKeyPair();
        OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        given(providerSettingsFactory.get(request)).willReturn(providerSettings);
        given(providerSettings.getSigningKeyPair(JwsAlgorithm.ES256)).willReturn(keyPair);
        String idToken = new JwtBuilderFactory().jws(new SigningManager().newEcdsaSigningHandler((ECPrivateKey) keyPair.getPrivate()))
                .headers().alg(JwsAlgorithm.ES256).done()
                .claims(azpClaims().build())
                .build();
        validator.validate(request, endSessionParameters(idToken));
    }

    @Test
    public void validate_registeredRedirectUri_passes() throws Exception {
        OpenIdConnectClientRegistration client = mockClientRegistration();
        given(client.getPostLogoutRedirectUris()).willReturn(Set.of(URI.create(REGISTERED_REDIRECT_URI)));
        validator.validate(request, endSessionParameters(
                buildHmacIdToken(CLIENT_SECRET, azpClaims().build()), null, REGISTERED_REDIRECT_URI));
    }

    @Test(expectedExceptions = RelativeRedirectUriException.class)
    public void validate_relativeRedirectUri_throwsRelativeRedirectUri() throws Exception {
        mockClientRegistration();
        validator.validate(request, endSessionParameters(
                buildHmacIdToken(CLIENT_SECRET, azpClaims().build()), null, "/relative/path"));
    }

    @Test(expectedExceptions = RedirectUriMismatchException.class)
    public void validate_unregisteredRedirectUri_throwsRedirectUriMismatch() throws Exception {
        OpenIdConnectClientRegistration client = mockClientRegistration();
        given(client.getPostLogoutRedirectUris()).willReturn(Set.of(URI.create(REGISTERED_REDIRECT_URI)));
        validator.validate(request, endSessionParameters(
                buildHmacIdToken(CLIENT_SECRET, azpClaims().build()), null, "https://attacker.example.com/loggedout"));
    }

    @Test(expectedExceptions = RedirectUriMismatchException.class)
    public void validate_registeredRedirectUriWithExtraQuery_throwsRedirectUriMismatch() throws Exception {
        OpenIdConnectClientRegistration client = mockClientRegistration();
        given(client.getPostLogoutRedirectUris()).willReturn(Set.of(URI.create(REGISTERED_REDIRECT_URI)));
        validator.validate(request, endSessionParameters(
                buildHmacIdToken(CLIENT_SECRET, azpClaims().build()), null, REGISTERED_REDIRECT_URI + "?next=/"));
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void validate_malformedRedirectUri_throwsBadRequest() throws Exception {
        mockClientRegistration();
        validator.validate(request, endSessionParameters(
                buildHmacIdToken(CLIENT_SECRET, azpClaims().build()), null, "https://rp example com/^"));
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void validate_redirectUriWithoutClientInToken_throwsBadRequest() throws Exception {
        validator.validate(request, endSessionParameters(
                buildHmacIdToken(CLIENT_SECRET, claims().build()), null, REGISTERED_REDIRECT_URI));
    }
}
