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
 * Copyright 2023 Wren Security
 */
package org.wrensecurity.wrenam.authentication.modules.duo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * {@link DuoAuthorizationResolver} test case.
 */
public class DuoAuthorizationResolverTest {

    private static final String SECRET_KEY = "Zh5eGmUq9zpfQnyUIu5OL9iWoMMv5ZNmk3zLJ4Ep";

    private static DuoAuthorizationResolver resolver;

    @BeforeAll
    public static void setup() {
        resolver = new DuoAuthorizationResolver(SECRET_KEY);
    }

    @Test
    public void testResolvePassword() {
        String date = "Tue, 21 Aug 2012 17:29:18 -0000";
        String method = "POST";
        String host = "api-XXXXXXXX.duosecurity.com";
        String path = "/auth/v2/auth";
        String body = "device=auto&factor=push&hostname=wks01&ipaddr=10.2.3.4&username=narroway";
        String expectedPassword = "8fe4d3186932175914dd8e97ae4b8cf3ad4653f1";
        assertEquals(expectedPassword, resolver.resolvePassword(date, method, host, path, body));
        // Check whether resolver correctly performs reset
        assertEquals(expectedPassword, resolver.resolvePassword(date, method, host, path, body));
    }

}
