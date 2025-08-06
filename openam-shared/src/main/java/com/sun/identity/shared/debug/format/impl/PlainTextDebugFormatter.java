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

/**
 * Format {@link DebugRecord DebugRecords} in the legacy plain text layout.
 */
public class PlainTextDebugFormatter implements DebugFormatter {

    @Override public String format(DebugRecord logRecord) {
        StringBuilder buffer = new StringBuilder()
            .append(logRecord.logger)
            .append(':').append(DebugConstants.DEBUG_DATE_FORMATTER.format(logRecord.timestamp))
            .append(": ").append(logRecord.thread)
            .append(": TransactionId[").append(logRecord.transactionId).append(']')
            .append('\n');

        switch (logRecord.level) {
            case WARNING:
                buffer.append("WARNING: ");
                break;
            case ERROR:
                buffer.append("ERROR: ");
                break;
            default:
                break;
        }
        buffer.append(logRecord.message);

        if (logRecord.throwable != null) {
            StringWriter stringBuffer = new StringWriter(DebugConstants.MAX_BUFFER_SIZE_EXCEPTION);
            PrintWriter stackTraceStream = new PrintWriter(stringBuffer);
            logRecord.throwable.printStackTrace(stackTraceStream);
            stackTraceStream.flush();
            buffer.append('\n').append(stringBuffer);
        }
        return buffer.toString();
    }

}
