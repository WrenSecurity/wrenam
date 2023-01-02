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
 * Portions Copyright 2021 Wren Security.
 */
package org.forgerock.openam.cts.impl.query.worker.queries;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Calendar;
import java.util.List;

import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.query.QueryBuilder;
import org.forgerock.openam.sm.datalayer.api.query.QueryFactory;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.util.query.QueryFilterVisitor;
import org.mockito.ArgumentMatchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SessionIdleTimeExpiredQueryTest {

    private ConnectionFactory<Connection> mockConnectionFactory;
    private QueryFactory<Connection, Filter> mockFactory;
    private CoreTokenConfig mockConfig;
    private QueryBuilder<Connection, Filter> mockBuilder;
    private QueryFilterVisitor<Filter, Void, CoreTokenField> mockQueryFilterConverter;

    @BeforeMethod
    public void setup() {
        mockConnectionFactory = mock(ConnectionFactory.class);
        mockBuilder = mock(QueryBuilder.class);
        given(mockBuilder.withFilter(any(Filter.class))).willReturn(mockBuilder);
        given(mockBuilder.pageResultsBy(anyInt())).willReturn(mockBuilder);
        given(mockBuilder.returnTheseAttributes(
                eq(CoreTokenField.TOKEN_ID),
                eq(SessionTokenField.SESSION_ID.getField())))
                .willReturn(mockBuilder);

        mockQueryFilterConverter = mock(QueryFilterVisitor.class);
        given(mockQueryFilterConverter.visitAndFilter(isNull(), argThat(filters -> {
            // FIXME Make better filter matcher?
            return filters.size() == 3;
        }))).willReturn(Filter.alwaysTrue());

        mockFactory = mock(QueryFactory.class);
        given(mockFactory.createFilterConverter()).willReturn(mockQueryFilterConverter);
        given(mockFactory.createInstance()).willReturn(mockBuilder);

        mockConfig = mock(CoreTokenConfig.class);
        given(mockConfig.getCleanupPageSize()).willReturn(1);
    }

    @Test
    public void shouldPageResultsAsConfigured() {
        // Given
        mockConfig = mock(CoreTokenConfig.class);
        given(mockConfig.getCleanupPageSize()).willReturn(9);
        SessionIdleTimeExpiredQuery<Connection> query = new SessionIdleTimeExpiredQuery<>(mockConnectionFactory,
                mockFactory, mockConfig);

        // When
        query.getQuery();

        // Then
        verify(mockBuilder).pageResultsBy(9);
    }

    @Test
    public void shouldReturnTokenIdAndEtagAndSessionId() {
        // Given
        SessionIdleTimeExpiredQuery<Connection> query = new SessionIdleTimeExpiredQuery<>(mockConnectionFactory,
                mockFactory, mockConfig);

        // When
        query.getQuery();

        // Then
        verify(mockBuilder).returnTheseAttributes(
                CoreTokenField.TOKEN_ID,
                CoreTokenField.ETAG,
                SessionTokenField.SESSION_ID.getField());
    }

}