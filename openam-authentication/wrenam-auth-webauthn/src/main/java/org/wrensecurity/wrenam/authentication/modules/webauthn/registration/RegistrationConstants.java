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
package org.wrensecurity.wrenam.authentication.modules.webauthn.registration;

/**
 * Constants for the WebAuthnRegistration authentication module.
 */
public final class RegistrationConstants {

    private RegistrationConstants() {
    }

    static final String RESOURCE_NAME = "amAuthWebAuthnRegistration";

    /**
     * Module configuration key that specifies the relying party's preference
     * for how attestation statements are conveyed during credential creation.
     */
    static final String ATTESTATION = "wrensec-am-auth-webauthnregistration-attestation";

    /**
     * Module configuration key that indicates which authenticator attachment
     * type should be permitted for the chosen authenticator.
     */
    static final String AUTHENTICATOR_ATTACHMENT = "wrensec-am-auth-webauthnregistration-authenticator-attachment";

    /**
     * Module configuration key that specifies the extent to which the relying party
     * desires to create a client-side discoverable credential (resident key).
     */
    static final String RESIDENT_KEY = "wrensec-am-auth-webauthnregistration-resident-key";

    /**
     * Module configuration key that defines the relying party’s requirement
     * for user verification during credential creation.
     */
    static final String USER_VERIFICATION = "wrensec-am-auth-webauthnregistration-user-verification";

    /**
     * Module configuration key for the Relying Party (RP) identifier which scopes WebAuthn
     * credentials to a specific application or domain.
     */
    static final String RP_ID = "wrensec-am-auth-webauthnregistration-rp-id";

    /**
     * Module configuration key for the Relying Party (RP) origin.
     */
    static final String RP_ORIGIN = "wrensec-am-auth-webauthnregistration-origin";

    /**
     * Module configuration key for the Relying Party (RP) display name shown to the user upon a WebAuthn operation.
     */
    static final String RP_NAME = "wrensec-am-auth-webauthnregistration-rp-name";

    /**
     * Module configuration key for the WebAuthn registration timeout.
     */
    static final String TIMEOUT = "wrensec-am-auth-webauthnregistration-timeout";

    /**
     * Module configuration key for the authentication level associated with this authentication module instance.
     */
    static final String AUTHENTICATION_LEVEL = "wrensec-am-auth-webauthnregistration-auth-level";

    /**
     * Module configuration key for the user display name attribute name.
     */
    static final String USER_DISPLAY_NAME_ATTR = "wrensec-am-auth-webauthnregistration-user-display-name-attr";

    /**
     * Module configuration key for the user id attribute name.
     *
     * <p>MUST NOT include personally identifying information, e.g., e-mail addresses or usernames, as per
     * <a href="https://www.w3.org/TR/webauthn-3/#sctn-privacy-considerations-rp">
     *     privacy considerations for relying parties</a>.
     */
    static final String USER_ID_ATTR = "wrensec-am-auth-webauthnregistration-user-id-attr";

    /**
     * Resource name of credentials create script template.
     */
    static final String CREDENTIALS_CREATE_SCRIPT_TEMPLATE_NAME = "webauthn_credentials_create.js";

    static final int STATE_VALIDATE_SCRIPT_OUTPUT = 2;

    static final int STATE_COMPLETE_REGISTRATION = 3;

    static final int VALIDATE_SCRIPT_OUTPUT_HIDDEN_VALUE_CALLBACK_INDEX = 0;

    static final int VALIDATE_SCRIPT_OUTPUT_SCRIPT_CALLBACK_INDEX = 1;

    static final int VALIDATE_SCRIPT_OUTPUT_CONFIRMATION_CALLBACK_INDEX = 2;

    static final int COMPLETE_REGISTRATION_NAME_CALLBACK_INDEX = 0;

    static final int COMPLETE_REGISTRATION_CONFIRMATION_CALLBACK_INDEX = 1;

}
