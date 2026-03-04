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

import static org.wrensecurity.wrenam.authentication.modules.webauthn.WebAuthnIdentityUtils.getAttributeValue;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.text.ParseException;
import java.util.Base64;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.LoginException;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.openam.utils.IOUtils;
import org.wrensecurity.wrenam.authentication.modules.webauthn.WebAuthnChallengeProvider;
import org.wrensecurity.wrenam.authentication.modules.webauthn.WebAuthnCallbackResultParser;
import org.wrensecurity.wrenam.authentication.modules.webauthn.WebAuthnConfigManager;
import org.wrensecurity.wrenam.authentication.modules.webauthn.WebAuthnDeviceProfileManager;
import org.wrensecurity.wrenam.authentication.modules.webauthn.WebAuthnFailureReason;
import org.wrensecurity.wrenam.authentication.modules.webauthn.WebAuthnPrincipal;
import org.wrensecurity.wrenam.authentication.modules.webauthn.WebAuthnResponseHandler;

/**
 * WebAuthn registration module that lets users authenticated earlier in the chain register a device.
 */
public class WebAuthnRegistration extends AMLoginModule {

    private static final String EMPTY_SELECTION = "[Empty]";

    private static final Debug debug = Debug.getInstance(RegistrationConstants.RESOURCE_NAME);

    private final WebAuthnDeviceProfileManager webAuthnDeviceProfileManager = InjectorHolder.getInstance(
            WebAuthnDeviceProfileManager.class);

    private final WebAuthnChallengeProvider challengeProvider = InjectorHolder.getInstance(
            WebAuthnChallengeProvider.class);

    private final WebAuthnResponseHandler webAuthnResponseHandler = new WebAuthnResponseHandler();

    private final WebAuthnConfigManager webAuthnConfigManager = new WebAuthnConfigManager();

    private Map options;

    private String username;

    private String realm;

    private AMIdentity user;

    private PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions;

    private WebAuthnDeviceSettings deviceSettings;

    private int maxAuthAgeMillis;

    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        this.options = options;
        username = (String) sharedState.get(getUserKey());
        realm = DNMapper.orgNameToRealmName(getRequestOrg());
        user = IdUtils.getIdentity(username, realm);
        maxAuthAgeMillis = CollectionHelper.getIntMapAttr(
                options, RegistrationConstants.MAX_AUTH_AGE, 300000, debug);
        setAuthLevel(CollectionHelper.getIntMapAttr(options, RegistrationConstants.AUTHENTICATION_LEVEL, 0, debug));
    }

    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {
        if (user == null) {
            throw failure(WebAuthnFailureReason.NOT_ALLOWED, "Registration requires authenticated user", null);
        }
        enforceRecentAuthentication();

        switch (state) {
        case ISAuthConstants.LOGIN_START:
            return startRegistration();
        case RegistrationConstants.STATE_VALIDATE_SCRIPT_OUTPUT:
            return validateScriptOutput(callbacks);
        case RegistrationConstants.STATE_COMPLETE_REGISTRATION:
            return completeRegistration(callbacks);
        default:
            throw failure(WebAuthnFailureReason.VERIFICATION_FAILED, null, null);
        }
    }

    @Override
    public Principal getPrincipal() {
        return new WebAuthnPrincipal(username);
    }

    @Override
    public void destroyModuleState() {
        username = null;
        nullifyUsedVars();
    }

    @Override
    public void nullifyUsedVars() {
        options = null;
        realm = null;
        user = null;
        publicKeyCredentialCreationOptions = null;
        deviceSettings = null;
    }

    private int startRegistration() throws AuthLoginException {
        publicKeyCredentialCreationOptions = preparePublicKeyCredentialCreationOptions();
        replaceScriptCallback(publicKeyCredentialCreationOptions);
        return RegistrationConstants.STATE_VALIDATE_SCRIPT_OUTPUT;
    }

    private int validateScriptOutput(Callback[] callbacks) throws AuthLoginException {
        String hiddenValueCallbackValue = ((HiddenValueCallback) callbacks[
                RegistrationConstants.VALIDATE_SCRIPT_OUTPUT_HIDDEN_VALUE_CALLBACK_INDEX]).getValue();
        boolean hasError = ((ConfirmationCallback) callbacks[
                RegistrationConstants.VALIDATE_SCRIPT_OUTPUT_CONFIRMATION_CALLBACK_INDEX]).getSelectedIndex() == 1;
        WebAuthnCallbackResultParser.Result callbackResult =
                WebAuthnCallbackResultParser.parse(hiddenValueCallbackValue, hasError);
        if (!callbackResult.isSuccess()) {
            throw failure(callbackResult.getFailureReason(), callbackResult.getFailureMessage(), null);
        }
        final String origin = normalizeOrigin();
        try {
            deviceSettings = webAuthnResponseHandler.handleRegistrationResponse(publicKeyCredentialCreationOptions,
                    origin, callbackResult.getCredentialJson());
        } catch (AuthLoginException e) {
            throw failure(WebAuthnFailureReason.fromAuthErrorCode(e.getErrorCode()), e.getMessage(), e);
        }
        return RegistrationConstants.STATE_COMPLETE_REGISTRATION;
    }

    private int completeRegistration(Callback[] callbacks) throws AuthLoginException {
        if (deviceSettings == null) {
            throw failure(WebAuthnFailureReason.VERIFICATION_FAILED, "Registration device response missing", null);
        }
        boolean renameRequested = ((ConfirmationCallback) callbacks[
                RegistrationConstants.COMPLETE_REGISTRATION_CONFIRMATION_CALLBACK_INDEX]).getSelectedIndex() == 0;
        if (renameRequested) {
            String friendlyName = ((NameCallback) callbacks[
                    RegistrationConstants.COMPLETE_REGISTRATION_NAME_CALLBACK_INDEX]).getName();
            if (friendlyName != null && !friendlyName.isBlank()) {
                deviceSettings.setDeviceName(friendlyName.trim());
            }
        }
        try {
            webAuthnDeviceProfileManager.saveDeviceProfile(username, realm, deviceSettings);
        } catch (IOException e) {
            throw failure(WebAuthnFailureReason.VERIFICATION_FAILED, "Failed to persist credential", e);
        }
        return ISAuthConstants.LOGIN_SUCCEED;
    }

    private PublicKeyCredentialCreationOptions preparePublicKeyCredentialCreationOptions() throws AuthLoginException {
        try {
            final String displayNameAttr = webAuthnConfigManager.getUserDisplayNameAttribute(realm);
            final String userIdAttr = webAuthnConfigManager.getUserIdAttribute(realm);
            final String displayName;
            final String userId;
            userId = getAttributeValue(user, userIdAttr);
            if (userId == null || userId.isBlank()) {
                throw new AuthLoginException(RegistrationConstants.RESOURCE_NAME, "missingUserIdAttribute", null);
            }
            displayName = getAttributeValue(user, displayNameAttr);
            String authenticatorAttachment = CollectionHelper.getMapAttr(options,
                    RegistrationConstants.AUTHENTICATOR_ATTACHMENT);
            return new PublicKeyCredentialCreationOptions.Builder()
                    .attestation(CollectionHelper.getMapAttr(options, RegistrationConstants.ATTESTATION))
                    .authenticatorAttachment(
                            EMPTY_SELECTION.equals(authenticatorAttachment) ? null : authenticatorAttachment)
                    .residentKey(CollectionHelper.getMapAttr(options, RegistrationConstants.RESIDENT_KEY))
                    .userVerification(CollectionHelper.getMapAttr(options, RegistrationConstants.USER_VERIFICATION))
                    .challenge(challengeProvider.generateChallenge())
                    .excludeCredentials(webAuthnDeviceProfileManager.getDeviceProfiles(username, realm))
                    .rpId(CollectionHelper.getMapAttr(options, RegistrationConstants.RP_ID))
                    .rpName(CollectionHelper.getMapAttr(options, RegistrationConstants.RP_NAME))
                    .timeout(CollectionHelper.getIntMapAttr(options, RegistrationConstants.TIMEOUT, 60000, debug))
                    .userId(userId.getBytes(StandardCharsets.UTF_8))
                    .userName(username)
                    .displayName((displayName == null || displayName.isBlank()) ? username : displayName)
                    .build();
        } catch (AuthLoginException e) {
            throw failure(WebAuthnFailureReason.fromAuthErrorCode(e.getErrorCode()), e.getMessage(), e);
        } catch (SMSException e) {
            throw failure(WebAuthnFailureReason.VERIFICATION_FAILED, "Failed reading WebAuthn service config", e);
        } catch (IOException | IdRepoException | SSOException | IllegalArgumentException e) {
            throw failure(WebAuthnFailureReason.VERIFICATION_FAILED, "Failed building registration options", e);
        }
    }

    private ScriptTextOutputCallback prepareScriptCallback(
            PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions) throws AuthLoginException {
        String script;
        try {
            String scriptTemplate = IOUtils.readStream(getClass().getClassLoader()
                    .getResourceAsStream(RegistrationConstants.CREDENTIALS_CREATE_SCRIPT_TEMPLATE_NAME));
            String publicKey = publicKeyCredentialCreationOptions.toJson().toString();
            String publicKeyEncoded = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(publicKey.getBytes(StandardCharsets.UTF_8));
            script = scriptTemplate.replace("{publicKeyB64}", publicKeyEncoded);
        } catch (IOException e) {
            throw new AuthLoginException(RegistrationConstants.RESOURCE_NAME, "failedPreparingScriptCallback", null, e);
        }
        return new ScriptTextOutputCallback(script);
    }

    private void replaceScriptCallback(PublicKeyCredentialCreationOptions options) throws AuthLoginException {
        final ScriptTextOutputCallback scriptCallback = prepareScriptCallback(options);
        replaceCallback(
                RegistrationConstants.STATE_VALIDATE_SCRIPT_OUTPUT,
                RegistrationConstants.VALIDATE_SCRIPT_OUTPUT_SCRIPT_CALLBACK_INDEX,
                scriptCallback);
    }

    private String normalizeOrigin() throws AuthLoginException {
        try {
            return webAuthnConfigManager.normalizeConfiguredOrigin(
                    CollectionHelper.getMapAttr(options, RegistrationConstants.RP_ORIGIN),
                    RegistrationConstants.RESOURCE_NAME);
        } catch (AuthLoginException e) {
            throw failure(WebAuthnFailureReason.fromAuthErrorCode(e.getErrorCode()), e.getMessage(), e);
        }
    }

    private void enforceRecentAuthentication() throws AuthLoginException {
        if (maxAuthAgeMillis <= 0) {
            return;
        }
        String authInstant = getUserSessionProperty(ISAuthConstants.AUTH_INSTANT);
        if (authInstant == null || authInstant.isBlank()) {
            throw failure(WebAuthnFailureReason.NOT_ALLOWED, "Recent authentication required", null);
        }
        try {
            long age = System.currentTimeMillis() - DateUtils.stringToDate(authInstant).getTime();
            if (age > maxAuthAgeMillis) {
                throw failure(WebAuthnFailureReason.NOT_ALLOWED, "Recent authentication required", null);
            }
        } catch (ParseException e) {
            throw failure(WebAuthnFailureReason.NOT_ALLOWED, "Recent authentication required", e);
        }
    }

    private AuthLoginException failure(WebAuthnFailureReason reason, String detail, Throwable cause) {
        if (username != null) {
            setFailureID(username);
        }
        StringBuilder logLine = new StringBuilder("WebAuthn registration failure; reason=").append(reason.name())
                .append(", realm=").append(realm);
        if (username != null && !username.isBlank()) {
            logLine.append(", user=").append(username);
        }
        if (detail != null && !detail.isBlank()) {
            logLine.append(", detail=").append(detail);
        }
        if (cause != null) {
            debug.warning(logLine.toString(), cause);
        } else {
            debug.warning(logLine.toString());
        }
        return new AuthLoginException(
                RegistrationConstants.RESOURCE_NAME,
                reason.messageKey(),
                null,
                cause);
    }

}
