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

import com.sun.identity.shared.debug.IDebug;
import com.sun.identity.shared.debug.IDebugProvider;

/**
 * A provider implementation of the {@link IDebugProvider} interface that creates and returns
 * instances of {@link Slf4jDebugImpl} associated with a specific debug name (typically the name of a service).
 */
public class Slf4jProviderImpl implements IDebugProvider {

    @Override
    public IDebug getInstance(String debugName) {
        return new Slf4jDebugImpl(debugName);
    }
}
