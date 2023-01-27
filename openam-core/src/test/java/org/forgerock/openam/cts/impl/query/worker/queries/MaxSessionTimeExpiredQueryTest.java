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

import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;

import java.util.Calendar;
import java.util.List;

import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.query.QueryBuilder;
import org.forgerock.openam.sm.datalayer.api.query.QueryFactory;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class MaxSessionTimeExpiredQueryTest {

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
        // FIXME Implement proper unit test that is not testing QueryFilter internals.
        given(mockQueryFilterConverter.visitAndFilter(isNull(), anyList()))
                .willReturn(Filter.alwaysTrue());

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
        MaxSessionTimeExpiredQuery<Connection> query = new MaxSessionTimeExpiredQuery<>(mockConnectionFactory,
                mockFactory, mockConfig);

        // When
        query.getQuery();

        // Then
        verify(mockBuilder).pageResultsBy(9);
    }

    @Test
    public void shouldReturnTokenIdAndSessionId() {
        // Given
        MaxSessionTimeExpiredQuery<Connection> query = new MaxSessionTimeExpiredQuery<>(mockConnectionFactory,
                mockFactory, mockConfig);

        // When
        query.getQuery();

        // Then
        verify(mockBuilder).returnTheseAttributes(
                CoreTokenField.TOKEN_ID,
                SessionTokenField.SESSION_ID.getField());
    }

}