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

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;

/**
 * Cisco Duo authorization password resolver.
 */
public class DuoAuthorizationResolver {

    private final static String SIGN_ALGORITHM = "HmacSHA1";

    private final Mac mac;

    public DuoAuthorizationResolver(String secretKey) {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), SIGN_ALGORITHM);
        try {
            this.mac = Mac.getInstance(SIGN_ALGORITHM);
            this.mac.init(secretKeySpec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize MAC provider.", e);
        }
    }

    /**
     * Resolve authorization password for request with the specified parameters.
     * @param date Date in RFC 2822 format. Never null.
     * @param method HTTP method (uppercase). Never null.
     * @param host Cisco DUO API hostname (lowercase). Never null.
     * @param path API method's path. Never null.
     * @param params HTTP query params for GET request, request BODY for POST request. Can be null.
     * @return
     */
    public String resolvePassword(String date, String method, String host, String path, String params) {
        String request = String.join("\n", date, method, host, path, params);
        byte[] raw = this.mac.doFinal(request.getBytes(StandardCharsets.UTF_8));
        this.mac.reset();
        return Hex.encodeHexString(raw);
    }

}
