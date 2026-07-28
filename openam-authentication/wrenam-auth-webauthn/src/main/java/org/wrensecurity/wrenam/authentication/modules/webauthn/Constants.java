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

/**
 * Constants for the WebAuthn authentication module.
 */
public final class Constants {

    private Constants() {
    }

    static final String RESOURCE_NAME = "amAuthWebAuthn";

    /**
     * Module configuration key that whether to prompt the user for a username if the module is used
     * without previous authentication.
     */
    static final String USERNAMELESS = "wrensec-am-auth-webauthn-usernameless";

    /**
     * Module configuration key that defines the relying party’s requirement
     * for user verification during credential authentication.
     */
    static final String USER_VERIFICATION = "wrensec-am-auth-webauthn-user-verification";

    /**
     * Module configuration key for the Relying Party (RP) identifier which scopes WebAuthn
     * credentials to a specific application or domain.
     */
    static final String RP_ID = "wrensec-am-auth-webauthn-rp-id";

    /**
     * Module configuration key for the Relying Party (RP) origin.
     */
    static final String RP_ORIGIN = "wrensec-am-auth-webauthn-origin";

    /**
     * Module configuration key for the WebAuthn authentication timeout.
     */
    static final String TIMEOUT = "wrensec-am-auth-webauthn-timeout";

    /**
     * Module configuration key for the authentication level associated with this authentication module instance.
     */
    static final String AUTHENTICATION_LEVEL = "wrensec-am-auth-webauthn-auth-level";

    /**
     * Resource name of credentials get script template.
     */
    static final String CREDENTIALS_GET_SCRIPT_TEMPLATE_NAME = "webauthn_credentials_get.js";

    static final int STATE_PROMPT_USERNAME = 2;

    static final int STATE_VALIDATE_SCRIPT_OUTPUT = 3;

    static final int VALIDATE_SCRIPT_OUTPUT_HIDDEN_VALUE_CALLBACK_INDEX = 0;

    static final int VALIDATE_SCRIPT_OUTPUT_SCRIPT_CALLBACK_INDEX = 1;

    static final int VALIDATE_SCRIPT_OUTPUT_CONFIRMATION_CALLBACK_INDEX = 2;

    static final int STATE_PROMPT_USERNAME_NAME_CALLBACK_INDEX = 0;

}
