/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.1.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.1.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2025 Wren Security. All rights reserved.
 */
package com.sun.identity.shared.debug.format.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.identity.shared.debug.DebugLevel;
import com.sun.identity.shared.debug.format.DebugRecord;
import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.TimeZone;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link JsonDebugFormatter}, verifying that
 * {@link DebugRecord} instances are correctly serialized to JSON.
 */
public class JsonDebugFormatterTest {

    private TimeZone originalTimeZone;

    @BeforeTest
    public void setUtcAsDefaultTimeZone() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterTest
    public void restoreDefaultTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @DataProvider
    public Object[][] testCases() {
        Exception exception = new IllegalStateException("Unrecognized or invalid syntax");
        exception.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("com.sun.identity.shared.debug.format", "Foobar",
                    "JsonDebugFormatter.java", 42)
        });
        return new Object[][] {
            {
                DebugLevel.MESSAGE,
                "Initialized event listener",
                null,
                "expected_message.json"
            },
            {
                DebugLevel.ERROR,
                "Unrecognized or invalid syntax",
                exception,
                "expected_error.json"
            }
        };
    }

    @Test(dataProvider = "testCases")
    public void testFormatMatchesGoldenFile(DebugLevel level, String message, Throwable throwable,
            String expectedResource) throws Exception {
        DebugRecord debugRecord = new DebugRecord(
                Instant.parse("2025-08-08T12:34:56Z"),
                level,
                "https-jsse-nio-443-exec-8",
                "RestApis",
                "94c1578f-8289-48f9-9ade-96849f8da56f-207",
                message,
                throwable);

        String formattedJson = new JsonDebugFormatter().format(debugRecord);
        assertFalse(formattedJson.contains("\n"), "Formatted JSON must be a single line string");

        File resourceFile = new File(JsonDebugFormatterTest.class.getResource(expectedResource).getFile());
        String expectedJson = Files.readString(resourceFile.toPath(), UTF_8);

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode formattedNode = objectMapper.readValue(formattedJson, ObjectNode.class);
        ObjectNode expectedNode = objectMapper.readValue(expectedJson, ObjectNode.class);

        // Replace line separators in the rendered stack trace with whatever the current JVM uses
        if (expectedNode.has(JsonDebugFormatter.EXCEPTION_ATTR)) {
            ObjectNode exceptionNode = expectedNode.withObject("/" + JsonDebugFormatter.EXCEPTION_ATTR);
            exceptionNode.put(JsonDebugFormatter.STACK_TRACE_ATTR, exceptionNode
                    .get(JsonDebugFormatter.STACK_TRACE_ATTR)
                    .asText().replace("\n", System.lineSeparator()));
        }

        assertEquals(formattedNode, expectedNode, "Formatted JSON must match the golden file");
    }

}
