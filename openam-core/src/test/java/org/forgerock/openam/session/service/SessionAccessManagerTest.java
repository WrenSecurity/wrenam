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
 * Copyright 2016 ForgeRock AS.
 * Portions Copyright 2021 Wren Security.
 */

package org.forgerock.openam.session.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.forgerock.openam.audit.context.AMExecutorServiceFactory;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.session.service.access.persistence.InternalSessionStore;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wrensecurity.wrenam.test.AbstractMockBasedTest;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionState;
import com.sun.identity.shared.debug.Debug;

public class SessionAccessManagerTest extends AbstractMockBasedTest {

    private SessionAccessManager sessionAccessManager;
    @Mock private Debug mockDebug;
    @Mock private InternalSessionStore sessionStore;
    @Mock private Session mockSession;
    @Mock private SessionID mockSessionID;
    @Mock private InternalSession mockInternalSession;

    @BeforeMethod
    public void setUp() throws Exception {
        sessionAccessManager = new SessionAccessManager(
                mock(SessionCache.class), mock(AMExecutorServiceFactory.class), mock(ThreadMonitor.class), sessionStore);
        given(mockSession.getID()).willReturn(mockSessionID);
        given(mockInternalSession.getSessionID()).willReturn(mockSessionID);

    }

    @Test
    public void shouldUpdateIfAboutToDelete() throws Exception {
        // Given
        final InternalSession session = mock(InternalSession.class);

        // set session up to be updated
        given(session.willExpire()).willReturn(true);
        given(session.isStored()).willReturn(true);
        given(session.isTimedOut()).willReturn(true);

        // now set it to be destroyed instead
        given(session.getState()).willReturn(SessionState.DESTROYED);

        // When
        sessionAccessManager.persistInternalSession(session);

        // Then
        verify(sessionStore).store(any(InternalSession.class));
    }

    @Test
    public void shouldDeleteSessionTokenOnLogout() throws Exception {
        // Given
        given(mockSession.getSessionID()).willReturn(mockSessionID);
        given(mockSession.getID()).willReturn(mockSessionID);
        given(mockInternalSession.getID()).willReturn(mockSessionID);

        given(mockInternalSession.isStored()).willReturn(true);
        given(mockInternalSession.getState()).willReturn(SessionState.DESTROYED);
        // When
        sessionAccessManager.removeInternalSession(mockInternalSession);
        // Then
        verify(sessionStore).remove(mockInternalSession);
    }
}
