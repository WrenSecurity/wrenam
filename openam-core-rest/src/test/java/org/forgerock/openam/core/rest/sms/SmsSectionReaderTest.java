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
 * Copyright 2026 Wren Security.
 */

package org.forgerock.openam.core.rest.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceSchema;

public class SmsSectionReaderTest {

    private static final String SECTION_PROPERTIES_NAME = "SmsSectionReaderTest";

    private static final String MISSING_SECTION_PROPERTIES_NAME = "MissingSmsSectionReaderTest";

    private Debug debug;

    private ServiceSchema schema;

    @BeforeMethod
    public void setUp() {
        debug = mock(Debug.class);
        schema = mock(ServiceSchema.class);
    }

    @Test
    public void loadsSectionPropertiesForNamedSchema() {
        // Given
        given(schema.getName()).willReturn(SECTION_PROPERTIES_NAME);
        given(schema.getServiceName()).willReturn(MISSING_SECTION_PROPERTIES_NAME);

        // When
        Map<String, String> result = SmsSectionReader.readSectionsByAttributeName(schema, debug);

        // Then
        assertThat(result).containsExactly(
                entry("firstAttribute", "general"),
                entry("secondAttribute", "general"),
                entry("thirdAttribute", "advanced"));
    }

    @Test
    public void loadsSectionPropertiesForUnnamedSchema() {
        // Given
        given(schema.getServiceName()).willReturn(SECTION_PROPERTIES_NAME);

        // When
        Map<String, String> result = SmsSectionReader.readSectionsByAttributeName(schema, debug);

        // Then
        assertThat(result).containsExactly(
                entry("firstAttribute", "general"),
                entry("secondAttribute", "general"),
                entry("thirdAttribute", "advanced"));
    }

    @Test
    public void loadsServiceSectionPropertiesForParentPath() {
        // Given
        given(schema.getName()).willReturn(MISSING_SECTION_PROPERTIES_NAME);
        given(schema.getResourceName()).willReturn(SmsRequestHandler.USE_PARENT_PATH);
        given(schema.getServiceName()).willReturn(SECTION_PROPERTIES_NAME);

        // When
        Map<String, String> result = SmsSectionReader.readSectionsByAttributeName(schema, debug);

        // Then
        assertThat(result).containsExactly(
                entry("firstAttribute", "general"),
                entry("secondAttribute", "general"),
                entry("thirdAttribute", "advanced"));
    }

    @Test
    public void returnsEmptyMapWhenSectionPropertiesDoNotExist() {
        // Given
        given(schema.getName()).willReturn(MISSING_SECTION_PROPERTIES_NAME);

        // When
        Map<String, String> result = SmsSectionReader.readSectionsByAttributeName(schema, debug);

        // Then
        assertThat(result).isEmpty();
    }

}
