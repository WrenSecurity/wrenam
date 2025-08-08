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
package com.sun.identity.shared.debug.impl;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.IDebug;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public final class Slf4jDebugImpl implements IDebug {

    private final String debugName;

    private final Logger logger;

    public Slf4jDebugImpl(String debugName) {
        this.debugName = debugName;
        this.logger = LoggerFactory.getLogger(debugName);
    }

    @Override
    public String getName() {
        return debugName;
    }

    @Override
    public int getState() {
        return -1;
    }

    @Override
    public void setDebug(int level) {
    }

    @Override
    public void setDebug(String strDebugLevel) {
    }

    @Override
    public void resetDebug(String mf) {
    }

    @Override
    public boolean messageEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean warningEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public boolean errorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void message(String message, Throwable throwable) {
        withTransactionId(() -> {
            logger.debug(message, throwable);
        });
    }

    @Override
    public void warning(String message, Throwable throwable) {
        withTransactionId(() -> {
            logger.warn(message, throwable);
        });
    }

    @Override
    public void error(String message, Throwable throwable) {
        withTransactionId(() -> {
            logger.error(message, throwable);
        });
    }

    private void withTransactionId(Runnable runnable) {
        String transactionId;
        if (SystemPropertiesManager.getAsBoolean(Constants.SERVER_MODE)) {
            transactionId = AuditRequestContext.getTransactionIdValue();
        } else {
            transactionId = "unknown";
        }
        try (MDC.MDCCloseable ignored = MDC.putCloseable("transactionId", transactionId)) {
            runnable.run();
        }
    }
}
