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
 * Copyright 2018-2019 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/constructFieldParams
 */

/**
 * Assembles the fields query parameter string.
 * @param {Array<string>} [fields] An array of fields to include
 * @returns {string} Returns the _fields query string or empty string.
 */
const constructFieldParams = (fields) => {
    if (fields) {
        return `&_fields=${fields.join(",")}`;
    } else {
        return "";
    }
};

export default constructFieldParams;
