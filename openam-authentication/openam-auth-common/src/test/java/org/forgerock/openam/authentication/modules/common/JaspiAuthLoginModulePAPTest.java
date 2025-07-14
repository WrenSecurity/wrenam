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
 * Copyright 2013-2016 ForgeRock AS.
 * Portions Copyright 2021 Wren Security.
 */

package org.forgerock.openam.authentication.modules.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.HashMap;
import java.util.Map;

import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.AuthStatus;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.module.ServerAuthModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthenticationException;

public class JaspiAuthLoginModulePAPTest {

    private JaspiAuthLoginModulePostAuthenticationPlugin jaspiPostAuthPlugin;

    private Map<String, Object> config = new HashMap<>();
    private boolean onLoginSuccessMethodCalled = false;
    private JaspiAuthModuleWrapper<ServerAuthModule> jaspiAuthWrapper;

    @BeforeMethod
    public void setUp() {

        jaspiAuthWrapper = mock(JaspiAuthModuleWrapper.class);

        jaspiPostAuthPlugin = new JaspiAuthLoginModulePostAuthenticationPlugin("amAuthPersistentCookie", jaspiAuthWrapper) {

            @Override
            protected Map<String, Object> generateConfig(HttpServletRequest request,
                    HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException {
                return config;
            }

            @Override
            protected void onLoginSuccess(MessageInfo messageInfo, Map requestParamsMap, HttpServletRequest request,
                    HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException {
                onLoginSuccessMethodCalled = true;
            }

            @Override
            public void onLoginFailure(Map requestParamsMap, HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

            }

            @Override
            public void onLogout(HttpServletRequest request, HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException {

            }
        };
    }

    @Test
    public void shouldCallOnLoginSuccessAndThrowAuthenticationExceptionWhenAuthExceptionCaught() throws Exception {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        doThrow(AuthException.class).when(jaspiAuthWrapper).initialize(isNull(), eq(config));

        //When
        boolean exceptionCaught = false;
        AuthenticationException exception = null;
        try {
            jaspiPostAuthPlugin.onLoginSuccess(requestParamsMap, request, response, ssoToken);
        } catch (AuthenticationException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        verify(jaspiAuthWrapper).initialize(any(), eq(config));
        verify(jaspiAuthWrapper, never()).secureResponse(any());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "authFailed");
    }

    @Test
    public void shouldCallOnLoginSuccessWhenSecureResponseReturnsSendSuccess() throws Exception {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(jaspiAuthWrapper.secureResponse(any()))
                .willReturn(AuthStatus.SEND_SUCCESS);

        //When
        jaspiPostAuthPlugin.onLoginSuccess(requestParamsMap, request, response, ssoToken);

        //Then
        verify(jaspiAuthWrapper).initialize(any(), eq(config));
        assertTrue(onLoginSuccessMethodCalled);
        verify(jaspiAuthWrapper).secureResponse(any());
    }

    @Test
    public void shouldCallOnLoginSuccessWhenSecureResponseReturnsSendFailure() throws AuthenticationException,
            AuthException {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(jaspiAuthWrapper.secureResponse(any()))
                .willReturn(AuthStatus.SEND_FAILURE);

        //When
        boolean exceptionCaught = false;
        AuthenticationException exception = null;
        try {
            jaspiPostAuthPlugin.onLoginSuccess(requestParamsMap, request, response, ssoToken);
        } catch (AuthenticationException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        verify(jaspiAuthWrapper).initialize(any(), eq(config));
        assertTrue(onLoginSuccessMethodCalled);
        verify(jaspiAuthWrapper).secureResponse(any());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "authFailed");
    }

    @Test
    public void shouldCallOnLoginSuccessWhenSecureResponseReturnsSendContinue() throws AuthenticationException,
            AuthException {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(jaspiAuthWrapper.secureResponse(any()))
                .willReturn(AuthStatus.SEND_CONTINUE);

        //When
        boolean exceptionCaught = false;
        AuthenticationException exception = null;
        try {
            jaspiPostAuthPlugin.onLoginSuccess(requestParamsMap, request, response, ssoToken);
        } catch (AuthenticationException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        verify(jaspiAuthWrapper).initialize(any(), eq(config));
        assertTrue(onLoginSuccessMethodCalled);
        verify(jaspiAuthWrapper).secureResponse(any());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "authFailed");
    }

    @Test
    public void shouldCallOnLoginSuccessWhenSecureResponseReturnsElse() throws AuthenticationException, AuthException {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(jaspiAuthWrapper.secureResponse(any()))
                .willReturn(AuthStatus.SUCCESS);

        //When
        boolean exceptionCaught = false;
        AuthenticationException exception = null;
        try {
            jaspiPostAuthPlugin.onLoginSuccess(requestParamsMap, request, response, ssoToken);
        } catch (AuthenticationException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        verify(jaspiAuthWrapper).initialize(any(), eq(config));
        assertTrue(onLoginSuccessMethodCalled);
        verify(jaspiAuthWrapper).secureResponse(any());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "authFailed");
    }

    @Test
    public void shouldCallOnLoginFailureAndDoNothing() throws AuthenticationException {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        //When
        jaspiPostAuthPlugin.onLoginFailure(requestParamsMap, request, response);

        //Then
        verifyNoInteractions(jaspiAuthWrapper);
    }

    @Test
    public void shouldCallOnLogoutAndDoNothing() throws Exception {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        //When
        jaspiPostAuthPlugin.onLogout(request, response, ssoToken);

        //Then
        verifyNoInteractions(jaspiAuthWrapper);
    }
}
