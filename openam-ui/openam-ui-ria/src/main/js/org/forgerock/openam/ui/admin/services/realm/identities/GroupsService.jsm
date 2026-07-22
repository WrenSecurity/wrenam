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
 * @module org/forgerock/openam/ui/admin/services/realm/identities/GroupsService
 */
import { map, omit, omitBy, startsWith } from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import constructFieldParams from "org/forgerock/openam/ui/admin/services/constructFieldParams";
import constructPaginationParams from "org/forgerock/openam/ui/admin/services/constructPaginationParams";
import fetchUrl from "api/fetchUrl";

const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

export function getSchema (realm) {
    return obj.serviceCall({
        url: fetchUrl("/groups?_action=schema", { realm }),
        headers: { "Accept-API-Version": "protocol=2.0,resource=1.0" },
        type: "POST"
    });
}

export function get (realm, id) {
    return obj.serviceCall({
        url: fetchUrl(`/groups/${encodeURIComponent(id)}`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.1,resource=4.0" }
    });
}

export function getAll (realm, additionalParams = {}) {
    const pagination = constructPaginationParams(additionalParams.pagination);
    const fields = constructFieldParams(additionalParams.fields);
    return obj.serviceCall({
        url: fetchUrl(`/groups?_queryFilter=true${pagination}${fields}`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.1,resource=4.0" }
    });
}

export function remove (realm, ids) {
    const promises = map(ids, (id) => obj.serviceCall({
        url: fetchUrl(`/groups/${encodeURIComponent(id)}`, { realm }),
        headers: { "Accept-API-Version": "protocol=2.1,resource=4.0" },
        type: "DELETE"
    }));

    return Promise.all(promises);
}

export function create (realm, id) {
    return obj.serviceCall({
        url: fetchUrl(`/groups/${encodeURIComponent(id)}`, { realm }),
        type: "PUT",
        headers: {
            "Accept-API-Version": "protocol=2.1,resource=4.0",
            "If-None-Match": "*"
        },
        data: "{}"
    });
}

export function update (realm, data, id) {
    const omitSchemaConfigViolationProperties = (obj) => omit(obj, ["dn", "objectclass"]);
    const omitReadOnlyProperties = (obj) => omitBy(obj, (prop, key) => startsWith(key, "_"));

    return obj.serviceCall({
        url: fetchUrl(`/groups/${encodeURIComponent(id)}`, { realm }),
        type: "PUT",
        headers: {
            "Accept-API-Version": "protocol=2.1,resource=4.0",
            "If-Match": "*"
        },
        // CREST Protocol 2.0 payload must not transmit _rev
        data: JSON.stringify(omitReadOnlyProperties(omitSchemaConfigViolationProperties(data)))
    });
}
