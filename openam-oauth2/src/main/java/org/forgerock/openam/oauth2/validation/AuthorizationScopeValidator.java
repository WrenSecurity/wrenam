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
package org.forgerock.openam.oauth2.validation;

import static org.forgerock.openam.oauth2.OAuth2Constants.AuthorizationScopes.AM_INTROSPECT_ALL_TOKENS;

import java.util.Collections;
import java.util.Set;

import com.sun.identity.sm.ServiceAttributeValidator;

/**
 * Validates OAuth2 provider scopes against scopes reserved for client authorization.
 */
public class AuthorizationScopeValidator implements ServiceAttributeValidator {

    private static final Set<String> AUTHORIZATION_SCOPES = Collections.singleton(AM_INTROSPECT_ALL_TOKENS);

    @Override
    public boolean validate(Set<String> values) {
        for (String value : values) {
            String scope = value.split("\\|", 2)[0].trim();
            for (String reservedScope : AUTHORIZATION_SCOPES) {
                if (reservedScope.equalsIgnoreCase(scope)) {
                    return false;
                }
            }
        }
        return true;
    }
}
