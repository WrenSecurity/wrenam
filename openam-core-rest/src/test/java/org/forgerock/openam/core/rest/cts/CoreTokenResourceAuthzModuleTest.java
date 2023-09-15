/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2016 ForgeRock AS.
 * Portions Copyright 2023 Wren Security
 */

package org.forgerock.openam.core.rest.cts;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

public class CoreTokenResourceAuthzModuleTest {

    Config<SessionService> mockConfig = mock(Config.class);
    SessionService mockService = mock(SessionService.class);
    CoreTokenConfig mockCoreTokenConfig = mock(CoreTokenConfig.class);
    Debug mockDebug = mock(Debug.class);
    private SSOTokenContext mockSSOTokenContext;

    @BeforeTest
    public void beforeTest() {
        given(mockConfig.get()).willReturn(mockService);
    }

    @BeforeMethod
    public void setup() {
        mockSSOTokenContext = mock(SSOTokenContext.class);
        given(mockSSOTokenContext.asContext(SSOTokenContext.class)).willReturn(mockSSOTokenContext);
    }

    @AfterMethod
    public void cleanup() {
        mockSSOTokenContext = null;
    }

    @Test
    public void shouldBlockAllAccessIfResourceDisabled() throws Exception {

        //given
        given(mockCoreTokenConfig.isCoreTokenResourceEnabled()).willReturn(false);
        CoreTokenResourceAuthzModule testModule = new CoreTokenResourceAuthzModule(mockConfig, mockDebug,
                mockCoreTokenConfig);

        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorize(mockSSOTokenContext);

        //then
        assertFalse(result.get().isAuthorized());

    }

    @Test
    public void shouldAuthorizeAccessToSuperUserIfResourceEnabled() throws Exception {

        //given
        given(mockCoreTokenConfig.isCoreTokenResourceEnabled()).willReturn(true);

        CoreTokenResourceAuthzModule testModule = new CoreTokenResourceAuthzModule(mockConfig, mockDebug,
                mockCoreTokenConfig);
        SSOToken mockSSOToken = mock(SSOToken.class);

        given(mockSSOTokenContext.getCallerSSOToken()).willReturn(mockSSOToken);
        given(mockSSOToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("test");
        given(mockService.isSuperUser("test")).willReturn(true);

        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorize(mockSSOTokenContext);

        //then
        assertTrue(result.get().isAuthorized());

    }

    @Test
    public void shouldBlockAllAccessIfResourceEnabledButNonSuperUser() throws Exception {

        //given
        given(mockCoreTokenConfig.isCoreTokenResourceEnabled()).willReturn(true);
        CoreTokenResourceAuthzModule testModule = new CoreTokenResourceAuthzModule(mockConfig, mockDebug,
                mockCoreTokenConfig);
        SSOToken mockSSOToken = mock(SSOToken.class);

        given(mockSSOTokenContext.getCallerSSOToken()).willReturn(mockSSOToken);
        given(mockSSOToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("test");
        given(mockService.isSuperUser("test")).willReturn(false);

        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorize(mockSSOTokenContext);

        //then
        assertFalse(result.get().isAuthorized());

    }

}

