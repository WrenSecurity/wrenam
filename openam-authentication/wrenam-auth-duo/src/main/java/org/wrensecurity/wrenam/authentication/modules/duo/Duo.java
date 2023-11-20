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
 * Copyright 2023 Wren Security
 */
package org.wrensecurity.wrenam.authentication.modules.duo;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import org.apache.commons.lang3.StringUtils;

/**
 * Cisco Duo authentication module.
 */
public class Duo extends AMLoginModule {

    /**
     * Initial authentication module state.
     */
    private static final int INIT_DUO_AUTH_STATE = 1;

    /**
     * State representing pending authentication status verification.
     */
    private static final int VERIFY_DUO_AUTH_STATUS_STATE = 2;

    /**
     * Attribute indicating whether Cisco Duo authentication should be skipped.
     */
    private static final String SKIP_DUO_MFA_ATTR = "skipDuoMFA";

    public static final String SERVICE_KEY = "amAuthDuo";

    private final Debug debug;
    private Map<String, Set<String>> config;
    private Map<String, ?> sharedState;
    private DuoClient duoClient;
    private String username;
    private String transactionId;
    private CompletableFuture<DuoAuthStatus> statusRequest;

    public Duo() {
        this.debug = Debug.getInstance(SERVICE_KEY);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void init(Subject subject, Map state, Map options) {
        sharedState = state;
        config = (Map<String, Set<String>>) options;
        duoClient = new DuoClient(DuoClientConfig.fromOptions(config));
        username = (String) sharedState.get(getUserKey());
    }

    @Override
    public int process(Callback[] callbacks, int state) throws AuthLoginException {
        // Check SKIP PROCESSING configuration
        if (INIT_DUO_AUTH_STATE == state && shouldSkipProcessing()) {
            debug.message("Skipping Cisco Duo authentication module.");
            return ISAuthConstants.LOGIN_IGNORE;
        }
        // Verify the user is known
        if (StringUtils.isBlank(username)) {
            throw new AuthLoginException(SERVICE_KEY, "missingUsername", null);
        }
        switch (state) {
            case INIT_DUO_AUTH_STATE:
                // Initialize authentication process
                debug.message("Initializing Cisco Duo authentication for user '" + username + "'.");
                try {
                    transactionId = duoClient.initAuth(username);
                } catch (Exception e) {
                    throw new AuthLoginException(SERVICE_KEY, "authFailed", null, e);
                }
            case VERIFY_DUO_AUTH_STATUS_STATE:
                // Verify authentication status
                if (StringUtils.isBlank(transactionId)) {
                    throw new AuthLoginException(SERVICE_KEY, "missingTransactionId", null);
                }
                debug.message("Checking Cisco Duo authentication status for transaction '" + transactionId + "'.");
                if (statusRequest == null) {
                    try {
                        statusRequest = duoClient.getAuthStatus(transactionId);
                    } catch (Exception e) {
                        throw new AuthLoginException(SERVICE_KEY, "authFailed", null, e);
                    }
                }
                DuoAuthStatus status;
                try {
                    status = statusRequest.get(2, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    return VERIFY_DUO_AUTH_STATUS_STATE;
                } catch (Exception e) {
                    throw new AuthLoginException(SERVICE_KEY, "authFailed", null, e);
                }
                if (DuoAuthStatus.WAITING == status) {
                    statusRequest = null;
                    return VERIFY_DUO_AUTH_STATUS_STATE;
                } else if (DuoAuthStatus.ALLOW == status) {
                    return ISAuthConstants.LOGIN_SUCCEED;
                }
                throw new AuthLoginException(SERVICE_KEY, "authDenied", null);
            default:
                debug.error("Invalid state '" + state + "' in Cisco Duo authentication process.");
                setFailureID(username);
                throw new AuthLoginException("Invalid state '" + state + "' in Cisco Duo authentication process.");
        }
    }

    @Override
    public Principal getPrincipal() {
        return new DuoPrincipal(username);
    }

    @Override
    public void destroyModuleState() {
        username = null;
        nullifyUsedVars();
    }

    @Override
    public void nullifyUsedVars() {
        config = null;
        sharedState = null;
        duoClient = null;
        transactionId = null;
        statusRequest = null;
    }

    /**
     * Determine whether the Cisco Duo authentication should be skipped for the checked user.
     */
    private boolean shouldSkipProcessing() {
        return sharedState.containsKey(SKIP_DUO_MFA_ATTR) && (Boolean) sharedState.get(SKIP_DUO_MFA_ATTR);
    }

}
