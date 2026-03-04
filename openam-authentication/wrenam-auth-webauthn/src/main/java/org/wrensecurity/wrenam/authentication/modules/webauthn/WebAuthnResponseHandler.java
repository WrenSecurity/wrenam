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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.wrensecurity.wrenam.authentication.modules.webauthn.Constants.RESOURCE_NAME;
import static org.wrensecurity.wrenam.authentication.modules.webauthn.WebAuthnIdentityUtils.getUsernameForUserId;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.sm.SMSException;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.exception.DataConversionException;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticatorTransport;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.attestation.AttestationObject;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.CollectedClientData;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.extension.authenticator.RegistrationExtensionAuthenticatorOutput;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.verifier.exception.BadOriginException;
import com.webauthn4j.verifier.exception.BadRpIdException;
import com.webauthn4j.verifier.exception.CrossOriginException;
import com.webauthn4j.verifier.exception.VerificationException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.wrensecurity.wrenam.authentication.modules.webauthn.registration.PublicKeyCredentialCreationOptions;

/**
 * Handle WebAuthn credential responses received during WebAuthn registration and authentication ceremonies.
 */
public class WebAuthnResponseHandler {

    private final WebAuthnManager webAuthnManager;

    private final WebAuthnDeviceProfileManager webAuthnDeviceProfileManager;

    private final WebAuthnConfigManager webAuthnConfigManager;

    private final ObjectConverter objectConverter;

    private final ObjectMapper objectMapper;

    /**
     * Create a new {@code WebAuthnResponseHandler} instance with default components.
     *
     * <p>Uses a non-strict {@link WebAuthnManager} and default instances of
     * {@link WebAuthnDeviceProfileManager}, {@link WebAuthnConfigManager}, {@link ObjectConverter},
     * and {@link ObjectMapper}.
     */
    public WebAuthnResponseHandler() {
        this(WebAuthnManager.createNonStrictWebAuthnManager(),
                InjectorHolder.getInstance(WebAuthnDeviceProfileManager.class),
                new WebAuthnConfigManager(),
                new ObjectConverter(), new ObjectMapper());
    }

    /**
     * Create a new {@code WebAuthnResponseHandler} with the provided dependencies.
     *
     * @param webAuthnManager the WebAuthn4J manager used for verification
     * @param webAuthnDeviceProfileManager the device profile manager used to persist and retrieve credentials
     * @param objectConverter the converter for JSON and CBOR objects
     * @param objectMapper the Jackson object mapper for JSON parsing
     */
    public WebAuthnResponseHandler(WebAuthnManager webAuthnManager,
            WebAuthnDeviceProfileManager webAuthnDeviceProfileManager,
            WebAuthnConfigManager webAuthnConfigManager,
            ObjectConverter objectConverter, ObjectMapper objectMapper) {
        this.webAuthnManager = webAuthnManager;
        this.webAuthnDeviceProfileManager = webAuthnDeviceProfileManager;
        this.webAuthnConfigManager = webAuthnConfigManager;
        this.objectConverter = objectConverter;
        this.objectMapper = objectMapper;
    }

    private static byte[] decodeBase64String(String src) {
        return Base64.getUrlDecoder().decode(src);
    }

    static void assertChallengeNotExpired(long challengeIssuedAtMillis, int timeoutMillis)
            throws AuthLoginException {
        if (timeoutMillis <= 0) {
            return;
        }
        if (challengeIssuedAtMillis <= 0) {
            throw new AuthLoginException(RESOURCE_NAME, "challengeExpired", null);
        }
        long ageMillis = System.currentTimeMillis() - challengeIssuedAtMillis;
        if (ageMillis > timeoutMillis) {
            throw new AuthLoginException(RESOURCE_NAME, "challengeExpired", null);
        }
    }

    static void assertSignCountProgression(long oldCounter, long newCounter) throws AuthLoginException {
        if (newCounter > 0 && oldCounter > 0 && newCounter <= oldCounter) {
            // Potential cloned credential per WebAuthn guidance
            throw new AuthLoginException(RESOURCE_NAME, "assertionVerificationFailed", null);
        }
    }

    private void verifyRegistrationData(PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions,
            String origin,
            RegistrationData registrationData) throws DataConversionException, VerificationException, AssertionError {
        ServerProperty serverProperty = createServerProperty(
                origin,
                publicKeyCredentialCreationOptions.getRpId(),
                publicKeyCredentialCreationOptions.getChallenge());
        List<PublicKeyCredentialParameters> pubKeyCredParams = publicKeyCredentialCreationOptions.getPubKeyCredParams()
                .stream().map(param -> new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY,
                        COSEAlgorithmIdentifier.create(param.get("alg").asInteger()))).collect(Collectors.toList());
        boolean userVerificationRequired = "required".equals(publicKeyCredentialCreationOptions.getUserVerification());
        RegistrationParameters registrationParameters = new RegistrationParameters(serverProperty, pubKeyCredParams,
                userVerificationRequired);
        webAuthnManager.verify(registrationData, registrationParameters);
    }

    private ServerProperty createServerProperty(String origin, String rpId, byte[] challengeBytes) {
        Challenge challenge = () -> challengeBytes;
        return new ServerProperty(Origin.create(origin), rpId, challenge);
    }

    private AuthLoginException mapVerificationFailure(String defaultErrorCode, Throwable e) {
        if (e instanceof BadOriginException || e instanceof CrossOriginException) {
            return new AuthLoginException(RESOURCE_NAME, "originMismatch", null, e);
        }
        if (e instanceof BadRpIdException) {
            return new AuthLoginException(RESOURCE_NAME, "rpIdMismatch", null, e);
        }
        return new AuthLoginException(RESOURCE_NAME, defaultErrorCode, null, e);
    }

    /**
     * Handle a WebAuthn registration response.
     *
     * <p>Parse and verify the registration response JSON, validate the attestation data,
     * and construct a {@link WebAuthnDeviceSettings} object that represents the registered credential.
     *
     * @param publicKeyCredentialCreationOptions the registration request options originally sent to the client
     * @param origin the request origin
     * @param response the JSON response returned from the authenticator
     * @return a {@link WebAuthnDeviceSettings} containing the registered credential data
     * @throws AuthLoginException if parsing or verification fails
     */
    public WebAuthnDeviceSettings handleRegistrationResponse(
            PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions, String origin, String response)
            throws AuthLoginException {
        assertChallengeNotExpired(publicKeyCredentialCreationOptions.getChallengeIssuedAtMillis(),
                publicKeyCredentialCreationOptions.getTimeout());

        JsonNode root;
        RegistrationData registrationData;

        try {
            root = objectMapper.readTree(response);
            registrationData = webAuthnManager.parseRegistrationResponseJSON(response);
        } catch (JsonProcessingException e) {
            throw new AuthLoginException(RESOURCE_NAME, "registrationParseFailed", null, e);
        }

        try {
            verifyRegistrationData(publicKeyCredentialCreationOptions, origin, registrationData);
        } catch (DataConversionException | VerificationException | AssertionError e) {
            throw mapVerificationFailure("registrationVerificationFailed", e);
        }

        AuthenticatorData<RegistrationExtensionAuthenticatorOutput> authenticatorData =
                registrationData.getAttestationObject().getAuthenticatorData();
        AttestedCredentialData attestedCredentialData =
                authenticatorData.getAttestedCredentialData();
        return new WebAuthnDeviceSettings(attestedCredentialData.getCredentialId(),
                attestedCredentialData.getAaguid().getBytes(),
                objectConverter.getCborConverter().writeValueAsBytes(attestedCredentialData.getCOSEKey()),
                authenticatorData.getSignCount(),
                registrationData.getTransports().stream().map(AuthenticatorTransport::getValue).toArray(String[]::new),
                authenticatorData.isFlagBE(), authenticatorData.isFlagBS(),
                decodeBase64String(root.at("/response/attestationObject").asText()),
                decodeBase64String(root.at("/response/clientDataJSON").asText()));
    }

    /**
     * Handle a WebAuthn authentication response.
     *
     * <p>Parse and verify the assertion response JSON, validate the authenticator data and signature,
     * update the stored credential counter, and resolve the authenticated username.
     *
     * @param publicKeyCredentialRequestOptions the original authentication request options
     * @param origin the request origin
     * @param response the JSON response returned from the authenticator
     * @param username the username provided during authentication (may be {@code null} for resident keys)
     * @param realm the realm of the authenticated user
     * @return the resolved username associated with the credential
     * @throws AuthLoginException if verification or user resolution fails
     */
    public String handleAuthenticationResponse(PublicKeyCredentialRequestOptions publicKeyCredentialRequestOptions,
            String origin, String response, String username, String realm) throws AuthLoginException {
        assertChallengeNotExpired(publicKeyCredentialRequestOptions.getChallengeIssuedAtMillis(),
                publicKeyCredentialRequestOptions.getTimeout());

        AuthenticationData authenticationData;
        try {
            authenticationData = webAuthnManager.parseAuthenticationResponseJSON(response);
        } catch (RuntimeException e) {
            throw new AuthLoginException(RESOURCE_NAME, "assertionVerificationFailed", null, e);
        }

        byte[] credentialId = authenticationData.getCredentialId();
        if (credentialId == null || credentialId.length == 0) {
            throw new AuthLoginException(RESOURCE_NAME, "missingCredentialId", null);
        }

        // Resolve username when usernameless (resident key) is used
        String resolvedUsername = null;
        if (username == null) {
            byte[] userHandle = authenticationData.getUserHandle();
            if (userHandle == null || userHandle.length == 0) {
                throw new AuthLoginException(RESOURCE_NAME, "missingUserHandle", null);
            }
            final String userIdAttr;
            try {
                userIdAttr = webAuthnConfigManager.getUserIdAttribute(realm);
            } catch (SMSException | SSOException e) {
                throw new AuthLoginException(RESOURCE_NAME, "userIdAttrLookupFailed", null, e);
            }
            try {
                resolvedUsername = getUsernameForUserId(userHandle, userIdAttr, realm);
            } catch (InternalServerErrorException e) {
                throw new AuthLoginException(RESOURCE_NAME, "userLookupFailed", null, e);
            }
        }
        String effectiveUser = (resolvedUsername != null) ? resolvedUsername : username;

        WebAuthnDeviceSettings device;
        try {
            device = webAuthnDeviceProfileManager.getDeviceProfile(effectiveUser, realm, credentialId);
        } catch (IOException e) {
            throw new AuthLoginException(RESOURCE_NAME, "deviceLookupFailed", null, e);
        }
        if (device == null) {
            throw new AuthLoginException(RESOURCE_NAME, "unknownCredential", null);
        }

        ServerProperty serverProperty = createServerProperty(
                origin,
                publicKeyCredentialRequestOptions.getRpId(),
                publicKeyCredentialRequestOptions.getChallenge());

        AttestationObject attestationObject = objectConverter.getCborConverter()
                .readValue(device.getAttestationObject(), AttestationObject.class);
        CollectedClientData clientData = objectConverter.getJsonConverter()
                .readValue(new String(device.getAttestationClientDataJSON(), UTF_8), CollectedClientData.class);
        Set<AuthenticatorTransport> transports = (device.getTransports() == null)
                ? Collections.emptySet()
                : Arrays.stream(device.getTransports()).map(AuthenticatorTransport::create).collect(Collectors.toSet());
        CredentialRecord credentialRecord = new CredentialRecordImpl(
                attestationObject.getAttestationStatement(),
                null,
                device.isBackupEligible(),
                device.isBackupState(),
                device.getSignCount(),
                attestationObject.getAuthenticatorData().getAttestedCredentialData(),
                attestationObject.getAuthenticatorData().getExtensions(),
                clientData,
                null,
                transports);
        boolean uvRequired = "required".equals(publicKeyCredentialRequestOptions.getUserVerification());
        List<byte[]> allowCredentials = Collections.singletonList(device.getCredentialId());
        AuthenticationParameters params = new AuthenticationParameters(serverProperty, credentialRecord,
                allowCredentials, uvRequired);

        try {
            webAuthnManager.verify(authenticationData, params);
        } catch (DataConversionException | VerificationException | AssertionError e) {
            throw mapVerificationFailure("assertionVerificationFailed", e);
        }

        long newCounter = authenticationData.getAuthenticatorData().getSignCount();
        long oldCounter = device.getSignCount();
        assertSignCountProgression(oldCounter, newCounter);
        if (newCounter > oldCounter) {
            device.setSignCount(newCounter);
            device.setBackupState(authenticationData.getAuthenticatorData().isFlagBS());
            try {
                webAuthnDeviceProfileManager.updateDeviceProfile(effectiveUser, realm, device);
            } catch (IOException e) {
                throw new AuthLoginException(RESOURCE_NAME, "counterPersistFailed", null, e);
            }
        }
        return resolvedUsername;
    }

}
