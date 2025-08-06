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

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.DebugConstants;
import com.sun.identity.shared.debug.format.DebugFormatter;

/**
 * Expose a singleton {@link DebugFormatter} determined by the
 * {@value DebugConstants#CONFIG_DEBUG_LOGFILE_FORMAT} system property.
 */
public class DebugFormatterFactory {

    private static DebugFormatter formatterInstance = createInstance();

    private DebugFormatterFactory() {
    }

    private static DebugFormatter createInstance() {
        String logFileFormat = SystemPropertiesManager.get(DebugConstants.CONFIG_DEBUG_LOGFILE_FORMAT);
        return "json".equalsIgnoreCase(logFileFormat) ? new JsonDebugFormatter() : new PlainTextDebugFormatter();
    }

    /**
     * Reset the formatter instance to reflect new system property value.
     */
    public static void resetInstance() {
        formatterInstance = createInstance();
    }

    /**
     * Return the formatter instance selected for the current run.
     */
    public static DebugFormatter getInstance() {
        return formatterInstance;
    }

}
