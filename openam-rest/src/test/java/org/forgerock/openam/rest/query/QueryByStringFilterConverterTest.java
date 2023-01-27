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
package org.forgerock.openam.rest.query;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.util.query.QueryFilter.*;
import static org.mockito.Mockito.*;

import org.forgerock.json.JsonPointer;
import org.forgerock.util.query.QueryFilter;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

import java.util.List;

public class QueryByStringFilterConverterTest {

    @Test
    public void shouldThrowQueryException() {
        // given
        QueryFilter<JsonPointer> filter = comparisonFilter(new JsonPointer("param1/param2"), "eq", "*");

        // when
        QueryException exception = null;
        try {
            filter.accept(new QueryByStringFilterConverter(), null);
        } catch (QueryException e) {
            exception = e;
        }

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(QueryException.QueryErrorCode.FILTER_DEPTH_NOT_SUPPORTED.name());
    }

    @Test
    public void shouldCreateAndFilter() {
        // given
        QueryFilter<JsonPointer> filter =
                and(equalTo(new JsonPointer("param1"), "value1"), contains(new JsonPointer("param2"), "value2"));
        QueryByStringFilterConverter mockConverter = mock(QueryByStringFilterConverter.class);
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        when(mockConverter.visitAndFilter(any(), anyList())).thenCallRealMethod();

        // when
        filter.accept(mockConverter, null);

        // then
        verify(mockConverter, times(1)).visitAndFilter(any(), captor.capture());
        assertThat(captor.getValue().size()).isEqualTo(2);

        verify(mockConverter, times(1)).visitEqualsFilter(any(), any(JsonPointer.class), any());
        verify(mockConverter, times(1)).visitContainsFilter(any(), any(JsonPointer.class), any());
    }

    @Test
    public void shouldCreateOrFilter() {
        // given
        QueryFilter<JsonPointer> filter =
                or(equalTo(new JsonPointer("param1"), "value1"), contains(new JsonPointer("param2"), "value2"));
        QueryByStringFilterConverter mockConverter = mock(QueryByStringFilterConverter.class);
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        when(mockConverter.visitOrFilter(any(), anyList())).thenCallRealMethod();

        // when
        filter.accept(mockConverter, null);

        // then
        verify(mockConverter, times(1)).visitOrFilter(any(), captor.capture());
        assertThat(captor.getValue().size()).isEqualTo(2);

        verify(mockConverter, times(1)).visitEqualsFilter(any(), any(JsonPointer.class), any());
        verify(mockConverter, times(1)).visitContainsFilter(any(), any(JsonPointer.class), any());
    }

    @Test
    public void shouldCreateCompoundFilter() {
        // given
        QueryFilter<JsonPointer> filter = and(
                or(equalTo(new JsonPointer("param1"), "value1"), contains(new JsonPointer("param2"), "value2")),
                or(equalTo(new JsonPointer("param3"), "value3"), contains(new JsonPointer("param4"), "value4")));
        QueryByStringFilterConverter mockConverter = mock(QueryByStringFilterConverter.class);
        ArgumentCaptor<List> andCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List> orCaptor = ArgumentCaptor.forClass(List.class);
        when(mockConverter.visitAndFilter(any(), anyList())).thenCallRealMethod();
        when(mockConverter.visitOrFilter(any(), anyList())).thenCallRealMethod();

        // when
        filter.accept(mockConverter, null);

        // then
        verify(mockConverter, times(1)).visitAndFilter(any(), andCaptor.capture());
        assertThat(andCaptor.getValue().size()).isEqualTo(2);

        verify(mockConverter, times(2)).visitOrFilter(any(), orCaptor.capture());
        assertThat(orCaptor.getAllValues().get(0).size()).isEqualTo(2);
        assertThat(orCaptor.getAllValues().get(1).size()).isEqualTo(2);

        verify(mockConverter, times(2)).visitEqualsFilter(any(), any(JsonPointer.class), any());
        verify(mockConverter, times(2)).visitContainsFilter(any(), any(JsonPointer.class), any());
    }
}
