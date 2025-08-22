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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2025 Wren Security.
 */
package com.sun.identity.shared.debug.file.impl;

import static com.sun.identity.shared.debug.DebugConstants.DEBUG_DATE_FORMATTER;
import static org.forgerock.openam.utils.Time.newDate;

import com.sun.identity.shared.debug.file.DebugFile;
import com.sun.identity.shared.debug.format.DebugRecord;
import com.sun.identity.shared.debug.format.impl.DebugFormatterFactory;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Debug file dedicated to std out
 */
public class StdDebugFile implements DebugFile {

    private static final StdDebugFile INSTANCE = new StdDebugFile();

    private final PrintWriter stdoutWriter = new PrintWriter(System.out, true);

    private StdDebugFile() {
    }

    /**
     * Get std out debug file
     *
     * @return std debug file
     */
    public static StdDebugFile getInstance() {
        return INSTANCE;
    }

    @Override
    public void write(DebugRecord logRecord) throws IOException {
        stdoutWriter.println(DebugFormatterFactory.getInstance().format(logRecord));
    }

    /**
     * Printing error directly into the stdout. A log header will be generated
     *
     * @param debugName debug name
     * @param message   the error message
     * @param ex        the exception (can be null)
     */
    public static void printError(String debugName, String message, Throwable ex) {
        String timestamp = DEBUG_DATE_FORMATTER.format(newDate().toInstant());
        String prefix = debugName + ":" + timestamp + ": " + Thread.currentThread() + "\n";

        System.err.println(prefix + message);
        if (ex != null) {
            ex.printStackTrace(System.err);
        }
    }

}
