package com.sun.identity.shared.debug.format;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.identity.shared.debug.DebugLevel;
import com.sun.identity.shared.debug.format.impl.JsonDebugFormatter;
import java.time.Instant;
import java.util.Map;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link JsonDebugFormatter}, verifying that
 * {@link DebugRecord} instances are correctly serialized to JSON.
 */
public class JsonDebugFormatterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final JsonDebugFormatter FORMATTER = new JsonDebugFormatter();

    private static final Instant TIMESTAMP = Instant.parse("2025-08-08T12:34:56Z");

    private static final String THREAD = "https-jsse-nio-443-exec-8";

    private static final String LOGGER = "RestApis";

    private static final String TRANSACTION_ID = "94c1578f-8289-48f9-9ade-96849f8da56f-207";

    private static DebugRecord record(DebugLevel level, String message, Throwable throwable) {
        return new DebugRecord(TIMESTAMP, level, THREAD, LOGGER, message, TRANSACTION_ID, throwable);
    }

    @Test
    public void producesValidJson() {
        Throwable throwable = new IllegalStateException("Foobar");
        DebugRecord debugRecord = record(DebugLevel.ERROR, "Unrecognized or invalid syntax", throwable);
        try {
            MAPPER.readTree(FORMATTER.format(debugRecord));
        } catch (JsonProcessingException e) {
            fail("Expected valid JSON but parsing threw an exception", e);
        }
    }

    @Test
    public void formatsFieldsIncludingException() throws Exception {
        // Given
        Throwable throwable = new IllegalStateException("Foobar");
        DebugRecord debugRecord = record(DebugLevel.ERROR, "Unrecognized or invalid syntax", throwable);

        // When
        Map<String, Object> root = MAPPER.readValue(FORMATTER.format(debugRecord),
                new TypeReference<Map<String, Object>>() {
                });

        // Then
        assertEquals(root.get("level"), "error");
        assertEquals(root.get("thread"), THREAD);
        assertEquals(root.get("logger"), LOGGER);
        assertEquals(root.get("message"), "Unrecognized or invalid syntax");
        assertEquals(root.get("transactionId"), TRANSACTION_ID);
        assertNotNull(root.get("timestamp"));

        @SuppressWarnings("unchecked")
        Map<String, Object> e = (Map<String, Object>) root.get("exception");
        assertNotNull(e, "Exception must be present");
        assertEquals(e.get("class"), "java.lang.IllegalStateException");
        assertEquals(e.get("message"), "Foobar");

        String stack = String.valueOf(e.get("stackTrace"));
        assertNotNull(stack);
        assertTrue(stack.contains("java.lang.IllegalStateException"));
        assertTrue(stack.contains("Foobar"));
        assertTrue(stack.contains("at "));
    }

    @Test
    public void omitsExceptionWhenNull() throws Exception {
        // Given
        DebugRecord debugRecord = record(DebugLevel.MESSAGE, "Initialized event listener", null);

        // When
        Map<String, Object> root = MAPPER.readValue(FORMATTER.format(debugRecord),
                new TypeReference<Map<String, Object>>() {
                });

        // Then
        assertEquals(root.get("level"), "message");
        assertEquals(root.get("message"), "Initialized event listener");
        assertFalse(root.containsKey("exception"), "Exception must be absent when throwable is null");
    }

}
