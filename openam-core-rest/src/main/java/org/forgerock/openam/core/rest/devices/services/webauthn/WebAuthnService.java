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
package org.forgerock.openam.core.rest.devices.services.webauthn;

import com.iplanet.sso.SSOException;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.openam.core.rest.devices.DeviceSerialisation;
import org.forgerock.openam.core.rest.devices.services.DeviceService;
import org.forgerock.openam.core.rest.devices.services.EncryptedDeviceStorage;

public class WebAuthnService extends EncryptedDeviceStorage implements DeviceService {

    /**
     * Name of this service for reference purposes.
     */
    public static final String SERVICE_NAME = "WebAuthn";

    /**
     * Version of this service.
     */
    public static final String SERVICE_VERSION = "1.0";

    private static final String DEBUG_LOCATION = "amAuthWebAuthn";

    private static final String WEBAUTHN_PROFILES_ATTR = "wrensec-am-auth-webauthn-profiles-attr-name";

    private static final String WEBAUTHN_USER_ID_ATTR = "wrensec-am-auth-webauthn-user-id-attr";

    private static final String WEBAUTHN_ENCRYPTION_SCHEME =
            "wrensec-am-auth-webauthn-device-settings-encryption-scheme";

    private static final String WEBAUTHN_KEYSTORE_FILE =
            "wrensec-am-auth-webauthn-device-settings-encryption-keystore";

    private static final String WEBAUTHN_KEYSTORE_TYPE =
            "wrensec-am-auth-webauthn-device-settings-encryption-keystore-type";

    private static final String WEBAUTHN_KEYSTORE_PASSWORD =
            "wrensec-am-auth-webauthn-device-settings-encryption-keystore-password";

    private static final String WEBAUTHN_KEYSTORE_KEYPAIR_ALIAS =
            "wrensec-am-auth-webauthn-device-settings-encryption-keypair-alias";

    private static final String WEBAUTHN_KEYSTORE_PRIVATEKEY_PASSWORD =
            "wrensec-am-auth-webauthn-device-settings-encryption-privatekey-password";

    /**
     * Construct a new WebAuthnService.
     *
     * @param serviceConfigManager used to communicate with the config store
     * @param realm the realm in which to look up the WebAuthn service
     * @throws SMSException if there were error communicating with the SMS
     * @throws SSOException if there were invalid privileges to perform the requested operation
     */
    protected WebAuthnService(ServiceConfigManager serviceConfigManager, String realm)
            throws SMSException, SSOException {
        super(serviceConfigManager, realm, DEBUG_LOCATION);
    }

    @Override
    public String getConfigStorageAttributeName() {
        return CollectionHelper.getMapAttr(options, WEBAUTHN_PROFILES_ATTR);
    }

    public String getUserIdAttributeName() {
        return CollectionHelper.getMapAttr(options, WEBAUTHN_USER_ID_ATTR);
    }

    @Override
    public DeviceSerialisation getDeviceSerialisationStrategy() {
        return getDeviceSerialisationStrategy(
                WEBAUTHN_ENCRYPTION_SCHEME,
                WEBAUTHN_KEYSTORE_FILE,
                WEBAUTHN_KEYSTORE_PASSWORD,
                WEBAUTHN_KEYSTORE_TYPE,
                WEBAUTHN_KEYSTORE_KEYPAIR_ALIAS,
                WEBAUTHN_KEYSTORE_PRIVATEKEY_PASSWORD);
    }

}
