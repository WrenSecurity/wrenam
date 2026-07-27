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

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Carrying the normalized parameters of an OpenID Connect RP-Initiated Logout request.
 * <p>
 * Parameters are trimmed on creation and blank values are treated as absent, so all getters consistently return
 * {@link Optional#empty()} for anything a caller should ignore.
 * </p>
 * <p>
 * A malformed {@code id_token_hint} is kept as the raw string but exposes no parsed token.
 * Strict rejection of a malformed hint is left to {@link EndSessionParametersValidator#validate}.
 * </p>
 */
public final class EndSessionParameters {

    private static final Logger logger = LoggerFactory.getLogger("OAuth2Provider");

    private final String idTokenHint;
    private final SignedJwt idToken;
    private final String clientId;
    private final String postLogoutRedirectUri;
    private final String state;

    private EndSessionParameters(String idTokenHint, SignedJwt idToken, String clientId,
            String postLogoutRedirectUri, String state) {
        this.idTokenHint = idTokenHint;
        this.clientId = clientId;
        this.postLogoutRedirectUri = postLogoutRedirectUri;
        this.state = state;
        this.idToken = idToken;
    }

    /**
     * Builds an end session request from the parameters carried by the servlet request.
     *
     * @param request The servlet request.
     * @return The end session request.
     */
    public static EndSessionParameters from(HttpServletRequest request) {
        String idTokenHint = trimToNull(request.getParameter(OAuth2Constants.Params.END_SESSION_ID_TOKEN_HINT));
        return new EndSessionParameters(
                idTokenHint,
                reconstruct(idTokenHint),
                trimToNull(request.getParameter(OAuth2Constants.Params.CLIENT_ID)),
                trimToNull(request.getParameter(OAuth2Constants.Params.POST_LOGOUT_REDIRECT_URI)),
                trimToNull(request.getParameter(OAuth2Constants.Params.STATE))
        );
    }

    private static String trimToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private static SignedJwt reconstruct(String idTokenHint) {
        if (idTokenHint == null) {
            return null;
        }
        try {
            return new JwtReconstruction().reconstructJwt(idTokenHint, SignedJwt.class);
        } catch (RuntimeException e) {
            // The hint is attacker-supplied input; reconstruction can fail with more than JwtRuntimeException
            // (e.g. Base64 decoding errors). Treat any malformed hint as "no token".
            logger.debug("Ignoring malformed id_token_hint", e);
            return null;
        }
    }

    public Optional<String> getIdTokenHint() {
        return Optional.ofNullable(idTokenHint);
    }

    public Optional<SignedJwt> getIdToken() {
        return Optional.ofNullable(idToken);
    }

    public Optional<JwtClaimsSet> getClaims() {
        return getIdToken().map(SignedJwt::getClaimsSet);
    }

    /**
     * @return The client the {@code id_token_hint} was issued to – the authorized party ({@code azp}) claim, or the
     * single audience when {@code azp} is absent — or {@link Optional#empty()} when it cannot be determined.
     */
    public Optional<String> getTokenClientId() {
        return getClaims().flatMap(EndSessionParameters::resolveTokenClientId);
    }

    private static Optional<String> resolveTokenClientId(JwtClaimsSet claims) {
        JsonValue azp = claims.get(OAuth2Constants.JWTTokenParams.AZP);
        if (azp != null && azp.isString() && StringUtils.isNotBlank(azp.asString())) {
            return Optional.of(azp.asString());
        }
        return Optional.ofNullable(claims.getAudience())
                .filter(audience -> audience.size() == 1)
                .map(audience -> audience.get(0));
    }

    public Optional<String> getClientId() {
        return Optional.ofNullable(clientId);
    }

    public Optional<String> getPostLogoutRedirectUri() {
        return Optional.ofNullable(postLogoutRedirectUri);
    }

    public Optional<String> getState() {
        return Optional.ofNullable(state);
    }

}
