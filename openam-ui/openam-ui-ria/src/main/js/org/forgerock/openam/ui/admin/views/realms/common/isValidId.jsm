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
 * @module org/forgerock/openam/ui/admin/views/realms/common/isValidId
 */

import { endsWith, includes, isEmpty, startsWith } from "lodash";

/**
  * Validates that the given Id satisfies the following conditions:
  * Does not start with the '#' or '"' characters
  * Does not start or end with the space character
  * Does not contain the '\', '/', '+', ';' or ',' characters
  * Is not be '.' or '..'
  * Or is an empty string
  * @param {string} id The Id to validate
  * @returns {boolean} Returns true if the Id does not start with a '#', `"`, or contain
  * the '\', '/', '+', ';' or ',' characters, or start or end with the space character, is not '.' or '..', or is empty.
  */
const isValidId = (id) => {
    if (isEmpty(id)) {
        return true;
    }
    if (id === "." || id === ".." ||
        startsWith(id, " ") ||
        endsWith(id, " ") ||
        startsWith(id, "#") ||
        startsWith(id, "\"") ||
        includes(id, "\\") ||
        includes(id, "/") ||
        includes(id, "+") ||
        includes(id, ";") ||
        includes(id, ",") ||
        includes(id, "%") ||
        includes(id, "[") ||
        includes(id, "]") ||
        includes(id, "|") ||
        includes(id, "?")) {
        return false;
    }

    return true;
};

export default isValidId;
