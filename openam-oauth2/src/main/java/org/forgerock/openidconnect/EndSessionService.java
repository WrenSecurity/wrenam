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

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Optional;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.CsrfProtection;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.CsrfException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the domain logic behind the OpenID Connect RP-Initiated Logout end session endpoint.
 */
public class EndSessionService {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");

    private final EndSessionParametersValidator parametersValidator;
    private final OpenIDConnectProvider openIDConnectProvider;
    private final SSOTokenManager ssoTokenManager;
    private final ClientRegistrationStore clientRegistrationStore;
    private final CsrfProtection csrfProtection;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;

    /**
     * Constructs a new EndSessionService.
     *
     * @param parametersValidator An instance of the EndSessionParametersValidator.
     * @param openIDConnectProvider An instance of the OpenIDConnectProvider.
     * @param ssoTokenManager An instance of the SSOTokenManager.
     * @param clientRegistrationStore An instance of the ClientRegistrationStore.
     * @param csrfProtection An instance of the CsrfProtection.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     */
    @Inject
    public EndSessionService(EndSessionParametersValidator parametersValidator,
            OpenIDConnectProvider openIDConnectProvider, SSOTokenManager ssoTokenManager,
            ClientRegistrationStore clientRegistrationStore, CsrfProtection csrfProtection,
            OAuth2ProviderSettingsFactory providerSettingsFactory) {
        this.parametersValidator = parametersValidator;
        this.openIDConnectProvider = openIDConnectProvider;
        this.ssoTokenManager = ssoTokenManager;
        this.clientRegistrationStore = clientRegistrationStore;
        this.csrfProtection = csrfProtection;
        this.providerSettingsFactory = providerSettingsFactory;
    }

    /**
     * Validate the end session parameters as required by the RP-Initiated Logout specification.
     * <p>
     * The {@code id_token_hint} is only RECOMMENDED, so a request without it is still valid. When it is present,
     * the OP MUST validate it (and the {@code post_logout_redirect_uri}, when also present) before acting on it.
     * </p>
     *
     * @param request The OAuth2 framework representation of the HTTP request.
     * @param params The end session request parameters.
     * @throws OAuth2Exception If the parameters do not represent a valid end session request.
     */
    public void validate(OAuth2Request request, EndSessionParameters params) throws OAuth2Exception {
        parametersValidator.validate(request, params);
    }

    /**
     * End the OpenID Provider session associated with the request's {@code id_token_hint}.
     *
     * @param request The OAuth2 framework representation of the HTTP request, needed to expose the ID Token
     *        to the audit.
     * @param params  The end session request parameters.
     * @throws BadRequestException If the request is malformed.
     * @throws ServerException     If any internal server error occurs.
     */
    public void endSession(OAuth2Request request, EndSessionParameters params)
            throws BadRequestException, ServerException {
        JwtClaimsSet claims = params.getClaims()
                .orElseThrow(() -> new BadRequestException("The endSession endpoint requires a valid id_token_hint parameter"));
        // The proprietary "ops" claim is used instead of the standard "sid" because ID tokens do not issue "sid" yet.
        String opsId = resolveOpsId(params)
                .orElseThrow(() -> new BadRequestException("id_token_hint does not reference a session"));
        // Expose the ID Token on the OAuth2 request so the access audit filter can attribute the logout.
        request.setToken(OpenIdConnectToken.class, new OpenIdConnectToken(claims));
        openIDConnectProvider.destroySession(opsId);
    }

    /**
     * Resolve the user's current SSO session from the servlet request.
     *
     * @param request The servlet request carrying the SSO token.
     * @return The session, or {@link Optional#empty()} if there is no active session.
     */
    public Optional<SSOToken> getCurrentSession(HttpServletRequest request) {
        try {
            return Optional.of(ssoTokenManager.createSSOToken(request));
        } catch (SSOException e) {
            logger.debug("No active session on the request", e);
            return Optional.empty();
        }
    }

    /**
     * Determines whether user confirmation may be skipped for an RP-Initiated Logout request.
     *
     * @param request The OAuth2 request.
     * @param params The end session parameters.
     * @param session The user's current SSO session, or {@code null} if there is none.
     * @return {@code true} if the confirmation page may be skipped.
     * @throws OAuth2Exception If the client or provider settings cannot be resolved.
     */
    public boolean canSkipLogoutConfirmation(OAuth2Request request, EndSessionParameters params, SSOToken session)
            throws OAuth2Exception {
        if (session == null || params.getIdTokenHint().isEmpty()) {
            return false;
        }
        Optional<String> clientId = params.getTokenClientId();
        if (clientId.isEmpty()) {
            return false;
        }
        // The proprietary "ops" claim is used instead of the standard "sid" because ID tokens do not issue "sid" yet.
        Optional<String> opsId = resolveOpsId(params);
        if (opsId.isEmpty()) {
            return false;
        }
        ClientRegistration client = clientRegistrationStore.get(clientId.get(), request);
        request.setClientRegistration(client);
        if (providerSettingsFactory.get(request).isLogoutConfirmationRequired()
                || client.isLogoutConfirmationRequired()) {
            return false;
        }
        return openIDConnectProvider.isOpsTokenForSession(opsId.get(), session);
    }

    /**
     * Terminate the given SSO session.
     * <p>
     * Used as a fallback when no {@code id_token_hint} is available. Logout is best-effort: a session that has
     * already expired or been destroyed is treated as a successful logout rather than surfaced as an error.
     * </p>
     *
     * @param session The SSO session to terminate.
     */
    public void logout(SSOToken session) {
        try {
            ssoTokenManager.destroyToken(session);
        } catch (SSOException e) {
            logger.debug("Unable to destroy an already terminated session", e);
        }
    }

    /**
     * Resolve the anti-CSRF token to embed in the confirmation page. The token is the id of the user's current SSO
     * session because a forged cross-site page cannot read it.
     *
     * @param session The user's current SSO session, may be {@code null}.
     * @return The session id, or {@link Optional#empty()} if there is no active session.
     */
    public Optional<String> resolveCsrfToken(SSOToken session) {
        return Optional.ofNullable(session).map(token -> token.getTokenID().toString());
    }

    /**
     * Verify the anti-CSRF token submitted with a logout decision against the user's current SSO session.
     *
     * @param request The end session request carrying the submitted {@code csrf} token.
     * @param session The user's current SSO session, or {@code null} if there is none.
     * @throws CsrfException If a session exists and the token is missing or does not match it.
     */
    public void verifyCsrf(OAuth2Request request, SSOToken session) throws CsrfException {
        if (session != null && csrfProtection.isCsrfAttack(request)) {
            logger.debug("Session id from end session request does not match users session");
            throw new CsrfException();
        }
    }

    /**
     * Resolve a human-friendly name of the client requesting the logout.
     *
     * @param realm The request realm.
     * @param request The end session request.
     * @param locale The locale used to resolve the display name, may be {@code null}.
     * @return The client name, or {@link Optional#empty()} if no client could be resolved.
     */
    public Optional<String> resolveClientName(String realm, EndSessionParameters request, Locale locale)
            throws InvalidClientException, NotFoundException {
        return resolveClientRegistration(realm, request)
                .map(registration -> {
                    String displayName = registration.getDisplayName(locale);
                    return StringUtils.isNotBlank(displayName) ? displayName : registration.getClientId();
                });
    }

    /**
     * Resolve the name of the user being logged out, preferring the {@code sub} claim of the {@code id_token_hint} and
     * falling back to the identity behind the SSO session.
     *
     * @param request The end session request.
     * @param session The user's current SSO session, may be {@code null}.
     * @return The user name, or {@link Optional#empty()} if it cannot be resolved.
     */
    public Optional<String> resolveUserName(EndSessionParameters request, SSOToken session) {
        return request.getClaims()
                .flatMap(claims -> claimAsString(claims, OAuth2Constants.JWTTokenParams.SUB))
                .or(() -> Optional.ofNullable(session).flatMap(this::identityName));
    }

    private Optional<String> resolveOpsId(EndSessionParameters params) {
        return params.getClaims()
                .flatMap(claims -> claimAsString(claims, OAuth2Constants.JWTTokenParams.OPS)
                        .or(() -> claimAsString(claims, OAuth2Constants.JWTTokenParams.LEGACY_OPS)));
    }

    private Optional<ClientRegistration> resolveClientRegistration(String realm, EndSessionParameters request)
            throws InvalidClientException, NotFoundException {
        Optional<String> resolvedClientId = resolveClientId(request);
        if (resolvedClientId.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(clientRegistrationStore.get(resolvedClientId.get(), realm, null));
    }

    private Optional<String> resolveClientId(EndSessionParameters request) {
        // Prefer the client identified by the id_token_hint, falling back to the explicit client_id parameter.
        return request.getTokenClientId().or(request::getClientId);
    }

    private Optional<String> identityName(SSOToken session) {
        try {
            return Optional.of(new AMIdentity(session).getName());
        } catch (SSOException | IdRepoException e) {
            logger.error("Unable to get the identity", e);
            return Optional.empty();
        }
    }

    private Optional<String> claimAsString(JwtClaimsSet claims, String claim) {
        JsonValue value = claims.get(claim);
        if (value != null && value.isString() && StringUtils.isNotBlank(value.asString())) {
            return Optional.of(value.asString());
        }
        return Optional.empty();
    }

}
