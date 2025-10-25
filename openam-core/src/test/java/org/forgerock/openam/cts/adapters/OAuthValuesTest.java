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
 * Portions copyright 2025 Wren Security
 */
package org.forgerock.openam.cts.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;

/**
 * {@link OAuthValues} test case.
 */
public class OAuthValuesTest {

    @Test
    public void shouldStoreDateValue() {
        // Given
        long timestamp = 1370259234252L;
        OAuthValues values = new OAuthValues();

        // When
        Calendar calendar = values.getDateValue(timestamp);
        Collection<String> result = values.fromDateValue(calendar);

        // Then
        assertEquals(1, result.size());
        assertThat(result).contains(String.valueOf(timestamp));
    }

    @Test
    public void shouldStoreCollectionOfStrings() {
        // Given
        String one = "Badger";
        String two = "Weasel";
        String three = "Ferret";

        OAuthValues values = new OAuthValues();

        // When
        String value = values.getSingleValue(Arrays.asList(one, two, three));
        Collection<String> result = values.fromSingleValue(value);

        // Then
        assertEquals(3, result.size());
        assertThat(result).contains(one, two, three);
    }

    @Test
    public void shouldHandleEmptyCollection() {
        // Given
        OAuthValues values = new OAuthValues();

        // When
        String value = values.getSingleValue(Collections.<String>emptyList());
        Collection<String> result = values.fromSingleValue(value);

        // Then
        assertEquals(0, result.size());
    }
}
