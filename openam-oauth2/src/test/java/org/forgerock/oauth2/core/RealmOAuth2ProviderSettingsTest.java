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
 * Copyright 2021 Wren Security.
 */

package org.forgerock.oauth2.core;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotEquals;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Map;
import org.forgerock.json.jose.jwk.KeyUse;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class RealmOAuth2ProviderSettingsTest {

    @DataProvider
    public Object[][] bigEndianTestData() {
        return new Object[][]{
                {new BigInteger("00100000", 2)},
                {new BigInteger("01000000", 2)},
                {new BigInteger("10000000", 2)},
                {new BigInteger("0010000000000000", 2)},
                {new BigInteger("0100000000000000", 2)},
                {new BigInteger("1000000000000000", 2)},
        };
    }

    /**
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-6.3.1.1">RFC 7518</a>
     */
    @Test(dataProvider = "bigEndianTestData")
    public void shouldEncodeModulusWithoutLeadingExtraZeroByte(final BigInteger modulus) throws ServerException {
        RSAPublicKey publicKey = mock(RSAPublicKey.class);
        when(publicKey.getModulus()).thenReturn(modulus);
        when(publicKey.getPublicExponent()).thenReturn(BigInteger.ONE);

        Map<String, Object> jwk = RealmOAuth2ProviderSettings.createRSAJWK("alias", publicKey, KeyUse.SIG, "RS256");

        byte[] n = Base64.getDecoder().decode(jwk.get("n").toString());
        assertNotEquals(n[0], Byte.valueOf("0"));
    }
}
