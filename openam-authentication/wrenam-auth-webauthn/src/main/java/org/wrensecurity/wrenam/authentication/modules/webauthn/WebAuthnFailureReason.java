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
 * Copyright 2026 Wren Security. All rights reserved.
 */
package org.wrensecurity.wrenam.authentication.modules.webauthn;

import java.util.Locale;

/**
 * WebAuthn failure reason used for user-facing messaging and failure routing.
 */
public enum WebAuthnFailureReason {

    USER_CANCELLED,

    UNSUPPORTED,

    NO_AUTHENTICATOR,

    NOT_ALLOWED,

    CREDENTIAL_NOT_FOUND,

    NO_REGISTERED_CREDENTIALS,

    CHALLENGE_EXPIRED,

    ORIGIN_MISMATCH,

    RP_ID_MISMATCH,

    VERIFICATION_FAILED;

    public String messageKey() {
        return "failureReason." + name();
    }

    public static WebAuthnFailureReason fromReasonCode(String reasonCode) {
        if (reasonCode == null || reasonCode.isBlank()) {
            return VERIFICATION_FAILED;
        }
        try {
            return WebAuthnFailureReason.valueOf(reasonCode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return VERIFICATION_FAILED;
        }
    }

    public static WebAuthnFailureReason fromAuthErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return VERIFICATION_FAILED;
        }
        switch (errorCode) {
        case "noRegisteredCredentials":
            return NO_REGISTERED_CREDENTIALS;
        case "challengeExpired":
            return CHALLENGE_EXPIRED;
        case "originMismatch":
        case "missingOriginConfig":
        case "invalidOriginConfig":
            return ORIGIN_MISMATCH;
        case "rpIdMismatch":
            return RP_ID_MISMATCH;
        case "missingUserHandle":
        case "missingCredentialId":
        case "unknownCredential":
        case "userLookupFailed":
        case "missingUserIdAttribute":
            return CREDENTIAL_NOT_FOUND;
        case "unauthenticated":
        case "authenticatorError":
        case "registrationAuthenticatorError":
            return NOT_ALLOWED;
        default:
            return VERIFICATION_FAILED;
        }
    }

}
