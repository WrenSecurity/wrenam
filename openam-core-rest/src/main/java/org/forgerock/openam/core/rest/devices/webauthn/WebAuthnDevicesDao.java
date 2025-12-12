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
package org.forgerock.openam.core.rest.devices.webauthn;

import javax.inject.Inject;
import javax.inject.Named;
import org.forgerock.openam.core.rest.devices.UserDevicesDao;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorDeviceServiceFactory;
import org.forgerock.openam.core.rest.devices.services.webauthn.WebAuthnService;
import org.forgerock.openam.core.rest.devices.services.webauthn.WebAuthnServiceFactory;

/**
 * Dao for handling the retrieval and saving of a user's WebAuthn devices.
 */
public class WebAuthnDevicesDao extends UserDevicesDao<WebAuthnService> {

    /**
     * Construct a new WebAuthnDevicesDao.
     *
     * @param serviceFactory factory used to retrieve the WebAuthnService for this dao
     */
    @Inject
    public WebAuthnDevicesDao(@Named(WebAuthnServiceFactory.FACTORY_NAME)
            AuthenticatorDeviceServiceFactory<WebAuthnService> serviceFactory) {
        super(serviceFactory);
    }

}
