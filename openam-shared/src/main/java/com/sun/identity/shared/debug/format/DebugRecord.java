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
package com.sun.identity.shared.debug.format;

import com.sun.identity.shared.debug.DebugLevel;
import java.time.Instant;

/**
 * Represent an immutable value object containing all data used to write a single debug log entry.
 */
public class DebugRecord {

    public final Instant timestamp;
    public final DebugLevel level;
    public final String thread;
    public final String logger;
    public final String transactionId;
    public final String message;
    public final Throwable throwable;

    public DebugRecord(
            Instant timestamp,
            DebugLevel level,
            String thread,
            String logger,
            String transactionId,
            String message,
            Throwable throwable) {
        this.timestamp = timestamp;
        this.level = level;
        this.thread = thread;
        this.logger = logger;
        this.transactionId = transactionId;
        this.message = message;
        this.throwable = throwable;
    }

}
