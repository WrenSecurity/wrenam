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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.scripting.api;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import org.forgerock.openam.scripting.ScriptConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps an SSO token allowing the script writer to access session data.
 *
 * @since 13.0.0
 */
public final class ScriptedSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptConstants.LOGGER_NAME);

    private final SSOToken ssoToken;

    public ScriptedSession(SSOToken ssoToken) {
        this.ssoToken = ssoToken;
    }

    /**
     * Given the property name, retrieves its corresponding value.
     *
     * @param name
     *         session property name
     *
     * @return property value
     */
    public String getProperty(String name) {
        try {
            return ssoToken.getProperty(name);
        } catch (SSOException ssoE) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Unable to access session property " + name, ssoE);
            }
            return null;
        }
    }

}
