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
 * Copyright 2024 Wren Security.
 */

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Router from "org/forgerock/commons/ui/common/main/Router";
import { get, update, remove } from "org/forgerock/openam/ui/admin/services/realm/DataStoresService";
import EditSchemaComponent from "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent";

class EditDataStoreView extends AbstractView {
    render ([realmPath, type, id]) {
        const editComponent = new EditSchemaComponent({
            data: {
                realmPath,
                type,
                id,
                headerActions: [
                    { actionPartial: "form/_Button", data: "delete", title: "common.form.delete", icon: "fa-times" }
                ]
            },
            listRoute: Router.configuration.routes.realmsDataStores,
            listRouteArgs: [encodeURIComponent(realmPath)],
            template: "templates/admin/views/realms/dataStores/EditDataStoreTemplate.html",

            getInstance: () => get(realmPath, type, id),
            updateInstance: (values) => update(realmPath, type, id, values),
            deleteInstance: () => remove(realmPath, [[type, id]])
        });

        this.parentRender(() => { this.$el.append(editComponent.render().$el); });
    }
}

export default EditDataStoreView;
