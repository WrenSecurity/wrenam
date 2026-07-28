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

import java.util.Arrays;
import java.util.Objects;
import org.forgerock.openam.core.rest.devices.DeviceSettings;

/**
 * Data model representation of an WebAuthn device settings.
 *
 * <p>Modeled after the
 * <a href="https://www.w3.org/TR/webauthn-3/#credential-record">credential record</a>
 * defined in the WebAuthn specification.
 */
public class WebAuthnDeviceSettings extends DeviceSettings {

    private static final String DEFAULT_CREDENTIAL_NAME = "Authenticator";

    /**
     * The <a href="https://www.w3.org/TR/webauthn-3/#credential-id">credential ID</a> of the credential.
     */
    private byte[] credentialId;

    /**
     * An <a href="https://www.w3.org/TR/webauthn-3/#aaguid">Authenticator Attestation Globally Unique Identifier</a>
     * for the device.
     */
    private byte[] aaguid;

    /**
     * A <a href="https://www.w3.org/TR/webauthn-3/#dom-publickeycredentialentity-name">human-palatable name</a>
     * for the device.
     */
    private String deviceName;

    /**
     * The <a href="https://www.w3.org/TR/webauthn-3/#credential-public-key">credential public key</a>
     * encoded in COSE_Key format, as defined in Section 7 of <ahref="https://tools.ietf.org/html/rfc8152">RFC 8152</a>.
     */
    private byte[] publicKey;

    /**
     * The latest value of the <a href="https://www.w3.org/TR/webauthn-3/#signature-counter">signature counter</a>
     * in the authenticator data from any ceremony using the public key credential source.
     */
    private long signCount;

    /**
     * The value returned from getTransports() when the public key credential source was registered.
     */
    private String[] transports;

    /**
     * The value of the BE flag when the public key credential source was created.
     */
    private boolean backupEligible;

    /**
     * The latest value of the BS flag in the authenticator data from any ceremony using the public key credential
     * source.
     */
    private boolean backupState;

    /**
     * The value of the attestationObject attribute when the public key credential source was registered.
     * Storing this enables the Relying Party to reference the credential’s attestation statement at a later time.
     */
    private byte[] attestationObject;

    /**
     * The value of the clientDataJSON attribute when the public key credential source was registered.
     * Storing this in combination with the attestationObject item enables the Relying Party
     * to re-verify the attestation signature at a later time.
     */
    private byte[] attestationClientDataJSON;

    /**
     * Default constructor for Jackson deserialization.
     */
    public WebAuthnDeviceSettings() {
        // Jackson requires a no-arg constructor
    }

    /**
     * Construct a new WebAuthnDeviceSettings object with the provided values.
     */
    public WebAuthnDeviceSettings(byte[] credentialId, byte[] aaguid, String deviceName, byte[] publicKey, long signCount,
            String[] transports, boolean backupEligible, boolean backupState, byte[] attestationObject,
            byte[] attestationClientDataJSON) {
        this.credentialId = credentialId;
        this.aaguid = aaguid;
        this.deviceName = deviceName;
        this.publicKey = publicKey;
        this.signCount = signCount;
        this.transports = transports;
        this.backupEligible = backupEligible;
        this.backupState = backupState;
        this.attestationObject = attestationObject;
        this.attestationClientDataJSON = attestationClientDataJSON;
    }

    /**
     * Construct a new WebAuthnDeviceSettings object with the provided values.
     */
    public WebAuthnDeviceSettings(byte[] credentialId, byte[] aaguid, byte[] publicKey, long signCount,
            String[] transports, boolean backupEligible, boolean backupState, byte[] attestationObject,
            byte[] attestationClientDataJSON) {
        this.credentialId = credentialId;
        this.aaguid = aaguid;
        this.deviceName = DEFAULT_CREDENTIAL_NAME;
        this.publicKey = publicKey;
        this.signCount = signCount;
        this.transports = transports;
        this.backupEligible = backupEligible;
        this.backupState = backupState;
        this.attestationObject = attestationObject;
        this.attestationClientDataJSON = attestationClientDataJSON;
    }

    public byte[] getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(byte[] credentialId) {
        this.credentialId = credentialId;
    }

    public byte[] getAAGUID() {
        return aaguid;
    }

    public void setAAGUID(byte[] aaguid) {
        this.aaguid = aaguid;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public long getSignCount() {
        return signCount;
    }

    public void setSignCount(long signCount) {
        this.signCount = signCount;
    }

    public String[] getTransports() {
        return transports;
    }

    public void setTransports(String[] transports) {
        this.transports = transports;
    }

    public boolean isBackupEligible() {
        return backupEligible;
    }

    public void setBackupEligible(boolean backupEligible) {
        this.backupEligible = backupEligible;
    }

    public boolean isBackupState() {
        return backupState;
    }

    public void setBackupState(boolean backupState) {
        this.backupState = backupState;
    }

    public byte[] getAttestationObject() {
        return attestationObject;
    }

    public void setAttestationObject(byte[] attestationObject) {
        this.attestationObject = attestationObject;
    }

    public byte[] getAttestationClientDataJSON() {
        return attestationClientDataJSON;
    }

    public void setAttestationClientDataJSON(byte[] attestationClientDataJSON) {
        this.attestationClientDataJSON = attestationClientDataJSON;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                Arrays.hashCode(credentialId),
                Arrays.hashCode(aaguid),
                deviceName,
                Arrays.hashCode(publicKey),
                signCount,
                Arrays.hashCode(transports),
                backupEligible,
                backupState,
                Arrays.hashCode(attestationObject),
                Arrays.hashCode(attestationClientDataJSON));
    }

}
