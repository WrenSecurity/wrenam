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
 */
package org.forgerock.oauth2.core;

import java.security.KeyPair;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.forgerock.oauth2.resources.ResourceSetStore;

import freemarker.template.Template;

/**
 * Interface for defining all settings an OAuth2 provider can have and that can be configured.
 */
public interface OAuth2ProviderSettings {
    /**
     * Determines whether access and refresh tokens should be stateless.
     *
     * @return {@code true} if access and refresh tokens are stateless.
     * @throws ServerException If any internal server error occurs.
     */
    boolean isStatelessTokensEnabled() throws ServerException;

    /**
     * Determines whether idtokeninfo endpoint should require client authentication.
     *
     * @return {@code true} if idtokeninfo endpoint requires client authentication.
     * @throws ServerException If any internal server error occurs.
     */
    boolean isIdTokenInfoClientAuthenticationEnabled() throws ServerException;

    /**
     * Gets the signing algorithm used when issuing stateless access and refresh tokens.
     *
     * @return The signing algorithm.
     * @throws ServerException If any internal server error occurs.
     */
    String getTokenSigningAlgorithm() throws ServerException;

    /**
     * Determines whether token compression is enabled for stateless access and refresh tokens.
     *
     * @return true if compression should be enabled.
     * @throws ServerException if an error occurs reading the settings.
     */
    boolean isTokenCompressionEnabled() throws ServerException;

    /**
     * Gets the Base64 encoded shared secret used to sign stateless access and refresh tokens.
     *
     * @return The Base64 encoded shared secret.
     * @throws ServerException If any internal server error occurs.
     */
    String getTokenHmacSharedSecret() throws ServerException;

    /**
     * Gets the response types allowed by the OAuth2 provider.
     *
     * @return The allowed response types and their handler implementations.
     * @throws UnsupportedResponseTypeException If the requested response type is not supported by either the client
     *          or the OAuth2 provider.
     * @throws ServerException If any internal server error occurs.
     */
    Map<String, ResponseTypeHandler> getAllowedResponseTypes() throws UnsupportedResponseTypeException,
            ServerException;

    /**
     * Determines if the consent can be saved or not, due to a lack of configuration.
     *
     * @return {@code true} if the consent can be saved, false if it is not configured properly.
     */
    boolean isSaveConsentEnabled();

    /**
     * Determines whether a resource owner's consent has been saved from a previous authorize request.
     *
     * @param resourceOwner The resource owner.
     * @param clientId The if of the client making the request.
     * @param scope The requested scope.
     * @return {@code true} if the resource owner has previously requested that consent should be saved from the
     *          specified client and the exact scope.
     */
    boolean isConsentSaved(ResourceOwner resourceOwner, String clientId, Set<String> scope);

    /**
     * Provided as an extension point to allow the OAuth2 provider to customise the scope requested when authorization
     * is requested.
     *
     * @param clientRegistration The client registration.
     * @param scope The requested scope.
     * @return The updated scope used in the remaining OAuth2 process.
     * @throws ServerException If any internal server error occurs.
     * @throws InvalidScopeException If the requested scope is invalid, unknown, or malformed.
     */
    Set<String> validateAuthorizationScope(ClientRegistration clientRegistration, Set<String> scope,
                                           OAuth2Request request) throws ServerException, InvalidScopeException;

    /**
     * Provided as an extension point to allow the OAuth2 provider to customise the scope requested when an access token
     * is requested.
     *
     * @param clientRegistration The client registration.
     * @param scope The requested scope.
     * @param request The OAuth2 request.
     * @return The updated scope used in the remaining OAuth2 process.
     * @throws ServerException If any internal server error occurs.
     * @throws InvalidScopeException If the requested scope is invalid, unknown, or malformed.
     */
    Set<String> validateAccessTokenScope(ClientRegistration clientRegistration, Set<String> scope,
                                         OAuth2Request request) throws ServerException, InvalidScopeException;

    /**
     * Provided as an extension point to allow the OAuth2 provider to customise the scope requested when a refresh token
     * is requested.
     *
     * @param clientRegistration The client registration.
     * @param requestedScope The requested scope.
     * @param tokenScope The scope from the access token.
     * @param request The OAuth2 request.
     * @return The updated scope used in the remaining OAuth2 process.
     * @throws ServerException If any internal server error occurs.
     * @throws InvalidScopeException If the requested scope is invalid, unknown, or malformed.
     */
    Set<String> validateRefreshTokenScope(ClientRegistration clientRegistration, Set<String> requestedScope,
                                          Set<String> tokenScope, OAuth2Request request)
            throws ServerException, InvalidScopeException;

    /**
     * Gets the resource owners information based on an issued access token or request.
     *
     * @param clientRegistration The client registration.
     * @param token The access token.
     * @param request The OAuth2 request.
     * @return The claims for the resource owner's information.
     * @throws ServerException If any internal server error occurs.
     * @throws UnauthorizedClientException If the client's authorization fails.
     * @throws NotFoundException If the realm does not have an OAuth 2.0 provider service.
     */
    UserInfoClaims getUserInfo(ClientRegistration clientRegistration, AccessToken token, OAuth2Request request)
            throws ServerException, UnauthorizedClientException, NotFoundException;

    /**
     * Gets the specified access token's information.
     *
     * @param accessToken The access token.
     * @return A {@code Map<String, Object>} of the access token's information.
     * @throws ServerException If any internal server error occurs.
     */
    Map<String, Object> evaluateScope(AccessToken accessToken) throws ServerException;

    /**
     * Provided as an extension point to allow the OAuth2 provider to return additional data from an authorization
     * request.
     *
     * @param tokens The tokens that will be returned from the authorization call.
     * @param request The OAuth2 request.
     * @return A {@code Map<String, String>} of the additional data to return.
     * @throws ServerException If any internal server error occurs.
     */
    Map<String, String> additionalDataToReturnFromAuthorizeEndpoint(Map<String, Token> tokens,
                                                                    OAuth2Request request) throws ServerException;

    /**
     * Provided as an extension point to allow the OAuth2 provider to return additional data from an access token
     * request.
     * <br/>
     * Any additional data to be returned should be added to the access token by invoking,
     * AccessToken#addExtraData(String, String).
     *
     * @param accessToken The access token.
     * @param request The OAuth2 request.
     * @throws ServerException If any internal server error occurs.
     * @throws InvalidClientException If either the request does not contain the client's id or the client fails to be
     *          authenticated.
     * @throws NotFoundException If the realm does not have an OAuth 2.0 provider service.
     */
    void additionalDataToReturnFromTokenEndpoint(AccessToken accessToken, OAuth2Request request)
            throws ServerException, InvalidClientException, NotFoundException;

    /**
     * Saves the resource owner's consent for the granting authorization for the specified client with the specified
     * scope.
     *
     * @param resourceOwner The resource owner.
     * @param clientId The client id.
     * @param scope The requested scope.
     */
    void saveConsent(ResourceOwner resourceOwner, String clientId, Set<String> scope);

    /**
     * Revokes the resource owner's consent for the granting authorization for the specified client.
     *
     * @param userId The user id.
     * @param clientId The client id.
     */
    void revokeConsent(String userId, String clientId);

    /**
     * Whether the OAuth2 provider should issue refresh tokens when issuing access tokens.
     *
     * @return {@code true} if refresh tokens should be issued.
     * @throws ServerException If any internal server error occurs.
     */
    boolean issueRefreshTokens() throws ServerException;

    /**
     * Whether the OAuth2 provider should issue refresh tokens when refreshing access tokens.
     *
     * @return {@code true} if refresh tokens should be issued when access tokens are refreshed.
     * @throws ServerException If any internal server error occurs.
     */
    boolean issueRefreshTokensOnRefreshingToken() throws ServerException;

    /**
     * Gets the lifetime an authorization code will have before it expires.
     *
     * @return The lifetime of an authorization code in seconds.
     * @throws ServerException If any internal server error occurs.
     */
    long getAuthorizationCodeLifetime() throws ServerException;

    /**
     * Gets the lifetime an access token will have before it expires.
     *
     * @return The lifetime of an access token in seconds.
     * @throws ServerException If any internal server error occurs.
     */
    long getAccessTokenLifetime() throws ServerException;

    /**
     * Gets the lifetime an OpenID token will have before it expires.
     *
     * @return The lifetime of an OpenID token in seconds.
     * @throws ServerException If any internal server error occurs.
     */
    long getOpenIdTokenLifetime() throws ServerException;

    /**
     * Gets the lifetime an refresh token will have before it expires.
     *
     * @return The lifetime of an refresh token in seconds.
     * @throws ServerException If any internal server error occurs.
     */
    long getRefreshTokenLifetime() throws ServerException;

    /**
     * Gets the signing key pair of the OAuth2 provider.
     *
     * @param algorithm The signing algorithm.
     * @return The KeyPair.
     * @throws ServerException If any internal server error occurs.
     */
    KeyPair getSigningKeyPair(JwsAlgorithm algorithm) throws ServerException;

    /**
     * Gets the attributes of the resource owner that are used for authenticating resource owners.
     *
     * @return A {@code Set} of resource owner attributes.
     * @throws ServerException If any internal server error occurs.
     */
    Set<String> getResourceOwnerAuthenticatedAttributes() throws ServerException;

    /**
     * Gets the supported claims for this provider.
     *
     * @return A {@code Set} of the supported claims.
     * @throws ServerException If any internal server error occurs.
     */
    Set<String> getSupportedClaims() throws ServerException;

    /**
     * Gets the supported claims for this provider as strings with pipe-separated translations.
     *
     * @return A {@code Set} of the supported claims.
     * @throws ServerException If any internal server error occurs.
     */
    Set<String> getSupportedClaimsWithTranslations() throws ServerException;

    /**
     * Gets the supported scopes for this provider without translations.
     *
     * @return A {@code Set} of the supported scopes.
     * @throws ServerException If any internal server error occurs.
     */
    Set<String> getSupportedScopes() throws ServerException;

    /**
     * Gets the supported scopes for this provider.
     *
     * @return A {@code Set} of the supported scopes.
     * @throws ServerException If any internal server error occurs.
     */
    Set<String> getSupportedScopesWithTranslations() throws ServerException;

    /**
     * Gets the default set of scopes to give a client registering with this provider.
     *
     * @return A {@code Set} of the default scopes.
     * @throws ServerException If any internal server error occurs.
     */
    Set<String> getDefaultScopes() throws ServerException;

    /**
     * Gets the algorithms that the OAuth2 provider supports for signing OpenID tokens.
     *
     * @return A {@code Set} of the supported algorithms.
     * @throws ServerException If any internal server error occurs.
     */
    Set<String> getSupportedIDTokenSigningAlgorithms() throws ServerException;

    /**
     * Gets the algorithms that the OAuth2 provider supports for encryptin OpenID tokens.
     *
     * @return A {@code Set} of the supported algorithms.
     * @throws ServerException If any internal server error occurs.
     */
    Set<String> getSupportedIDTokenEncryptionAlgorithms() throws ServerException;

    /**
     * Gets the encryption methods that the OAuth2 provider supports for encryptin OpenID tokens.
     *
     * @return A {@code Set} of the supported algorithms.
     * @throws ServerException If any internal server error occurs.
     */
    Set<String> getSupportedIDTokenEncryptionMethods() throws ServerException;

    /**
     * Gets the supported version of the OpenID Connect specification.
     *
     * @return The OpenID Connect version.
     */
    String getOpenIDConnectVersion();

    /**
     * Gets the JWK Set for this OAuth2 Authorization /OpenID Provider.
     *
     * @return The JWK Set of signing and encryption keys.
     */
    JsonValue getJWKSet() throws ServerException;

    /**
     * Gets the created timestamp attribute name.
     *
     * @return The created attribute timestamp attribute name.
     */
    String getCreatedTimestampAttributeName() throws ServerException;

    /**
     * Gets the modified timestamp attribute name.
     *
     * @return The modified attribute timestamp attribute name.
     */
    String getModifiedTimestampAttributeName() throws ServerException;

    /**
     * Gets the subject types supported by the OAuth2 provider.
     *
     * @return A {@code Set} of supported subject types.
     * @throws ServerException If any internal server error occurs.
     */
    Set<String> getSupportedSubjectTypes() throws ServerException;

    /**
     * Indicates whether clients may register without providing an access token.
     *
     * @return true if allowed, otherwise false.
     * @throws ServerException If any internal server error occurs.
     */
    boolean isOpenDynamicClientRegistrationAllowed() throws ServerException;

    /**
     * Whether to generate access tokens for clients that register without one. Only enabled if
     * {@link #isOpenDynamicClientRegistrationAllowed()} is true.
     *
     * @return true if an access token should be generated for clients that register without one.
     * @throws ServerException If any internal server error occurs.
     */
    boolean isRegistrationAccessTokenGenerationEnabled() throws ServerException;

    /**
     * Returns a mapping from Authentication Context Class Reference (ACR) values (typically a Level of Assurance
     * value) to concrete authentication methods.
     */
    Map<String, AuthenticationMethod> getAcrMapping() throws ServerException;

    /**
     * The default Authentication Context Class Reference (ACR) values to use for authentication if none is specified
     * in the request. This is a space-separated list of values in preference order.
     */
    String getDefaultAcrValues() throws ServerException;

    /**
     * The mappings between amr values and auth module names.
     *
     * @return The mappings.
     */
    Map<String, String> getAMRAuthModuleMappings() throws ServerException;

    /**
     * Checks whether the config exists.
     *
     * @return Whether it exists.
     */
    boolean exists();

    /**
     * Returns the ResourceSetStore instance for the realm.
     *
     * @return The ResourceSetStore instance.
     */
    ResourceSetStore getResourceSetStore();

    /**
     * Returns whether this provider supports claims requested via 'claims' parameter.
     *
     * @return true or false.
     */
    boolean getClaimsParameterSupported() throws ServerException;

    /**
     * Validates that the requested claims are appropriate to be requested by the given client.
     */
    String validateRequestedClaims(String requestedClaims) throws InvalidRequestException, ServerException;

    /**
     * Returns the token_endpoint_auth_methods available for clients to register (and subsequently auth) using.
     */
    Set<String> getEndpointAuthMethodsSupported();

    /**
     * Whether or not to enforce the Code Verifier Parameter.
     *
     * @return Whether the Code Verifier option has been configured.
     * @see <a href="https://tools.ietf.org/html/draft-ietf-oauth-spop-12">PKCE</a>
     */
    boolean isCodeVerifierRequired() throws ServerException;

    /**
     * Returns the salt to use for hashing sub values upon pairwise requests.
     */
    String getHashSalt() throws ServerException;

    /**
     * Whether to always add claims to id_tokens - non-spec compliant.
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ScopeClaims">OpenID Connect Specification</a>
     */
    boolean isAlwaysAddClaimsToToken() throws ServerException;

    /**
     * The attribute that can be used to obtain a UI-displayable name for a user's AMIdentity.
     */
    String getUserDisplayNameAttribute() throws ServerException;

    /**
     * Gets the JSON Web Key Set URI.
     *
     * @return The JWKS URI.
     * @throws ServerException If any internal server error occurs.
     */
    String getJWKSUri() throws ServerException;

    /**
     * Gets the custom login url template which will create the url to redirect resource owners to for authentication.
     *
     * @return The custom login url template.
     * @throws ServerException If the custom login url template setting could not be retrieved.
     */
    Template getCustomLoginUrlTemplate() throws ServerException;

    /**
     * The URL that the user will be instructed to visit to complete their OAuth 2 login and consent when using the
     * device code flow.
     *
     * @return The verification URL.
     * @throws ServerException If the setting could not be retrieved.
     */
    String getVerificationUrl() throws ServerException;

    /**
     * The URL that the user will be sent to on completion of their OAuth 2 login
     * and consent when using the device code flow.
     *
     * @return The completion URL.
     * @throws ServerException If the setting could not be retrieved.
     */
    String getCompletionUrl() throws ServerException;

    /**
     * The lifetime of the device code.
     *
     * @return The lifetime in seconds.
     * @throws ServerException If the setting could not be retrieved.
     */
    int getDeviceCodeLifetime() throws ServerException;

    /**
     * The polling interval for devices waiting for tokens when using the device code flow.
     *
     * @return The interval in seconds.
     * @throws ServerException If the setting could not be retrieved.
     */
    int getDeviceCodePollInterval() throws ServerException;

    /**
     * Whether to generate and store an ops token in CTS for this OIDC provider.
     *
     * @return <code>true</code> if ops tokens should be generated/stored in CTS.
     * @throws ServerException If the setting could not be retrieved.
     */
    boolean shouldStoreOpsTokens() throws ServerException;

    /**
     * Whether clients can opt to skip resource owner consent during authorization flows.
     *
     * @return <code>true</code> if clients are allowed to opt to skip resource owner consent.
     * @throws ServerException If the setting could not be retrieved.
     */
    boolean clientsCanSkipConsent() throws ServerException;

    /**
     * Whether OpenID Connect ID Tokens are accepted as SSOTokens in this realm or not.
     *
     * @return {@code true} if ID Tokens are accepted as SSOTokens in this realm.
     * @throws ServerException If the setting could not be retrieved.
     */
    boolean isOpenIDConnectSSOProviderEnabled() throws ServerException;
}
