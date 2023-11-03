/**
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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2023 Wren Security
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/UsersService
 */

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";

const obj = new AbstractDelegate(`${Constants.host}/${Constants.context}/json`);

/**
 * Fetch a list of users from the realm that start with the prefix.
 *
 * @param {string} id User ID prefix.
 * @param {(string|boolean)} [realm=false] Realm.
 * @returns {Promise<Array.<string>>} Promise with list of users.
 */
export function getByIdStartsWith (id, realm = false) {
    return obj.serviceCall({
        url: fetchUrl(`/users?_queryId=${id}*`, { realm }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    }).then((response) => response.result);
}
