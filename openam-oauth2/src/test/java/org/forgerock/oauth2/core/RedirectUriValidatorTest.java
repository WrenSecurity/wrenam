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
 * Portions Copyright 2023 Wren Security.
 */

package org.forgerock.oauth2.core;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.Set;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RedirectUriValidatorTest {

    private static final String VALID_URL = "https://wrensecurity.org";
    private static final String INVALID_URL = "https://foo.bar";

    private RedirectUriValidator validator;
    private ClientRegistration clientRegistration;

    @BeforeMethod
    public void setup() {
        validator = new RedirectUriValidator();
        clientRegistration = mock(ClientRegistration.class);
        given(clientRegistration.getRedirectUris()).willReturn(Set.of(URI.create(VALID_URL)));
    }

    @Test
    public void testValidateValidRedirectUrl() throws Exception {
        validator.validate(clientRegistration, VALID_URL, null);
    }

    @Test(expectedExceptions = { RedirectUriMismatchException.class })
    public void testValidateInvalidRedirectUrl() throws Exception {
        validator.validate(clientRegistration, INVALID_URL, null);
    }

    @Test(expectedExceptions = { InvalidRequestException.class }, expectedExceptionsMessageRegExp = "Missing parameter: redirect_uri")
    public void testValidateMissingRedirectUrlWithoutUserCode() throws Exception {
        given(clientRegistration.getRedirectUris()).willReturn(Set.of(URI.create(VALID_URL), URI.create("http://wrensecurity.org")));
        validator.validate(clientRegistration, null, null);
    }

    @Test()
    public void testValidateMissingRedirectUrlWithUserCode() throws Exception {
        validator.validate(clientRegistration, null, "foobar");
    }

}
