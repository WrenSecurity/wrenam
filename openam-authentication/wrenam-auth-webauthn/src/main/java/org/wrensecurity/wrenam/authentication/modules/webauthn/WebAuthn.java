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

import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.LoginException;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.openam.utils.IOUtils;

/**
 * WebAuthn authentication module.
 */
public class WebAuthn extends AMLoginModule {

    private static final Debug debug = Debug.getInstance(Constants.RESOURCE_NAME);

    protected final WebAuthnDeviceProfileManager webAuthnDeviceProfileManager =
            InjectorHolder.getInstance(WebAuthnDeviceProfileManager.class);

    private final WebAuthnChallengeProvider challengeProvider =
            InjectorHolder.getInstance(WebAuthnChallengeProvider.class);

    private final WebAuthnResponseHandler webAuthnResponseHandler = new WebAuthnResponseHandler();

    private Map options;

    private String username;

    private String realm;

    private AMIdentity user;

    private boolean usernameless;

    private PublicKeyCredentialRequestOptions publicKeyCredentialRequestOptions;

    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        this.options = options;
        username = (String) sharedState.get(getUserKey());
        realm = DNMapper.orgNameToRealmName(getRequestOrg());
        if (username != null) {
            user = IdUtils.getIdentity(username, realm);
        }
        usernameless = CollectionHelper.getBooleanMapAttr(options, Constants.USERNAMELESS, false);
        setAuthLevel(CollectionHelper.getIntMapAttr(options, Constants.AUTHENTICATION_LEVEL, 0, debug));
    }

    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {
        switch (state) {
        case ISAuthConstants.LOGIN_START:
            return startAuthentication();
        case Constants.STATE_PROMPT_USERNAME:
            return promptUsername(callbacks);
        case Constants.STATE_VALIDATE_SCRIPT_OUTPUT:
            return validateScriptOutput(callbacks);
        default:
            setFailureID(username);
            throw new AuthLoginException(Constants.RESOURCE_NAME, "authFailed", null);
        }
    }

    @Override
    public Principal getPrincipal() {
        return new WebAuthnPrincipal(username);
    }

    private int startAuthentication() throws AuthLoginException {
        // Start immediately for usernameless OR when user is already known (2FA), otherwise prompt for username
        if (usernameless || user != null) {
            publicKeyCredentialRequestOptions = preparePublicKeyCredentialRequestOptions(usernameless);
            ScriptTextOutputCallback scriptCallback = prepareScriptCallback(publicKeyCredentialRequestOptions);
            replaceCallback(
                    Constants.STATE_VALIDATE_SCRIPT_OUTPUT,
                    Constants.VALIDATE_SCRIPT_OUTPUT_SCRIPT_CALLBACK_INDEX,
                    scriptCallback);
            return Constants.STATE_VALIDATE_SCRIPT_OUTPUT;
        }
        return Constants.STATE_PROMPT_USERNAME;
    }

    private int promptUsername(Callback[] callbacks) throws AuthLoginException {
        username = ((NameCallback) callbacks[Constants.STATE_PROMPT_USERNAME_NAME_CALLBACK_INDEX]).getName();
        if (username != null) {
            user = IdUtils.getIdentity(username, realm);
        }
        publicKeyCredentialRequestOptions = preparePublicKeyCredentialRequestOptions(false);
        ScriptTextOutputCallback scriptCallback = prepareScriptCallback(publicKeyCredentialRequestOptions);
        replaceCallback(
                Constants.STATE_VALIDATE_SCRIPT_OUTPUT,
                Constants.VALIDATE_SCRIPT_OUTPUT_SCRIPT_CALLBACK_INDEX,
                scriptCallback);
        return Constants.STATE_VALIDATE_SCRIPT_OUTPUT;
    }

    private int validateScriptOutput(Callback[] callbacks) throws AuthLoginException {
        String hiddenValueCallbackValue = ((HiddenValueCallback) callbacks[
                Constants.VALIDATE_SCRIPT_OUTPUT_HIDDEN_VALUE_CALLBACK_INDEX]).getValue();
        boolean hasError = ((ConfirmationCallback) callbacks[
                Constants.VALIDATE_SCRIPT_OUTPUT_CONFIRMATION_CALLBACK_INDEX]).getSelectedIndex() == 1;
        if (hasError) {
            throw new AuthLoginException(Constants.RESOURCE_NAME, "authenticatorError", null);
        }
        HttpServletRequest request = getHttpServletRequest();
        String origin = request.getScheme() + "://" + request.getServerName() + ':' + request.getServerPort();
        String resolvedUsername = webAuthnResponseHandler.handleAuthenticationResponse(
                publicKeyCredentialRequestOptions, origin, hiddenValueCallbackValue, username, realm);
        if (resolvedUsername != null) {
            username = resolvedUsername;
            user = IdUtils.getIdentity(username, realm);
            if (user == null) {
                // userHandle present but not mapped to a directory entry
                throw new AuthLoginException(Constants.RESOURCE_NAME, "userLookupFailed", null);
            }
        }
        if (usernameless && user == null) {
            throw new AuthLoginException(Constants.RESOURCE_NAME, "missingUserHandle", null);
        }
        return ISAuthConstants.LOGIN_SUCCEED;
    }

    private PublicKeyCredentialRequestOptions preparePublicKeyCredentialRequestOptions(boolean usernameless)
            throws AuthLoginException {
        try {
            PublicKeyCredentialRequestOptions.Builder builder =
                    new PublicKeyCredentialRequestOptions.Builder()
                            .challenge(challengeProvider.generateChallenge())
                            .rpId(CollectionHelper.getMapAttr(options, Constants.RP_ID))
                            .timeout(CollectionHelper.getIntMapAttr(options, Constants.TIMEOUT, 60000, debug))
                            .userVerification(CollectionHelper.getMapAttr(options, Constants.USER_VERIFICATION));
            if (!usernameless) {
                List<WebAuthnDeviceSettings> allowCredentials =
                        webAuthnDeviceProfileManager.getDeviceProfiles(username, realm);
                builder.allowCredentials(allowCredentials);
            }
            return builder.build();
        } catch (IOException e) {
            throw new AuthLoginException(Constants.RESOURCE_NAME, "failedPreparingPublicKeyCredentialRequestOptions", null, e);
        }
    }

    private ScriptTextOutputCallback prepareScriptCallback(
            PublicKeyCredentialRequestOptions publicKeyCredentialRequestOptions) throws AuthLoginException {
        String script;
        try {
            String scriptTemplate = IOUtils.readStream(
                    getClass().getClassLoader().getResourceAsStream(Constants.CREDENTIALS_GET_SCRIPT_TEMPLATE_NAME));
            String publicKey = publicKeyCredentialRequestOptions.toJson().toString();
            script = scriptTemplate.replace("{publicKey}", publicKey);
        } catch (IOException e) {
            throw new AuthLoginException(Constants.RESOURCE_NAME, "failedPreparingScriptCallback", null, e);
        }
        return new ScriptTextOutputCallback(script);
    }

}
