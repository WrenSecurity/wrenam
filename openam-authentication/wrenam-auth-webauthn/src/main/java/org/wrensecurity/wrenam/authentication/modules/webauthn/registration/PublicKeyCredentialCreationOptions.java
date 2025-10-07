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

import static org.forgerock.openam.utils.StringUtils.isBlank;

import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.openam.utils.JsonArray;
import org.forgerock.openam.utils.JsonObject;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.util.Reject;

/**
 * Represent options for creating a {@code PublicKeyCredential} via the WebAuthn API.
 */
public class PublicKeyCredentialCreationOptions {

    private static final Set<String> ATTESTATION_VALUES = Set.of("none", "indirect", "direct", "enterprise");

    private static final Set<String> AUTHENTICATOR_ATTACHMENT_VALUES = Set.of("platform", "cross-platform");

    private static final Set<String> RESIDENT_KEY_VALUES = Set.of("discouraged", "preferred", "required");

    private static final Set<String> USER_VERIFICATION_VALUES = Set.of("required", "preferred", "discouraged");

    private final String attestation;

    private final String authenticatorAttachment;

    private final String residentKey;

    private final String userVerification;

    private final byte[] challenge;

    private final List<JsonValue> pubKeyCredParams;

    private final List<WebAuthnDeviceSettings> excludeCredentials;

    private final String rpId;

    private final String rpName;

    private final int timeout;

    private final byte[] userId;

    private final String userName;

    private final String displayName;

    private PublicKeyCredentialCreationOptions(Builder builder) {
        this.attestation = builder.attestation;
        this.authenticatorAttachment = builder.authenticatorAttachment;
        this.residentKey = builder.residentKey;
        this.userVerification = builder.userVerification;
        this.challenge = builder.challenge;
        this.pubKeyCredParams = builder.pubKeyCredParams;
        this.excludeCredentials = builder.excludeCredentials;
        this.rpId = builder.rpId;
        this.rpName = builder.rpName;
        this.timeout = builder.timeout;
        this.userId = builder.userId;
        this.userName = builder.userName;
        this.displayName = builder.displayName;
    }

    public static class Builder {

        private String attestation;

        private String authenticatorAttachment;

        private String residentKey;

        private String userVerification;

        private byte[] challenge;

        private List<JsonValue> pubKeyCredParams;

        private List<WebAuthnDeviceSettings> excludeCredentials;

        private String rpId;

        private String rpName;

        private int timeout;

        private byte[] userId;

        private String userName;

        private String displayName;

        /**
         * Set the attestation conveyance preference.
         *
         * @param attestation one of {@code none}, {@code direct}, {@code indirect}, or {@code enterprise}
         * @return this builder instance
         * @see <a href="https://www.w3.org/TR/webauthn-3/#enum-attestation-convey">
         *     §5.4.7. Attestation Conveyance Preference Enumeration</a>
         */
        public Builder attestation(String attestation) {
            this.attestation = attestation;
            return this;
        }

        /**
         * Set the authenticator attachment modality.
         *
         * @param authenticatorAttachment one of {@code platform} or {@code cross-platform}
         * @return this builder instance
         * @see <a href="https://www.w3.org/TR/webauthn-3/#dom-publickeycredential-authenticatorattachment">
         *     §5.1. PublicKeyCredential Interface</a>
         */
        public Builder authenticatorAttachment(String authenticatorAttachment) {
            this.authenticatorAttachment = authenticatorAttachment;
            return this;
        }

        /**
         * Set the relying party’s resident key preference.
         *
         * @param residentKey one of {@code discouraged}, {@code preferred}, or {@code required}
         * @return this builder instance
         * @see <a href="https://www.w3.org/TR/webauthn-3/#enum-residentKeyRequirement">
         *     §5.4.6. Resident Key Requirement Enumeration</a>
         */
        public Builder residentKey(String residentKey) {
            this.residentKey = residentKey;
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
         * Set the challenge to be used during credential creation.
         *
         * @param challenge a cryptographically random byte array, at least 16 bytes long
         * @return this builder instance
         * @see <a href="https://www.w3.org/TR/webauthn-3/#dom-publickeycredentialcreationoptions-challenge">
         *     §5.4. Options for Credential Creation</a>
         */
        public Builder challenge(byte[] challenge) {
            this.challenge = challenge;
            return this;
        }

        /**
         * Set the list of supported public key credential parameters.
         *
         * @param pubKeyCredParams a list of key types and algorithm identifiers
         * @return this builder instance
         */
        public Builder pubKeyCredParams(List<JsonValue> pubKeyCredParams) {
            this.pubKeyCredParams = pubKeyCredParams;
            return this;
        }

        /**
         * Set credentials that should be excluded from creation.
         *
         * @param excludeCredentials list of existing credentials to exclude
         * @return this builder instance
         * @see <a href="https://www.w3.org/TR/webauthn-3/#dom-publickeycredentialcreationoptions-excludecredentials">
         *     §5.4. Options for Credential Creation</a>
         */
        public Builder excludeCredentials(List<WebAuthnDeviceSettings> excludeCredentials) {
            this.excludeCredentials = excludeCredentials;
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
         * Set the Relying Party display name.
         *
         * @param rpName a human-palatable name of the relying party
         * @return this builder instance
         */
        public Builder rpName(String rpName) {
            this.rpName = rpName;
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
         * Set the user handle.
         *
         * @param userId an opaque user handle with a maximum size of 64 bytes, not meant to be displayed to the user
         * @return this builder instance
         * @see <a href="https://www.w3.org/TR/webauthn-3/#dom-publickeycredentialuserentity-id">
         *     §5.4.3. User Account Parameters for Credential Generation</a>
         */
        public Builder userId(byte[] userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Set the user’s name.
         *
         * @param userName user identifier
         * @return this builder instance
         */
        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        /**
         * Set the user’s display name.
         *
         * @param displayName a human-palatable name for the user account, intended only for display
         * @return this builder instance
         * @see <a href="https://www.w3.org/TR/webauthn-3/#dom-publickeycredentialuserentity-displayname">
         *     §5.4.3. User Account Parameters for Credential Generation</a>
         */
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public PublicKeyCredentialCreationOptions build() {
            if (userId == null || userId.length == 0) {
                throw new IllegalArgumentException("userId is required");
            }
            if (userId.length > 64) {
                throw new IllegalArgumentException("userId length exceeds 64 bytes");
            }
            Reject.ifTrue(isBlank(userName), "userName is required");
            Reject.ifTrue(isBlank(displayName), "displayName is required");
            Reject.ifTrue(isBlank(rpName), "rpName is required");
            Reject.ifTrue(isBlank(rpId), "rpId is required");
            if (challenge == null || challenge.length < 16) {
                throw new IllegalArgumentException("challenge must be at least 16 bytes");
            }
            if (timeout < 0) {
                throw new IllegalArgumentException("timeout must be at least 0");
            }
            if (attestation != null && !ATTESTATION_VALUES.contains(attestation)) {
                throw new IllegalArgumentException("Invalid attestation: " + attestation);
            }
            if (authenticatorAttachment != null
                    && !AUTHENTICATOR_ATTACHMENT_VALUES.contains(authenticatorAttachment)) {
                throw new IllegalArgumentException("Invalid authenticatorAttachment: " + authenticatorAttachment);
            }
            if (residentKey != null && !RESIDENT_KEY_VALUES.contains(residentKey)) {
                throw new IllegalArgumentException("Invalid residentKey: " + residentKey);
            }
            if (userVerification != null && !USER_VERIFICATION_VALUES.contains(userVerification)) {
                throw new IllegalArgumentException("Invalid userVerification: " + userVerification);
            }
            if (pubKeyCredParams == null || pubKeyCredParams.isEmpty()) {
                // Include broad defaults per spec recommendation
                // @see https://www.w3.org/TR/webauthn-3/#dom-publickeycredentialcreationoptions-pubkeycredparams
                pubKeyCredParams = Stream.of(-7, -8, -257)
                        .map(alg -> JsonValueBuilder.jsonValue()
                                .put("type", "public-key")
                                .put("alg", alg)
                                .build())
                        .collect(Collectors.toList());
            } else {
                for (JsonValue pubKeyCredParam : pubKeyCredParams) {
                    String type = pubKeyCredParam.get("type").asString();
                    if (!"public-key".equals(type)) {
                        throw new IllegalArgumentException("pubKeyCredParam type must be public-key");
                    }
                    if (!pubKeyCredParam.isDefined("alg") || !pubKeyCredParam.get("alg").isNumber()) {
                        throw new IllegalArgumentException("pubKeyCredParam alg must be a integer");
                    }
                }
            }
            return new PublicKeyCredentialCreationOptions(this);
        }

    }

    /**
     * Get the attestation conveyance preference.
     *
     * @return one of {@code none}, {@code direct}, {@code indirect}, or {@code enterprise}
     * @see <a href="https://www.w3.org/TR/webauthn-3/#enum-attestation-convey">
     *     §5.4.7. Attestation Conveyance Preference Enumeration</a>
     */
    public String getAttestation() {
        return attestation;
    }

    /**
     * Get the authenticator attachment modality.
     *
     * @return one of {@code platform} or {@code cross-platform}
     * @see <a href="https://www.w3.org/TR/webauthn-3/#dom-publickeycredential-authenticatorattachment">
     *     §5.1. PublicKeyCredential Interface</a>
     */
    public String getAuthenticatorAttachment() {
        return authenticatorAttachment;
    }

    /**
     * Get the relying party’s resident key preference.
     *
     * @return one of {@code discouraged}, {@code preferred}, or {@code required}
     * @see <a href="https://www.w3.org/TR/webauthn-3/#enum-residentKeyRequirement">
     *     §5.4.6. Resident Key Requirement Enumeration</a>
     */
    public String getResidentKey() {
        return residentKey;
    }

    /**
     * Get the user verification requirement.
     *
     * @return one of {@code required}, {@code preferred}, or {@code discouraged}
     * @see <a href="https://www.w3.org/TR/webauthn-3/#enum-userVerificationRequirement">
     *     §5.8.6. User Verification Requirement Enumeration</a>
     */
    public String getUserVerification() {
        return userVerification;
    }

    /**
     * Get the challenge to be used during credential creation.
     *
     * @return a cryptographically random byte array, at least 16 bytes long
     * @see <a href="https://www.w3.org/TR/webauthn-3/#dom-publickeycredentialcreationoptions-challenge">
     *     §5.4. Options for Credential Creation</a>
     */
    public byte[] getChallenge() {
        return challenge;
    }

    /**
     * Get the list of supported public key credential parameters.
     *
     * @return a list of key types and algorithm identifiers
     */
    public List<JsonValue> getPubKeyCredParams() {
        return pubKeyCredParams;
    }

    /**
     * Get credentials that should be excluded from creation.
     *
     * @return list of existing credentials to exclude
     * @see <a href="https://www.w3.org/TR/webauthn-3/#dom-publickeycredentialcreationoptions-excludecredentials">
     *     §5.4. Options for Credential Creation</a>
     */
    public List<WebAuthnDeviceSettings> getExcludeCredentials() {
        return excludeCredentials;
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
     * Get the Relying Party display name.
     *
     * @return a human-palatable name of the relying party
     */
    public String getRpName() {
        return rpName;
    }

    /**
     * Get the timeout hint, in milliseconds.
     *
     * @return timeout value (≥ 0)
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Get the user handle.
     *
     * @return an opaque user handle with a maximum size of 64 bytes, not meant to be displayed to the user
     * @see <a href="https://www.w3.org/TR/webauthn-3/#dom-publickeycredentialuserentity-id">
     *     §5.4.3. User Account Parameters for Credential Generation</a>
     */
    public byte[] getUserId() {
        return userId;
    }

    /**
     * Get the user’s name.
     *
     * @return user identifier
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Get the user’s display name.
     *
     * @return a human-palatable name for the user account, intended only for display
     * @see <a href="https://www.w3.org/TR/webauthn-3/#dom-publickeycredentialuserentity-displayname">
     *     §5.4.3. User Account Parameters for Credential Generation</a>
     */
    public String getDisplayName() {
        return displayName;
    }

    private String encodeBytes(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    /**
     * Serializes to a JSON value suitable for use as {@code publicKey} inside
     * {@code navigator.credentials.create({ publicKey: ... })}.
     */
    public JsonValue toJson() throws IOException {
        JsonObject publicKey = JsonValueBuilder.jsonValue();
        JsonObject rp = JsonValueBuilder.jsonValue()
                .put("name", rpName)
                .put("id", rpId);
        publicKey.put("rp", rp.build());
        JsonObject user = JsonValueBuilder.jsonValue()
                .put("id", encodeBytes(userId))
                .put("name", userName)
                .put("displayName", displayName);
        publicKey.put("user", user.build());
        publicKey.put("challenge", encodeBytes(challenge));
        JsonArray pubKeyCredParams = publicKey.array("pubKeyCredParams");
        for (JsonValue param : this.pubKeyCredParams) {
            pubKeyCredParams.add(param);
        }
        pubKeyCredParams.build();
        if (timeout > 0) {
            publicKey.put("timeout", timeout);
        }
        if (excludeCredentials != null && !excludeCredentials.isEmpty()) {
            JsonArray exclude = publicKey.array("excludeCredentials");
            for (WebAuthnDeviceSettings credential : excludeCredentials) {
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
                exclude.add(jsonObject.build());
            }
            exclude.build();
        }
        boolean hasAuthenticatorSelection =
                authenticatorAttachment != null ||
                        residentKey != null ||
                        userVerification != null;
        if (hasAuthenticatorSelection) {
            JsonObject authenticatorSelection = JsonValueBuilder.jsonValue();
            if (authenticatorAttachment != null) {
                authenticatorSelection.put("authenticatorAttachment", authenticatorAttachment);
            }
            if (residentKey != null) {
                authenticatorSelection.put("residentKey", residentKey);
            }
            if ("required".equals(residentKey)) {
                authenticatorSelection.put("requireResidentKey", true);
            }
            if (userVerification != null) {
                authenticatorSelection.put("userVerification", userVerification);
            }
            publicKey.put("authenticatorSelection", authenticatorSelection.build());
        }
        if (attestation != null) {
            publicKey.put("attestation", attestation);
        }
        return publicKey.build();
    }

}
