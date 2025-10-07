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

import static org.forgerock.openam.utils.StringUtils.isBlank;

import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.openam.utils.JsonArray;
import org.forgerock.openam.utils.JsonObject;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.util.Reject;

/**
 * Represent options for requesting a {@code PublicKeyCredential} via the WebAuthn API.
 */
public class PublicKeyCredentialRequestOptions {

    // @see https://www.w3.org/TR/webauthn-3/#enum-userVerificationRequirement
    private static final Set<String> USER_VERIFICATION_VALUES = Set.of("required", "preferred", "discouraged");

    private final byte[] challenge;

    private final String rpId;

    private final int timeout;

    private final String userVerification;

    private final List<WebAuthnDeviceSettings> allowCredentials;

    private PublicKeyCredentialRequestOptions(Builder builder) {
        this.challenge = builder.challenge;
        this.rpId = builder.rpId;
        this.timeout = builder.timeout;
        this.userVerification = builder.userVerification;
        this.allowCredentials = builder.allowCredentials;
    }

    public static class Builder {

        private byte[] challenge;

        private String rpId;

        private int timeout;

        private String userVerification;

        private List<WebAuthnDeviceSettings> allowCredentials;

        /**
         * Set the challenge to be used during credential assertation.
         *
         * @param challenge a cryptographically random byte array, at least 16 bytes long
         * @return this builder instance
         * @see <a href="https://www.w3.org/TR/webauthn-3/#dom-publickeycredentialrequestoptions-challenge">
         *     §5.5. Options for Assertion Generation</a>
         */
        public Builder challenge(byte[] challenge) {
            this.challenge = challenge;
            return this;
        }

        /**
         * Set the Relying Party identifier.
         *
         * @param rpId the relying party ID (usually a domain)
         * @return this builder instance
         */
        public Builder rpId(String rpId) {
            this.rpId = rpId;
            return this;
        }

        /**
         * Set the timeout hint, in milliseconds.
         *
         * @param timeout timeout value (≥ 0)
         * @return this builder instance
         */
        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Set the user verification requirement.
         *
         * @param userVerification one of {@code required}, {@code preferred}, or {@code discouraged}
         * @return this builder instance
         * @see <a href="https://www.w3.org/TR/webauthn-3/#enum-userVerificationRequirement">
         *     §5.8.6. User Verification Requirement Enumeration</a>
         */
        public Builder userVerification(String userVerification) {
            this.userVerification = userVerification;
            return this;
        }

        /**
         * Set the list of authenticators eligible for this authentication ceremony.
         *
         * @return this builder instance
         * @see <a href="https://www.w3.org/TR/webauthn-3/#dom-publickeycredentialrequestoptions-allowcredentials">
         *     §§5.5. Options for Assertion Generation</a>
         */
        public Builder allowCredentials(List<WebAuthnDeviceSettings> allowCredentials) {
            this.allowCredentials = allowCredentials;
            return this;
        }

        public PublicKeyCredentialRequestOptions build() {
            if (challenge == null || challenge.length < 16) {
                throw new IllegalArgumentException("challenge must be at least 16 bytes");
            }
            Reject.ifTrue(isBlank(rpId), "rpId is required");
            if (timeout < 0) {
                throw new IllegalArgumentException("timeout must be at least 0");
            }
            if (userVerification != null && !USER_VERIFICATION_VALUES.contains(userVerification)) {
                throw new IllegalArgumentException("Invalid userVerification: " + userVerification);
            }
            return new PublicKeyCredentialRequestOptions(this);
        }
    }

    /**
     * Get the challenge used during credential assertation.
     *
     * @return a cryptographically random byte array, at least 16 bytes long
     * @see <a href="https://www.w3.org/TR/webauthn-3/#dom-publickeycredentialrequestoptions-challenge">
     *     §5.5. Options for Assertion Generation</a>
     */
    public byte[] getChallenge() {
        return challenge;
    }

    /**
     * Get the Relying Party identifier.
     *
     * @return the relying party ID (usually a domain)
     */
    public String getRpId() {
        return rpId;
    }

    /**
     * Get the timeout hint, in milliseconds.
     *
     * @return timeout timeout value (≥ 0)
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Get the user verification requirement.
     *
     * @return one of {@code required}, {@code preferred}, or {@code discouraged}
     */
    public String getUserVerification() {
        return userVerification;
    }

    /**
     * Get the list of authenticators eligible for this authentication ceremony.
     *
     * @return list of authenticators eligible for this authentication ceremony.
     * @see <a href="https://www.w3.org/TR/webauthn-3/#dom-publickeycredentialrequestoptions-allowcredentials">
     *     §§5.5. Options for Assertion Generation</a>
     */
    public List<WebAuthnDeviceSettings> getAllowCredentials() {
        return allowCredentials;
    }

    private String encodeBytes(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    /**
     * Serializes to a JSON value suitable for use as {@code publicKey} inside
     * {@code navigator.credentials.get({ publicKey: ... })}.
     */
    public JsonValue toJson() throws IOException {
        JsonObject publicKey = JsonValueBuilder.jsonValue();
        publicKey.put("challenge", encodeBytes(challenge));
        publicKey.put("rpId", rpId);
        if (timeout > 0) {
            publicKey.put("timeout", timeout);
        }
        if (userVerification != null) {
            publicKey.put("userVerification", userVerification);
        }
        if (allowCredentials != null && !allowCredentials.isEmpty()) {
            JsonArray allow = publicKey.array("allowCredentials");
            for (WebAuthnDeviceSettings credential : allowCredentials) {
                JsonObject jsonObject = JsonValueBuilder.jsonValue()
                        .put("type", "public-key")
                        .put("id", encodeBytes(credential.getCredentialId()));
                String[] transports = credential.getTransports();
                if (transports != null && transports.length > 0) {
                    JsonArray transportsArray = jsonObject.array("transports");
                    for (String t : transports) {
                        transportsArray.add(t);
                    }
                    transportsArray.build();
                }
                allow.add(jsonObject.build());
            }
            allow.build();
        }
        return publicKey.build();
    }
}
