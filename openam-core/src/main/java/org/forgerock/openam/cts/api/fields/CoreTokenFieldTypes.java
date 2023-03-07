/**
 * Copyright 2013-2016 ForgeRock AS.
 *
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
 * Portions Copyright 2023 Wren Security
 */
package org.forgerock.openam.cts.api.fields;

import static java.util.Collections.singleton;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.tokens.CoreTokenField;

/**
 * Provides the mapping between CoreTokenFields and the type of the value that is associated to
 * that field.
 *
 * There are currently a number of uses for the type information of a Core Token Field:
 * - Manipulating a Token via its generic fields.
 * - Persisting a Token to LDAP
 *
 * Both of these cases need to know the type of the value stored in the Tokens map.
 */
public class CoreTokenFieldTypes {
    /**
     * Validate a collection of key/value mappings.
     *
     * @param types A mapping of CoreTokenField to value. Non null, may be empty.
     * @throws CoreTokenException If one of the values was invalid for the CoreTokenField field.
     */
    public static void validateTypes(Map<CoreTokenField, Object> types) throws CoreTokenException {
        for (Map.Entry<CoreTokenField, Object> entry : types.entrySet()) {
            validateType(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Validate the value matches the expected type for the given key.
     *
     * @param field The CoreTokenField to validate against.
     * @param value The value to verify. Non null.
     */
    public static void validateType(CoreTokenField field, Object value) throws CoreTokenException {
        if (value == null) {
            throw new CoreTokenException(MessageFormat.format(
                    "\n" +
                            CoreTokenConstants.DEBUG_HEADER +
                            "Value field cannot be null!" +
                            "Key: {0}:{1}",
                    CoreTokenField.class.getSimpleName(),
                    field.name()));
        }

        if (isMulti(field)) {
            validateMultiStringType(field, value);
        } else if (isString(field)) {
            validateSingleType(field, value, String.class);
        } else if (isInteger(field)) {
            validateSingleType(field, value, Integer.class);
        } else if (isCalendar(field)) {
            validateSingleType(field, value, Calendar.class);
        } else if (isByteArray(field)) {
            validateSingleType(field, value, byte[].class);
        } else {
            throw new IllegalStateException("Unknown field: " + field.name());
        }
    }

    private static void validateMultiStringType(CoreTokenField field, Object value) throws CoreTokenException {
        if (value instanceof String) {
            return;
        }

        if (!(value instanceof Set)) {
            throw new CoreTokenException(MessageFormat.format(
                    "\n" +
                            CoreTokenConstants.DEBUG_HEADER +
                            "Value was not the correct type:\n" +
                            "           Key: {0}:{1}\n" +
                            "Required Class: String or Set<String>\n" +
                            "  Actual Class: {2}",
                    CoreTokenField.class.getSimpleName(),
                    field.name(),
                    value.getClass().getName()));
        }

        for (Object setValue : (Set) value) {
            if (!(setValue instanceof String)) {
                throw new CoreTokenException(MessageFormat.format(
                        "\n" +
                                CoreTokenConstants.DEBUG_HEADER +
                                "Value set contains an invalidate type:\n" +
                                "           Key: {0}:{1}\n" +
                                "Required Class: String\n" +
                                "  Actual Class: {2}",
                        CoreTokenField.class.getSimpleName(),
                        field.name(),
                        setValue.getClass().getName()));
            }
        }
    }

    private static void validateSingleType(CoreTokenField field, Object value, Class<?> expectedType) throws CoreTokenException {
        if (!expectedType.isAssignableFrom(value.getClass())) {
            throw new CoreTokenException(MessageFormat.format(
                    "\n" +
                            CoreTokenConstants.DEBUG_HEADER +
                            "Value was not the correct type:\n" +
                            "           Key: {0}:{1}\n" +
                            "Required Class: {2}" +
                            "  Actual Class: {3}",
                    CoreTokenField.class.getSimpleName(),
                    field.name(),
                    expectedType.getName(),
                    value.getClass().getName()));
        }
    }

    /**
     * @param field Non null field to check.
     * @return True if the field is a Date.
     */
    public static boolean isCalendar(CoreTokenField field) {
        // Intentional fall-through
        switch (field) {
            case EXPIRY_DATE:
            case CREATE_TIMESTAMP:
            case DATE_ONE:
            case DATE_TWO:
            case DATE_THREE:
            case DATE_FOUR:
            case DATE_FIVE:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param field Non null field to check.
     * @return True if the field is an Integer.
     */
    public static boolean isInteger(CoreTokenField field) {
        // Intentional fall-through
        switch (field) {
            case INTEGER_ONE:
            case INTEGER_TWO:
            case INTEGER_THREE:
            case INTEGER_FOUR:
            case INTEGER_FIVE:
            case INTEGER_SIX:
            case INTEGER_SEVEN:
            case INTEGER_EIGHT:
            case INTEGER_NINE:
            case INTEGER_TEN:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param field Non null field to check.
     * @return True if the field is a String.
     */
    public static boolean isString(CoreTokenField field) {
        switch (field) {
            case TOKEN_ID:
            case USER_ID:
            case ETAG:
            case STRING_ONE:
            case STRING_TWO:
            case STRING_THREE:
            case STRING_FOUR:
            case STRING_FIVE:
            case STRING_SIX:
            case STRING_SEVEN:
            case STRING_EIGHT:
            case STRING_NINE:
            case STRING_TEN:
            case STRING_ELEVEN:
            case STRING_TWELVE:
            case STRING_THIRTEEN:
            case STRING_FOURTEEN:
            case STRING_FIFTEEN:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param field Non null field to check.
     * @return True if the field is a multi-value field.
     */
    public static boolean isMulti(CoreTokenField field) {
        switch (field) {
            case MULTI_STRING_ONE:
            case MULTI_STRING_TWO:
            case MULTI_STRING_THREE:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param field Non null field to check.
     * @return True if the field is a binary field.
     */
    public static boolean isByteArray(CoreTokenField field) {
        return CoreTokenField.BLOB.equals(field);
    }

}
