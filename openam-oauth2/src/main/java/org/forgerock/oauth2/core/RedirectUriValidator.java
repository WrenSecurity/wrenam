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
 * Copyright 2014-2015 ForgeRock AS.
 * Portions Copyright 2023 Wren Security.
 */

package org.forgerock.oauth2.core;

import java.net.URI;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;

/**
 * Validates that the redirect uri on the request matches against the client's registered redirect uris.
 *
 * @since 12.0.0
 */
public class RedirectUriValidator {

    /**
     * Validates that the requested redirect uri matches against one of the pre-registered redirect uris on the
     * client's registration.
     *
     * @param clientRegistration The client's registration.
     * @param redirectUri The redirect uri.
     * @param userCode The user code.
     * @throws InvalidRequestException If the request is missing any required parameters or is otherwise malformed.
     * @throws RedirectUriMismatchException If the redirect uri on the request does not match the redirect uri
     *          registered for the client.
     */
    public void validate(ClientRegistration clientRegistration, String redirectUri, String userCode) throws InvalidRequestException,
            RedirectUriMismatchException {
        // Check if redirect URI is mandatory
        if (Utils.isEmpty(redirectUri)) {
            if (userCode == null && clientRegistration.getRedirectUris().size() != 1) {
                throw new InvalidRequestException("Missing parameter: redirect_uri");
            }
            return;
        }
        // Check redirect URI validity
        URI uri = URI.create(redirectUri);
        if (uri.getFragment() != null || !uri.isAbsolute() || !clientRegistration.getRedirectUris().contains(uri)) {
            throw new RedirectUriMismatchException();
        }
    }
}
