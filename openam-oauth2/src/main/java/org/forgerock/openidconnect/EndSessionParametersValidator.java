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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.ECPublicKey;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.RelativeRedirectUriException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates OpenID Connect RP-Initiated Logout requests.
 */
public class EndSessionParametersValidator {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");

    private final OpenIdConnectClientRegistrationStore clientRegistrationStore;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final SigningManager signingManager;

    /**
     * Constructs a new EndSessionParametersValidator.
     *
     * @param clientRegistrationStore An instance of the OpenIdConnectClientRegistrationStore.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     */
    @Inject
    public EndSessionParametersValidator(OpenIdConnectClientRegistrationStore clientRegistrationStore,
            OAuth2ProviderSettingsFactory providerSettingsFactory) {
        this.clientRegistrationStore = clientRegistrationStore;
        this.providerSettingsFactory = providerSettingsFactory;
        this.signingManager = new SigningManager();
    }

    /**
     * Validate the end session parameters as required by the RP-Initiated Logout specification.
     * <p>
     * The {@code id_token_hint} is only RECOMMENDED, so a request without it is still valid. When it is present,
     * the OP MUST validate that it issued the ID Token and that the {@code client_id}, if also present, matches
     * the client the ID Token was issued to. The {@code post_logout_redirect_uri} may only be used when it exactly
     * matches one registered for that client.
     * </p>
     *
     * @param request The OAuth2 framework representation of the HTTP request.
     * @param params  The end session request parameters.
     * @throws OAuth2Exception If the parameters do not represent a valid end session request.
     */
    public void validate(OAuth2Request request, EndSessionParameters params) throws OAuth2Exception {
        if (params.getIdTokenHint().isEmpty()) {
            // The id_token_hint is only RECOMMENDED. A request without it is still valid.
            return;
        }
        verifyIdToken(request, params);
        if (params.getPostLogoutRedirectUri().isPresent()) {
            validatePostLogoutRedirect(request, params);
        }
    }

    private void verifyIdToken(OAuth2Request request, EndSessionParameters params)
            throws BadRequestException, InvalidClientException, NotFoundException, ServerException {
        SignedJwt jwt = params.getIdToken()
                .orElseThrow(() -> new BadRequestException("id_token_hint is not a valid JWT"));
        String tokenClientId = params.getTokenClientId()
                .orElseThrow(() -> new BadRequestException("id_token_hint does not identify a client"));
        // When both client_id and id_token_hint are present, they MUST match.
        Optional<String> clientId = params.getClientId();
        if (clientId.isPresent() && !clientId.get().equals(tokenClientId)) {
            throw new BadRequestException("client_id does not match the id_token_hint");
        }
        OpenIdConnectClientRegistration client = clientRegistrationStore.get(tokenClientId, request);
        // The OP MUST validate that it was the issuer of the ID Token.
        if (!verifyIssuerSignature(request, jwt, client)) {
            throw new BadRequestException("id_token_hint signature could not be verified");
        }
    }

    private void validatePostLogoutRedirect(OAuth2Request request, EndSessionParameters params)
            throws BadRequestException, InvalidClientException, RedirectUriMismatchException,
            RelativeRedirectUriException, NotFoundException {
        if (params.getIdToken().isEmpty()) {
            throw new BadRequestException("id_token_hint is not a valid JWT");
        }
        String clientId = params.getTokenClientId()
                .orElseThrow(() -> new BadRequestException("id_token_hint does not identify a client"));
        OpenIdConnectClientRegistration client = clientRegistrationStore.get(clientId, request);
        String redirectUri = params.getPostLogoutRedirectUri()
                .orElseThrow(() -> new BadRequestException("post_logout_redirect_uri is missing"));
        URI requestedUri;
        try {
            requestedUri = URI.create(redirectUri);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("post_logout_redirect_uri is not a valid URI");
        }
        if (!requestedUri.isAbsolute()) {
            throw new RelativeRedirectUriException();
        }
        if (!client.getPostLogoutRedirectUris().contains(requestedUri)) {
            throw new RedirectUriMismatchException();
        }
    }

    private boolean verifyIssuerSignature(OAuth2Request request, SignedJwt jwt, OpenIdConnectClientRegistration client)
            throws NotFoundException, ServerException {
        try {
            JwsAlgorithm algorithm = JwsAlgorithm.valueOf(client.getIDTokenSignedResponseAlgorithm().toUpperCase(Locale.ROOT));
            if (algorithm != jwt.getHeader().getAlgorithm()) {
                return false;
            }
            SigningHandler signingHandler;
            switch (algorithm.getAlgorithmType()) {
                case HMAC:
                    String clientSecret = client.getClientSecret();
                    if (StringUtils.isEmpty(clientSecret)) {
                        return false;
                    }
                    signingHandler = signingManager.newHmacSigningHandler(
                            clientSecret.getBytes(StandardCharsets.UTF_8));
                    break;
                case RSA:
                    signingHandler = signingManager.newRsaSigningHandler(
                            providerSettingsFactory.get(request).getSigningKeyPair(algorithm).getPublic());
                    break;
                case ECDSA:
                    signingHandler = signingManager.newEcdsaVerificationHandler(
                            (ECPublicKey) providerSettingsFactory.get(request).getSigningKeyPair(algorithm).getPublic());
                    break;
                default:
                    return false;
            }
            return jwt.verify(signingHandler);
        } catch (RuntimeException e) {
            logger.debug("Unable to verify the id_token_hint signature", e);
            return false;
        }
    }

}
