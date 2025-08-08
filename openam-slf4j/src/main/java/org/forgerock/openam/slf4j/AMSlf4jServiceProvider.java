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

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * SLF4J service provider implementation which registers {@link AMLoggerFactory} as the logger factory.
 */
public class AMSlf4jServiceProvider implements SLF4JServiceProvider {

    /**
     * Version of the SLF4J API this implementation is compiled against.
     */
    public static String REQUESTED_API_VERSION = "2.0.17";

    private final IMarkerFactory markerFactory = new BasicMarkerFactory();

    private MDCAdapter mdcAdapter;

    private ILoggerFactory loggerFactory;

    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return mdcAdapter;
    }

    @Override
    public String getRequestedApiVersion() {
        return REQUESTED_API_VERSION;
    }

    @Override
    public void initialize() {
        if ("com.sun.identity.shared.debug.impl.Slf4jProviderImpl".equals(
                System.getProperty("com.sun.identity.util.debug.provider"))) {
            mdcAdapter = LogbackLoggerAdapter.getMDCAdapter();
        } else {
            mdcAdapter = new NOPMDCAdapter();
        }
        loggerFactory = new AMLoggerFactory();
    }

}
