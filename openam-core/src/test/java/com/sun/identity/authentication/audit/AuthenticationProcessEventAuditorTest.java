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
 * Copyright 2015 ForgeRock AS.
 * Portions Copyright 2021 Wren Security.
 */
package com.sun.identity.authentication.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditConstants.EventName;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wrensecurity.wrenam.test.AbstractMockBasedTest;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.service.LoginState;

public class AuthenticationProcessEventAuditorTest extends AbstractMockBasedTest {

    private AuthenticationProcessEventAuditor auditor;

    @Mock
    private AuditEventPublisher eventPublisher;

    @Mock
    private AuditEventFactory eventFactory;

    @Mock
    private LoginState emptyState;

    @Mock
    private SSOToken emptyToken;

    @BeforeMethod
    public void setupMocks() {
        when(eventPublisher.isAuditing(any(), anyString(), any(EventName.class))).thenReturn(true);
        when(eventFactory.authenticationEvent(any())).thenCallRealMethod();
        auditor = new AuthenticationProcessEventAuditor(eventPublisher, eventFactory);
    }

    @Test
    public void shouldNotFailAuditLoginSuccess() {
        // given

        // when
        auditor.auditLoginSuccess(null);
        auditor.auditLoginSuccess(emptyState);

        // then
        verify(eventPublisher, times(2)).tryPublish(anyString(), any(AuditEvent.class));
        // no exceptions expected
    }

    @Test
    public void shouldNotFailAuditLoginFailure() {
        // given

        // when
        auditor.auditLoginFailure(null, null);
        auditor.auditLoginFailure(emptyState, AuditConstants.AuthenticationFailureReason.LOGIN_FAILED);

        // then
        verify(eventPublisher, times(2)).tryPublish(anyString(), any(AuditEvent.class));
        // no exceptions expected
    }

    @Test
    public void shouldNotFailAuditLogout() {
        // given

        // when
        auditor.auditLogout(null);
        auditor.auditLogout(emptyToken);

        // then
        verify(eventPublisher, times(2)).tryPublish(anyString(), any(AuditEvent.class));
        // no exceptions expected
    }
}
