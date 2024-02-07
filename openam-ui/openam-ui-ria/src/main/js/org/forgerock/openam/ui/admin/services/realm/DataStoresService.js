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

define([
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues",
    "org/forgerock/openam/ui/common/services/fetchUrl",
    "org/forgerock/openam/ui/common/util/Promise"
], (_, AbstractDelegate, Constants, JSONSchema, JSONValues, fetchUrl, Promise) => {
    /**
     * @exports org/forgerock/openam/ui/admin/services/realm/DataStoresService
     */
    const obj = new AbstractDelegate(`${Constants.host}/${Constants.context}/json`);
    const basePath = "/realm-config/services/id-repos";
    const baseHeaders = { "Accept-API-Version": "protocol=1.0,resource=1.0" };

    const getDataStoreSchema = (realm, type) => (
        obj.serviceCall({
            url: fetchUrl.default(`${basePath}/${type}?_action=schema`, { realm }),
            headers: baseHeaders,
            type: "POST"
        }).then((response) => {
            // Filter out attributes that are not nested in a section to match openam-console behaviour
            const properties = Object.entries(response.properties).filter((p) => p[1].type === "object");
            return new JSONSchema({
                ...response,
                properties: Object.fromEntries(properties)
            });
        })
    );

    obj.getAll = (realm) => (
        obj.serviceCall({
            url: fetchUrl.default(`${basePath}?_action=nextdescendents`, { realm }),
            headers: baseHeaders,
            type: "POST"
        }).then((response) => _.sortBy(response.result, "_id"))
    );

    obj.get = (realm, type, id) => {
        const getInstance = () => (
            obj.serviceCall({
                url: fetchUrl.default(`${basePath}/${type}/${id}`, { realm }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            })
        );

        return Promise.all([getDataStoreSchema(realm, type), getInstance()]).then((response) => ({
            name: response[1][0]._type.name,
            schema: response[0],
            values: new JSONValues(response[1][0])
        }));
    };

    obj.getInitialState = (realm, type) => {
        const getTemplate = () => (
            obj.serviceCall({
                url: fetchUrl.default(`${basePath}/${type}?_action=template`, { realm }),
                headers: baseHeaders,
                type: "POST"
            }).then((response) => new JSONValues(response))
        );

        return Promise.all([getDataStoreSchema(realm, type), getTemplate()]).then((response) => ({
            schema: response[0],
            values: response[1]
        }));
    };

    obj.remove = (realm, typesAndIds) => {
        const promises = _.map(typesAndIds, ([type, id]) => obj.serviceCall({
            url: fetchUrl.default(`${basePath}/${type}/${id}`, { realm }),
            headers: baseHeaders,
            type: "DELETE"
        }));

        return Promise.all(promises);
    };

    obj.update = (realm, type, id, data) => (
        obj.serviceCall({
            url: fetchUrl.default(`${basePath}/${type}/${id}`, { realm }),
            headers: baseHeaders,
            type: "PUT",
            data: new JSONValues(data).toJSON()
        }).then((response) => new JSONValues(response))
    );

    obj.create = (realm, type, data) => (
        obj.serviceCall({
            url: fetchUrl.default(`${basePath}/${type}?_action=create`, { realm }),
            headers: baseHeaders,
            type: "POST",
            data: new JSONValues(data).toJSON()
        })
    );

    obj.getCreatables = (realm) => (
        obj.serviceCall({
            url: fetchUrl.default(`${basePath}?_action=getCreatableTypes`, { realm }),
            headers: baseHeaders,
            type: "POST"
        }).then((response) => _.sortBy(response.result, "name"))
    );

    return obj;
});
