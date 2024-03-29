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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.core.rest.record;

import org.forgerock.json.JsonValue;

/**
 * Manage the records.
 */
public interface DebugRecorder {

    /**
     * Start recording
     *
     * @param jsonProperties record json properties
     * @throws RecordException if y
     */
    void startRecording(JsonValue jsonProperties) throws RecordException;

    /**
     * Get the current record if exist
     *
     * @return the current record or null if we are not recording
     */
    Record getCurrentRecord();

    /**
     * Stop recording
     *
     * @return the stopped record or null if we are not recording
     */
    Record stopRecording() throws RecordException;

}
