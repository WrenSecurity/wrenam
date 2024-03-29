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
 */

package org.forgerock.openam.core.rest.sms;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.test.assertj.AssertJJsonValueAssert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.identity.shared.encode.Base64;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;


public class SmsJsonConverterTest {
    public static final String SECTION_1_NAME = "section1";
    public static final String STRING_VALUE_NAME = "stringValueName";
    public static final String STRING_VALUE_RESOURCE_NAME = "stringValueResourceName";
    public static final String INT_VALUE_NAME = "intValueName";
    public static final String BOOLEAN_VALUE_NAME = "booleanValueName";
    public static final String DECIMAL_VALUE_NAME = "decimalValueName";
    public static final String PASSWORD_VALUE_NAME = "passwordValueName";
    public static final String MAP_VALUE_NAME = "mapValueName";
    public static final String ARRAY_VALUE_NAME = "arrayValueName";
    public static final String ATT_1_NAME = "att1Name";
    public static final String ATT_2_NAME = "att2Name";
    public static final String ATT_2_VAL = "att2Val";
    public static final String ATT_1_VAL = "att1Val";
    public static final String ARRAY_STRING_1 = "String1";
    public static final String ARRAY_STRING_2 = "String2";
    public static final String STRING_VALUE = "stringValue";
    public static final String SCRIPT_VALUE = "var a = 1; var b = 2;";
    public static final int INT_VALUE = 1;
    public static final double DOUBLE_VALUE = 1.1;
    public static final boolean BOOLEAN_VALUE = true;
    public static final String SECTION_1_STRING_VALUE_NAME = "section1StringValueName";
    public static final String SCRIPT_VALUE_NAME = "scriptValueName";
    private ServiceSchema serviceSchema;
    private Map<String, Set<String>> mapRepresentation;
    private JsonValue jsonRepresentation;
    private SmsJsonConverter converter;

    private class TestJsonConverter extends SmsJsonConverter {

        public TestJsonConverter(ServiceSchema schema) {
            super(schema);
        }

        @Override
        protected HashMap<String, String> getAttributeNameToSection() {
            final HashMap<String, String> sectionToAttribute = new HashMap<String, String>();
            sectionToAttribute.put(SECTION_1_STRING_VALUE_NAME, SECTION_1_NAME);
            return sectionToAttribute;
        }

        @Override
        protected List<String> getHiddenAttributeNames() {
            return new ArrayList<String>();
        }
    }

    @BeforeClass
    public void setup() throws UnsupportedEncodingException {
        serviceSchema = mock(ServiceSchema.class);

        given(serviceSchema.getAttributeSchemaNames()).willReturn(getHashSet(STRING_VALUE_NAME, INT_VALUE_NAME,
                BOOLEAN_VALUE_NAME, DECIMAL_VALUE_NAME, ARRAY_VALUE_NAME, MAP_VALUE_NAME, SECTION_1_STRING_VALUE_NAME,
                SCRIPT_VALUE_NAME, PASSWORD_VALUE_NAME));

        addAttributeSchema(STRING_VALUE_NAME, AttributeSchema.Syntax.STRING, null, AttributeSchema.Type.SINGLE,
                STRING_VALUE_RESOURCE_NAME);
        addAttributeSchema(INT_VALUE_NAME, AttributeSchema.Syntax.NUMERIC, null, AttributeSchema.Type.SINGLE, null);
        addAttributeSchema(BOOLEAN_VALUE_NAME, AttributeSchema.Syntax.BOOLEAN, null, AttributeSchema.Type.SINGLE, null);
        addAttributeSchema(DECIMAL_VALUE_NAME, AttributeSchema.Syntax.DECIMAL, null, AttributeSchema.Type.SINGLE, null);
        addAttributeSchema(ARRAY_VALUE_NAME, AttributeSchema.Syntax.STRING, AttributeSchema.UIType.UNORDEREDLIST,
                AttributeSchema.Type.LIST, null);
        addAttributeSchema(MAP_VALUE_NAME, AttributeSchema.Syntax.STRING, AttributeSchema.UIType.MAPLIST,
                AttributeSchema.Type.LIST, null);
        addAttributeSchema(SECTION_1_STRING_VALUE_NAME, AttributeSchema.Syntax.STRING, null, AttributeSchema.Type.SINGLE,
                null);
        addAttributeSchema(SCRIPT_VALUE_NAME, AttributeSchema.Syntax.SCRIPT, null, AttributeSchema.Type.SINGLE, null);
        addAttributeSchema(PASSWORD_VALUE_NAME, AttributeSchema.Syntax.PASSWORD, null, AttributeSchema.Type.SINGLE, null);

        converter = new TestJsonConverter(serviceSchema);

        //create map version..
        mapRepresentation = new HashMap<>();
        mapRepresentation.put(STRING_VALUE_NAME, getHashSet(STRING_VALUE));
        mapRepresentation.put(INT_VALUE_NAME, getHashSet(Integer.toString(INT_VALUE)));
        mapRepresentation.put(BOOLEAN_VALUE_NAME, getHashSet(Boolean.toString(BOOLEAN_VALUE)));
        mapRepresentation.put(DECIMAL_VALUE_NAME, getHashSet(Double.toString(DOUBLE_VALUE)));
        mapRepresentation.put(ARRAY_VALUE_NAME, getHashSet(ARRAY_STRING_1, ARRAY_STRING_2));
        mapRepresentation.put(MAP_VALUE_NAME, getHashSet("[" + ATT_1_NAME + "]=" + ATT_1_VAL, "[" + ATT_2_NAME + "]="
                + ATT_2_VAL));
        mapRepresentation.put(SECTION_1_STRING_VALUE_NAME, getHashSet(STRING_VALUE));
        mapRepresentation.put(SCRIPT_VALUE_NAME, getHashSet(SCRIPT_VALUE));
        mapRepresentation.put(PASSWORD_VALUE_NAME, getHashSet(STRING_VALUE));

        //create json version..
        final HashMap<String, String> mapValue = new
                HashMap<String, String>();
        mapValue.put(ATT_1_NAME, ATT_1_VAL);
        mapValue.put(ATT_2_NAME, ATT_2_VAL);
        final HashMap<String, String> sectionValue = new HashMap<String, String>();
        sectionValue.put(SECTION_1_STRING_VALUE_NAME, STRING_VALUE);
        final List<String> arrayValue = new ArrayList<String>();
        arrayValue.add(ARRAY_STRING_1);
        arrayValue.add(ARRAY_STRING_2);

        jsonRepresentation = json(object(
                field(STRING_VALUE_RESOURCE_NAME, STRING_VALUE),
                field(INT_VALUE_NAME, INT_VALUE),
                field(BOOLEAN_VALUE_NAME, BOOLEAN_VALUE),
                field(DECIMAL_VALUE_NAME, DOUBLE_VALUE),
                field(ARRAY_VALUE_NAME, arrayValue),
                field(MAP_VALUE_NAME, mapValue),
                field(SECTION_1_NAME, sectionValue),
                field(SCRIPT_VALUE_NAME, Base64.encode(SCRIPT_VALUE.getBytes("UTF-8"))),
                field(PASSWORD_VALUE_NAME, STRING_VALUE)));
    }

    private void addAttributeSchema(String valueName, AttributeSchema.Syntax syntax, AttributeSchema.UIType
            uiType, AttributeSchema.Type type, String resourceName) {
        AttributeSchema attributeSchema = mock(AttributeSchema.class);
        given(attributeSchema.getSyntax()).willReturn(syntax);
        given(attributeSchema.getType()).willReturn(type);
        given(attributeSchema.getUIType()).willReturn(uiType);
        given(attributeSchema.getResourceName()).willReturn(resourceName);
        given(attributeSchema.getI18NKey()).willReturn("I18NKey");
        given(serviceSchema.getAttributeSchema(valueName)).willReturn(attributeSchema);
    }

    private HashSet<String> getHashSet(String... strings) {
        HashSet<String> result = new HashSet<String>();
        for (String s : strings) {
            result.add(s);
        }
        return result;
    }

    @Test
    public void convertFromJson() throws Exception {
        //Given
        given(serviceSchema.validateAttributes(any())).willReturn(true);

        //When
        Map<String, Set<String>> result = converter.fromJson(jsonRepresentation);

        //Then

        assertThat(result).isEqualTo(mapRepresentation);
    }

    @Test
    public void convertToJson() throws SMSException {
        //Given
        given(serviceSchema.validateAttributes(any())).willReturn(true);

        //When
        JsonValue result = converter.toJson(mapRepresentation, true);

        //Then
        AssertJJsonValueAssert.AbstractJsonValueAssert asserter = AssertJJsonValueAssert.assertThat(result);
        asserter.isObject()
                .containsField(SECTION_1_NAME)
                .containsField(STRING_VALUE_RESOURCE_NAME)
                .containsField(INT_VALUE_NAME)
                .containsField(BOOLEAN_VALUE_NAME)
                .containsField(DECIMAL_VALUE_NAME)
                .containsField(MAP_VALUE_NAME)
                .containsField(ARRAY_VALUE_NAME);

        asserter.isObject().hasObject(SECTION_1_NAME)
                .contains(Assertions.entry(SECTION_1_STRING_VALUE_NAME, STRING_VALUE));

        asserter.isObject().stringAt(STRING_VALUE_RESOURCE_NAME)
                .isEqualTo(STRING_VALUE);

        asserter.isObject().integerAt(INT_VALUE_NAME)
                .isEqualTo(INT_VALUE);

        asserter.isObject().booleanAt(BOOLEAN_VALUE_NAME)
                .isEqualTo(BOOLEAN_VALUE);

        asserter.isObject().doubleAt(DECIMAL_VALUE_NAME)
                .isEqualTo(DOUBLE_VALUE);

        asserter.hasArray(ARRAY_VALUE_NAME).isArray().containsOnly(ARRAY_STRING_1, ARRAY_STRING_2);

        asserter.hasObject(MAP_VALUE_NAME)
                .contains(Assertions.entry(ATT_1_NAME, ATT_1_VAL))
                .contains(Assertions.entry(ATT_2_NAME, ATT_2_VAL));

        asserter.isObject().hasNull(PASSWORD_VALUE_NAME);
    }

    @Test(expectedExceptions = JsonException.class)
    public void invalidValuesToJson() throws SMSException {
        //Given
        given(serviceSchema.validateAttributes(any())).willReturn(false);

        //When
        converter.toJson(mapRepresentation, true);
    }

    @Test(expectedExceptions = JsonException.class)
    public void invalidValuesToMap() throws Exception {
        //Given
        given(serviceSchema.validateAttributes(any())).willReturn(false);

        //When
        converter.fromJson(jsonRepresentation);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void invalidValueTypeResultsInBadRequestException() throws Exception {
        given(serviceSchema.validateAttributes(any(Map.class))).willReturn(true);

        converter.fromJson(json(object(field(INT_VALUE_NAME, "blabla"))));
    }
}
