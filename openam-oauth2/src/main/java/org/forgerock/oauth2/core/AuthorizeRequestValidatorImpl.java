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
 * Copyright 2014-2017 ForgeRock AS.
 * Portions Copyright 2023 Wren Security.
 */

package org.forgerock.oauth2.core;

import static org.forgerock.openam.oauth2.OAuth2Constants.DeviceCode.USER_CODE;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.CLIENT_ID;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.REDIRECT_URI;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.RESPONSE_TYPE;

import com.sun.identity.shared.debug.Debug;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;

/**
 * Implementation of the request validator for the OAuth2 authorize endpoint.
 *
 * @since 12.0.0
 */
@Singleton
public class AuthorizeRequestValidatorImpl implements AuthorizeRequestValidator {

    private static final Debug debug = Debug.getInstance("OAuth2Provider");
    private final ClientRegistrationStore clientRegistrationStore;
    private final RedirectUriValidator redirectUriValidator;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final ResponseTypeValidator responseTypeValidator;

    /**
     * Constructs a new AuthorizeRequestValidatorImpl instance.
     *
     * @param clientRegistrationStore An instance of the ClientRegistrationStore.
     * @param redirectUriValidator An instance of the RedirectUriValidator.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param responseTypeValidator An instance of the ResponseTypeValidator.
     */
    @Inject
    public AuthorizeRequestValidatorImpl(ClientRegistrationStore clientRegistrationStore,
            RedirectUriValidator redirectUriValidator, OAuth2ProviderSettingsFactory providerSettingsFactory,
            ResponseTypeValidator responseTypeValidator) {
        this.clientRegistrationStore = clientRegistrationStore;
        this.redirectUriValidator = redirectUriValidator;
        this.providerSettingsFactory = providerSettingsFactory;
        this.responseTypeValidator = responseTypeValidator;
    }

    @Override
    public void validateRequest(OAuth2Request request) throws InvalidClientException, InvalidRequestException,
            RedirectUriMismatchException, UnsupportedResponseTypeException, ServerException, NotFoundException {
        // Validate client identifier
        if (Utils.isEmpty(request.<String>getParameter(CLIENT_ID))) {
            String errMsg = "Missing parameter, " + CLIENT_ID;
            debug.error(errMsg);
            throw new InvalidRequestException(errMsg);
        }
        ClientRegistration clientRegistration = clientRegistrationStore.get(request.<String>getParameter(CLIENT_ID), request);
        // Validate redirect URL
        redirectUriValidator.validate(clientRegistration, request.<String>getParameter(REDIRECT_URI),
                request.<String>getParameter(USER_CODE));
        // Validate response type
        if (Utils.isEmpty(request.<String>getParameter(RESPONSE_TYPE))) {
            String errMsg = "Missing parameter, " + RESPONSE_TYPE;
            debug.error(errMsg);
            throw new InvalidRequestException(errMsg);
        }
        responseTypeValidator.validate(clientRegistration, Utils.splitResponseType(
                request.<String>getParameter(RESPONSE_TYPE)), providerSettingsFactory.get(request), request);
    }
}
