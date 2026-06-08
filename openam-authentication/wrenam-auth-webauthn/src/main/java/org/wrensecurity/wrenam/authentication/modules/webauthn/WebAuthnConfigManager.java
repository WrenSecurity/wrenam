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
 * Copyright 2025 Wren Security. All rights reserved.
 */
package org.wrensecurity.wrenam.authentication.modules.webauthn;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.util.Map;
import java.util.Set;
import org.forgerock.openam.core.rest.devices.services.webauthn.WebAuthnService;

/**
 * Resolves WebAuthn service configuration used by authentication and registration modules.
 */
public class WebAuthnConfigManager {

    private static final String USER_ID_KEY = "wrensec-am-auth-webauthn-user-id-attr";

    private static final String DEFAULT_USER_ID_KEY = "entryUUID";

    private static final String USER_DISPLAY_NAME_KEY = "wrensec-am-auth-webauthn-user-display-name-attr";

    private static final String DEFAULT_USER_DISPLAY_NAME_KEY = "cn";

    private ServiceConfig getServiceConfig(String realm) throws SMSException, SSOException {
        ServiceConfigManager scm = new ServiceConfigManager(
                AccessController.doPrivileged(AdminTokenAction.getInstance()),
                WebAuthnService.SERVICE_NAME,
                WebAuthnService.SERVICE_VERSION);
        return scm.getOrganizationConfig(realm, null);
    }

    public String getUserIdAttribute(String realm) throws SMSException, SSOException {
        ServiceConfig config = getServiceConfig(realm);
        Map<String, Set<String>> attrs = config.getAttributes();
        return CollectionHelper.getMapAttr(attrs, USER_ID_KEY, DEFAULT_USER_ID_KEY);
    }

    public String getUserDisplayNameAttribute(String realm) throws SMSException, SSOException {
        ServiceConfig config = getServiceConfig(realm);
        Map<String, Set<String>> attrs = config.getAttributes();
        return CollectionHelper.getMapAttr(attrs, USER_DISPLAY_NAME_KEY, DEFAULT_USER_DISPLAY_NAME_KEY);
    }

    public String normalizeConfiguredOrigin(String configuredOrigin, String resourceName)
            throws AuthLoginException {
        if (configuredOrigin == null || configuredOrigin.isBlank()) {
            throw new AuthLoginException(resourceName, "missingOriginConfig", null);
        }
        try {
            URI origin = new URI(configuredOrigin.trim());
            if (origin.getScheme() == null || origin.getHost() == null
                    || origin.getPath() != null && !origin.getPath().isEmpty() && !"/".equals(origin.getPath())
                    || origin.getQuery() != null
                    || origin.getFragment() != null) {
                throw new AuthLoginException(resourceName, "invalidOriginConfig", null);
            }
            StringBuilder normalized = new StringBuilder()
                    .append(origin.getScheme().toLowerCase())
                    .append("://")
                    .append(origin.getHost().toLowerCase());
            if (origin.getPort() >= 0) {
                normalized.append(':').append(origin.getPort());
            }
            return normalized.toString();
        } catch (URISyntaxException e) {
            throw new AuthLoginException(resourceName, "invalidOriginConfig", null, e);
        }
    }

}
