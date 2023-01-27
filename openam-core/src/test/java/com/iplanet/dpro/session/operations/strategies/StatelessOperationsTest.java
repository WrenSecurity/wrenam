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

package com.iplanet.dpro.session.operations.strategies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.forgerock.openam.blacklist.Blacklist;
import org.forgerock.openam.session.SessionEventType;
import org.forgerock.openam.session.authorisation.SessionChangeAuthorizer;
import org.forgerock.openam.sso.providers.stateless.StatelessSSOProvider;
import org.forgerock.openam.sso.providers.stateless.StatelessSession;
import org.forgerock.openam.sso.providers.stateless.StatelessSessionManager;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wrensecurity.wrenam.test.AbstractMockBasedTest;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.SessionTimedOutException;
import com.iplanet.dpro.session.service.SessionAuditor;
import com.iplanet.dpro.session.service.SessionLogging;
import com.iplanet.dpro.session.share.SessionInfo;

public class StatelessOperationsTest extends AbstractMockBasedTest {

    @Mock
    private StatelessSSOProvider mockSsoProvider;

    @Mock
    private StatelessSessionManager mockSessionFactory;

    @Mock
    private Blacklist<Session> mockSessionBlacklist;

    @Mock
    private StatelessSession mockSession;

    @Mock
    private SessionLogging mockSessionLogging;
    @Mock
    private SessionAuditor mockSessionAuditor;

    private SessionID sid;

    private StatelessOperations statelessOperations;

    @Mock
    private SessionChangeAuthorizer mockSessionChangeAuthorizer;

    @BeforeMethod
    public void setup() {
        sid = new SessionID("test");
        given(mockSession.getID()).willReturn(sid);
        given(mockSession.getSessionID()).willReturn(sid);
        statelessOperations = new StatelessOperations(
                null, mockSessionFactory, mockSessionBlacklist, mockSessionLogging, mockSessionAuditor,
                mockSessionChangeAuthorizer);
    }

    @Test
    public void shouldRefreshFromStatelessSessionFactory() throws Exception {
        // Given
        SessionInfo info = new SessionInfo();
        info.setExpiryTime(currentTimeMillis() + TimeUnit.MINUTES.toMillis(10), TimeUnit.MILLISECONDS);
        given(mockSessionFactory.getSessionInfo(sid)).willReturn(info);

        // When
        SessionInfo result = statelessOperations.refresh(mockSession, false);

        // Then
        verify(mockSessionFactory).getSessionInfo(sid);
        assertThat(result).isSameAs(info);
    }

    @Test(expectedExceptions = SessionTimedOutException.class)
    public void refreshShouldTimeoutFromStatelessSessionFactory() throws Exception {
        // Given
        SessionInfo info = new SessionInfo();
        given(mockSessionFactory.getSessionInfo(sid)).willReturn(info);

        // When
        SessionInfo result = statelessOperations.refresh(mockSession, false);

        // Then exception should be thrown, as session is timed-out
    }

    @Test
    public void shouldBlacklistSessionOnLogout() throws Exception {
        // Given

        // When
        statelessOperations.logout(mockSession);

        // Then
        verify(mockSessionLogging).logEvent(
                eq(mockSessionFactory.getSessionInfo(mockSession.getSessionID())),
                eq(SessionEventType.LOGOUT),
                anyLong());
        verify(mockSessionAuditor).auditActivity(
                eq(mockSessionFactory.getSessionInfo(mockSession.getSessionID())),
                eq(SessionEventType.LOGOUT),
                anyLong());
        verify(mockSessionBlacklist).blacklist(mockSession);
    }

    @Test
    public void shouldCheckPermissionToDestroySession() throws Exception {
        // Given
        Session requester = mock(Session.class);

        // When
        statelessOperations.destroy(requester, mockSession);

        // Then
        verify(mockSessionChangeAuthorizer).checkPermissionToDestroySession(requester, sid);
    }

    @Test
    public void shouldBlacklistSessionOnDestroyWhenAllowed() throws Exception {
        // Given
        Session requester = mock(Session.class);

        // When
        statelessOperations.destroy(requester, mockSession);

        // Then
        verify(mockSessionLogging).logEvent(
                eq(mockSessionFactory.getSessionInfo(mockSession.getSessionID())),
                eq(SessionEventType.DESTROY),
                anyLong());
        verify(mockSessionAuditor).auditActivity(
                eq(mockSessionFactory.getSessionInfo(mockSession.getSessionID())),
                eq(SessionEventType.DESTROY),
                anyLong());
        verify(mockSessionBlacklist).blacklist(mockSession);
    }

    @Test(expectedExceptions = SessionException.class, expectedExceptionsMessageRegExp = "test")
    public void shouldNotBlacklistSessionOnDestroyIfNotAllowed() throws Exception {
        // Given
        Session requester = mock(Session.class);
        SessionException ex = new SessionException("test");
        willThrow(ex).given(mockSessionChangeAuthorizer).checkPermissionToDestroySession(requester, sid);

        // When
        try {
            statelessOperations.destroy(requester, mockSession);
        } finally {
            // Then
            verifyNoInteractions(mockSessionBlacklist);
        }
    }
}