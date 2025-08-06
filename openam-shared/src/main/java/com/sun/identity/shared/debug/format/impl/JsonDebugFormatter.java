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

import com.sun.identity.shared.debug.DebugConstants;
import com.sun.identity.shared.debug.format.DebugFormatter;
import com.sun.identity.shared.debug.format.DebugRecord;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import org.forgerock.json.JsonValue;

/**
 * Format {@link DebugRecord DebugRecords} as a serialized JSON object.
 */
public class JsonDebugFormatter implements DebugFormatter {

    static final String EXCEPTION_ATTR = "exception";

    static final String STACK_TRACE_ATTR = "stackTrace";

    private final DateTimeFormatter timestampFormatter;

    /**
     * Create new debug formatter with the default timestamp formatter.
     */
    public JsonDebugFormatter() {
        this(DebugConstants.DEBUG_DATE_FORMATTER.withZone(TimeZone.getDefault().toZoneId()));
    }

    /**
     * Create new debug formatter with the given timestamp formatter.
     *
     * @param timestampFormatter date time formatter for formatting timestamp value (never null)
     */
    public JsonDebugFormatter(DateTimeFormatter timestampFormatter) {
        this.timestampFormatter = timestampFormatter;
    }

    @Override
    public String format(DebugRecord logRecord) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("timestamp", timestampFormatter.format(logRecord.timestamp));
        message.put("level", logRecord.level.name());
        message.put("thread", logRecord.thread);
        message.put("logger", logRecord.logger);
        message.put("transactionId", logRecord.transactionId);
        message.put("message", logRecord.message);

        if (logRecord.throwable != null) {
            Map<String, Object> exception = getExceptionJson(logRecord);
            message.put(EXCEPTION_ATTR, exception);
        }

        return new JsonValue(message).toString();
    }

    private Map<String, Object> getExceptionJson(DebugRecord logRecord) {
        Map<String, Object> exception = new LinkedHashMap<>();
        Throwable throwable = logRecord.throwable;
        exception.put("className", throwable.getClass().getName());
        if (throwable.getMessage() != null) {
            exception.put("message", throwable.getMessage());
        }
        StringWriter buffer = new StringWriter(DebugConstants.MAX_BUFFER_SIZE_EXCEPTION);
        throwable.printStackTrace(new PrintWriter(buffer, true));
        exception.put(STACK_TRACE_ATTR, buffer.toString());
        return exception;
    }

}
