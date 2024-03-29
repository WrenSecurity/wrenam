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
 * Copyright 2013-2017 ForgeRock AS.
 */

package org.forgerock.openam.cts.api.fields;

import static java.util.Collections.singleton;
import static org.forgerock.openam.utils.Time.getCalendarInstance;

import java.util.Calendar;
import java.util.Set;

import org.fest.assertions.Assertions;
import org.fest.assertions.Condition;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.tokens.CoreTokenField;
import org.testng.annotations.Test;

public class CoreTokenFieldTypesTest {
    @Test
    public void shouldValidateIntegerField() throws CoreTokenException {
        // Given
        CoreTokenField key = CoreTokenField.INTEGER_ONE;
        Integer value = 1234;

        // When / Then
        CoreTokenFieldTypes.validateType(key, value);
    }

    @Test
    public void shouldValidateStringField() throws CoreTokenException {
        // Given
        CoreTokenField key = CoreTokenField.STRING_ONE;
        String value = "badger";

        // When / Then
        CoreTokenFieldTypes.validateType(key, value);
    }

    @Test
    public void shouldValidateDateField() throws CoreTokenException {
        // Given
        CoreTokenField key = CoreTokenField.DATE_ONE;
        Calendar value = getCalendarInstance();

        // When / Then
        CoreTokenFieldTypes.validateType(key, value);
    }

    @Test
    public void shouldValidateByteField() throws CoreTokenException {
        // Given
        CoreTokenField key = CoreTokenField.BLOB;
        byte[] value = new byte[]{1, 2, 3, 4};

        // When / Then
        CoreTokenFieldTypes.validateType(key, value);
    }

    @Test (expectedExceptions = CoreTokenException.class)
    public void shouldValidateInvalidType() throws CoreTokenException {
        // Given
        CoreTokenField key = CoreTokenField.BLOB;
        Integer value = 1234;

        // When / Then
        CoreTokenFieldTypes.validateType(key, value);
    }

    @Test
    public void whenValidMultiStringProvidedValidationPassed() throws CoreTokenException {
        // Given
        CoreTokenField key = CoreTokenField.MULTI_STRING_ONE;
        String value = "abc";

        // When
        CoreTokenFieldTypes.validateType(key, value);
    }

    @Test
    public void whenValidMultiStringSetProvidedValidationPassed() throws CoreTokenException {
        // Given
        CoreTokenField key = CoreTokenField.MULTI_STRING_ONE;
        Set<String> value = singleton("abc");

        // When
        CoreTokenFieldTypes.validateType(key, value);
    }

    @Test(expectedExceptions = CoreTokenException.class)
    public void whenInvalidTypeProvidedForMultiStringFieldThrowException() throws CoreTokenException {
        // Given
        CoreTokenField key = CoreTokenField.MULTI_STRING_ONE;
        Integer value = 1;

        // When
        CoreTokenFieldTypes.validateType(key, value);
    }

    @Test(expectedExceptions = CoreTokenException.class)
    public void whenInvalidTypeProvidedInSetForMultiStringSetFieldThrowException() throws CoreTokenException {
        // Given
        CoreTokenField key = CoreTokenField.MULTI_STRING_ONE;
        Set<?> value = singleton(1);

        // When
        CoreTokenFieldTypes.validateType(key, value);
    }

    @Test
    public void shouldEnsureAllFieldTypesHaveAValidationType() {
        for (CoreTokenField field : CoreTokenField.values()) {
            // Ignore read only fields.
            if (Token.isFieldReadOnly(field)) {
                continue;
            }

            Assertions.assertThat(field).satisfies(new Condition<Object>("has valid type") {
                @Override
                public boolean matches(Object t) {
                    CoreTokenField obj = (CoreTokenField) t;
                    return CoreTokenFieldTypes.isByteArray(obj) || CoreTokenFieldTypes.isCalendar(obj)
                            || CoreTokenFieldTypes.isInteger(obj) || CoreTokenFieldTypes.isString(obj)
                            || CoreTokenFieldTypes.isMulti(obj);
                }
            });
        }
    }
}
