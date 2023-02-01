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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions Copyright 2021 Wren Security.
 */

package com.sun.identity.authentication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.security.auth.Subject;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wrensecurity.wrenam.test.AbstractMockBasedTest;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;

public class DefaultSessionActivatorTest extends AbstractMockBasedTest {

    private static final String ORGDN = "testdn";
    private static final SessionID SID = new SessionID("test");

    @Mock
    private SessionService mockSessionService;

    @Mock
    private InternalSession mockAuthSession;

    @Mock
    private InternalSession mockNewSession;

    @Mock
    private LoginState mockState;

    @BeforeMethod
    public void setupMocks() {
        given(mockState.getOrgDN()).willReturn(ORGDN);
        given(mockSessionService.newInternalSession(eq(ORGDN), eq(false)))
                .willReturn(mockNewSession);
        given(mockNewSession.getID()).willReturn(SID);
    }

    @Test
    public void shouldCreateNewInternalSession() throws Exception {
        // Given
        given(mockAuthSession.getPropertyNames()).willReturn(Collections.enumeration(Collections.emptyList()));

        // When
        DefaultSessionActivator.INSTANCE.activateSession(mockState, mockSessionService, mockAuthSession, null);

        // Then
        verify(mockSessionService).newInternalSession(eq(ORGDN), anyBoolean());
    }

    @Test
    public void shouldRemoveAuthContext() throws Exception {
        // Given
        given(mockAuthSession.getPropertyNames()).willReturn(Collections.enumeration(Collections.emptyList()));

        // When
        DefaultSessionActivator.INSTANCE.activateSession(mockState, mockSessionService, mockAuthSession, null);

        // Then
        verify(mockNewSession).clearAuthContext();
    }

    @Test
    public void shouldCopySessionProperties() throws Exception {
        // Given
        final List<String> sessionProperties = Arrays.asList("one", "two", "three");
        given(mockAuthSession.getPropertyNames()).willReturn(Collections.enumeration(sessionProperties));
        given(mockAuthSession.getProperty("one")).willReturn("a");
        given(mockAuthSession.getProperty("two")).willReturn("b");
        given(mockAuthSession.getProperty("three")).willReturn("c");

        // When
        DefaultSessionActivator.INSTANCE.activateSession(mockState, mockSessionService, mockAuthSession, null);

        // Then
        verify(mockState).setSessionProperties(mockNewSession);
        verify(mockNewSession).putProperty("one", "a");
        verify(mockNewSession).putProperty("two", "b");
        verify(mockNewSession).putProperty("three", "c");
    }

    @Test
    public void shouldDestroyAuthSession() throws Exception {
        // Given
        final SessionID authSessionID = new SessionID();
        given(mockAuthSession.getID()).willReturn(authSessionID);
        given(mockAuthSession.getPropertyNames()).willReturn(Collections.enumeration(Collections.emptyList()));

        // When
        DefaultSessionActivator.INSTANCE.activateSession(mockState, mockSessionService, mockAuthSession, null);

        // Then
        verify(mockSessionService).destroyAuthenticationSession(authSessionID);
    }

    @Test
    public void shouldUpdateLoginStateSession() throws Exception {
        // Given
        given(mockAuthSession.getPropertyNames()).willReturn(Collections.enumeration(Collections.emptyList()));

        // When
        DefaultSessionActivator.INSTANCE.activateSession(mockState, mockSessionService, mockAuthSession, null);

        // Then
        verify(mockState).setSession(mockNewSession);
    }

    @Test
    public void shouldUpdateLoginStateSubject() throws Exception {
        // Given
        given(mockAuthSession.getPropertyNames()).willReturn(Collections.enumeration(Collections.emptyList()));

        // When
        DefaultSessionActivator.INSTANCE.activateSession(mockState, mockSessionService, mockAuthSession, null);

        // Then
        ArgumentCaptor<Subject> subjectArgumentCaptor = ArgumentCaptor.forClass(Subject.class);
        verify(mockState).setSubject(subjectArgumentCaptor.capture());
        assertThat(subjectArgumentCaptor.getValue().getPrincipals()).contains(new SSOTokenPrincipal(SID.toString()));
    }

    @Test
    public void shouldReuseExistingSubjectIfProvided() throws Exception {
        // Given
        Subject subject = new Subject();
        given(mockAuthSession.getPropertyNames()).willReturn(Collections.enumeration(Collections.emptyList()));

        // When
        DefaultSessionActivator.INSTANCE.activateSession(mockState, mockSessionService, mockAuthSession, subject);

        // Then
        verify(mockState).setSubject(subject);
        assertThat(subject.getPrincipals()).contains(new SSOTokenPrincipal(SID.toString()));
    }
}