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
package org.forgerock.openam.slf4j;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.Configurator.ExecutionStatus;
import ch.qos.logback.classic.util.DefaultJoranConfigurator;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.StatusUtil;
import ch.qos.logback.core.util.StatusPrinter;
import com.sun.identity.common.ShutdownManager;
import java.net.URL;
import java.util.Objects;
import org.forgerock.util.thread.listener.ShutdownPriority;
import org.slf4j.Logger;
import org.slf4j.helpers.Reporter;
import org.slf4j.spi.MDCAdapter;

final class LogbackLoggerAdapter {

    public static final LoggerContext loggerContext = new LoggerContext();

    private static final LogbackMDCAdapter mdcAdapter = new LogbackMDCAdapter();

    static {
        loggerContext.setName("amDebug");
        loggerContext.setMDCAdapter(mdcAdapter);
        initializeLoggerContext();
        loggerContext.start();
        ShutdownManager.getInstance()
                .addShutdownListener(loggerContext::stop, ShutdownPriority.LOWEST);
    }

    private static void initializeLoggerContext() {
        DefaultJoranConfigurator defaultJoranConfigurator = new DefaultJoranConfigurator();
        defaultJoranConfigurator.setContext(loggerContext);
        ExecutionStatus executionStatus = defaultJoranConfigurator.configure(loggerContext);
        if (executionStatus == ExecutionStatus.INVOKE_NEXT_IF_ANY) {
            // Didn't find a configuration file on the classpath, continue with AM's default configuration file
            URL defaultConfiguration = LogbackLoggerAdapter.class.getResource("/am-logback.xml");
            JoranConfigurator joranConfigurator = new JoranConfigurator();
            joranConfigurator.setContext(loggerContext);
            try {
                joranConfigurator.doConfigure(Objects.requireNonNull(defaultConfiguration));
            } catch (JoranException je) {
                Reporter.error("Failed to configure logger context", je);
            }
        }
        if (!StatusUtil.contextHasStatusListener(loggerContext)) {
            StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
        }
    }

    private LogbackLoggerAdapter() {
    }

    static MDCAdapter getMDCAdapter() {
        return mdcAdapter;
    }

    static Logger getLogger(String name) {
        return loggerContext.getLogger(name);
    }

}
