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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.oauth2.OAuth2Constants.AuthorizationScopes.AM_INTROSPECT_ALL_TOKENS;

import java.util.Set;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AuthorizationScopeValidatorTest {

    @DataProvider
    private Object[][] scopes() {
        return new Object[][] {
                {Set.of(), true},
                {Set.of("openid", "profile|en|Profile"), true},
                {Set.of(AM_INTROSPECT_ALL_TOKENS), false},
                {Set.of(" " + AM_INTROSPECT_ALL_TOKENS + " |en|Inspect tokens "), false},
                {Set.of("AM-INTROSPECT-ALL-TOKENS"), false},
                {Set.of("profile", AM_INTROSPECT_ALL_TOKENS), false},
        };
    }

    @Test(dataProvider = "scopes")
    public void shouldValidateProviderScopes(Set<String> scopes, boolean expected) {
        assertThat(new AuthorizationScopeValidator().validate(scopes)).isEqualTo(expected);
    }
}
