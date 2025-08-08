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
import java.util.LinkedHashMap;
import java.util.Map;
import org.forgerock.json.JsonValue;

/**
 * Format {@link DebugRecord DebugRecords} as a serialized JSON object.
 */
public class JsonDebugFormatter implements DebugFormatter {

    @Override
    public String format(DebugRecord logRecord) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("timestamp", DebugConstants.DEBUG_DATE_FORMATTER.format(logRecord.timestamp));
        message.put("level", logRecord.level.getName());
        message.put("thread", logRecord.thread);
        message.put("logger", logRecord.logger);
        message.put("message", logRecord.message);
        message.put("transactionId", logRecord.transactionId);

        if (logRecord.throwable != null) {
            Map<String, Object> exception = getExceptionJson(logRecord);
            message.put("exception", exception);
        }

        return new JsonValue(message).toString();
    }

    private Map<String, Object> getExceptionJson(DebugRecord logRecord) {
        Map<String, Object> exception = new LinkedHashMap<>();
        Throwable throwable = logRecord.throwable;
        exception.put("class", throwable.getClass().getName());
        if (throwable.getMessage() != null) {
            exception.put("message", throwable.getMessage());
        }
        StringWriter stringBuffer = new StringWriter(DebugConstants.MAX_BUFFER_SIZE_EXCEPTION);
        PrintWriter stackTraceStream = new PrintWriter(stringBuffer);
        throwable.printStackTrace(stackTraceStream);
        stackTraceStream.flush();
        exception.put("stackTrace", stringBuffer.toString());
        return exception;
    }

}
