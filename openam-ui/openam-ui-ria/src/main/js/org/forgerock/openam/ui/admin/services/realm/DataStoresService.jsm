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
 * Copyright 2024 Wren Security.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/DataStoresService
 */

import _ from "lodash";
import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";
import Promise from "org/forgerock/openam/ui/common/util/Promise";

const obj = new AbstractDelegate(`${Constants.host}/${Constants.context}/json`);
const basePath = "/realm-config/services/id-repos";
const baseHeaders = { "Accept-API-Version": "protocol=1.0,resource=1.0" };

const getDataStoreSchema = (realm, type) => (
    obj.serviceCall({
        url: fetchUrl(`${basePath}/${type}?_action=schema`, { realm }),
        headers: baseHeaders,
        type: "POST"
    }).then((response) => {
        // Filter out attributes that are not nested in a section to match openam-console behaviour
        const properties = Object.entries(response.properties).filter((p) => p[1].type === "object");
        return new JSONSchema(Object.assign(response, { properties: Object.fromEntries(properties) }));
    })
);

export const getAll = (realm) => (
    obj.serviceCall({
        url: fetchUrl(`${basePath}?_action=nextdescendents`, { realm }),
        headers: baseHeaders,
        type: "POST"
    }).then((response) => _.sortBy(response.result, "_id"))
);

export const get = (realm, type, id) => {
    const getInstance = () => (
        obj.serviceCall({
            url: fetchUrl(`${basePath}/${type}/${id}`, { realm }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        })
    );

    return Promise.all([getDataStoreSchema(realm, type), getInstance()]).then((response) => ({
        name: response[1][0]._type.name,
        schema: response[0],
        values: new JSONValues(response[1][0])
    }));
};

export const getInitialState = (realm, type) => {
    const getTemplate = () => (
        obj.serviceCall({
            url: fetchUrl(`${basePath}/${type}?_action=template`, { realm }),
            headers: baseHeaders,
            type: "POST"
        }).then((response) => new JSONValues(response))
    );

    return Promise.all([getDataStoreSchema(realm, type), getTemplate()]).then((response) => ({
        schema: response[0],
        values: response[1]
    }));
};

export const remove = (realm, typesAndIds) => {
    const promises = _.map(typesAndIds, ([type, id]) => obj.serviceCall({
        url: fetchUrl(`${basePath}/${type}/${id}`, { realm }),
        headers: baseHeaders,
        type: "DELETE"
    }));

    return Promise.all(promises);
};

export const update = (realm, type, id, data) => (
    obj.serviceCall({
        url: fetchUrl(`${basePath}/${type}/${id}`, { realm }),
        headers: baseHeaders,
        type: "PUT",
        data: new JSONValues(data).toJSON()
    }).then((response) => new JSONValues(response))
);

export const create = (realm, type, data) => (
    obj.serviceCall({
        url: fetchUrl(`${basePath}/${type}?_action=create`, { realm }),
        headers: baseHeaders,
        type: "POST",
        data: new JSONValues(data).toJSON()
    })
);

export const getCreatables = (realm) => (
    obj.serviceCall({
        url: fetchUrl(`${basePath}?_action=getCreatableTypes`, { realm }),
        headers: baseHeaders,
        type: "POST"
    }).then((response) => _.sortBy(response.result, "name"))
);
