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
 * Portions Copyright 2023 Wren Security
 */
package org.forgerock.openam.sm.datalayer.impl.tasks;


import java.util.Collection;

import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.sm.datalayer.api.AbstractTask;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.util.Reject;

/**
 * Performs a partial query against the persistence layer. Partial queries operate like normal queries
 * except that the results are not full Token instances. Instead they are a collection
 * of Name/Value pairs which represent a subset of a Token.
 *
 * @see QueryTask
 */
public class PartialQueryTask extends AbstractTask {
    private final TokenFilter tokenFilter;

    /**
     * @param tokenFilter Non null and must define at least one Return Attribute.
     * @param handler Non null, required for asynchronous response.
     */
    public PartialQueryTask(TokenFilter tokenFilter, ResultHandler<Collection<PartialToken>, ?> handler) {
        super(handler);
        this.tokenFilter = tokenFilter;
    }

    /**
     * Performs the partial query operation from the persistence store using the TokenStorageAdapter.
     *
     * @param adapter Utility functions to perform the task with.
     * @throws IllegalArgumentException If the TokenFilter did not define any return fields.
     */
    @Override
    public void performTask(TokenStorageAdapter adapter) throws DataLayerException {
        Reject.ifTrue(tokenFilter.getReturnFields().isEmpty());

        handler.processResults(adapter.partialQuery(tokenFilter));
    }

    @Override
    public String toString() {
        return "PartialQueryTask: " + tokenFilter;
    }
}
