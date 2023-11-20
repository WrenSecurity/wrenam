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

import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.datastruct.ValueNotFoundException;
import java.util.Map;
import java.util.Set;

/**
 * Cisco Duo client configuration.
 */
public class DuoClientConfig {

    private static final String API_BASE_URL = "duo-client-api-base-url";
    private static final String INTEGRATION_KEY = "duo-client-integration-key";
    private static final String SECRET_KEY = "duo-client-secret-key";

    private final String apiBaseUrl;
    private final String integrationKey;
    private final String secretKey;

    public DuoClientConfig(String apiBaseUrl, String integrationKey, String secretKey) {
        this.apiBaseUrl = apiBaseUrl;
        this.integrationKey = integrationKey;
        this.secretKey = secretKey;
    }

    public static DuoClientConfig fromOptions(Map<String, Set<String>> options) {
        try {
            return new DuoClientConfig(CollectionHelper.getMapAttrThrows(options, API_BASE_URL),
                    CollectionHelper.getMapAttrThrows(options, INTEGRATION_KEY),
                    CollectionHelper.getMapAttrThrows(options, SECRET_KEY));
        } catch (ValueNotFoundException e) {
            throw new IllegalStateException("Failed to create Cisco Duo configuration.", e);
        }
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public String getIntegrationKey() {
        return integrationKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

}
