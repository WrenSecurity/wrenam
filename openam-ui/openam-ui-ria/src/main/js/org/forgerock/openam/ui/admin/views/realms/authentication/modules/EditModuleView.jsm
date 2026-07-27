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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2026 Wren Security.
 */

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import AuthenticationService from "org/forgerock/openam/ui/admin/services/realm/AuthenticationService";
import EditSchemaComponent from "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent";

class EditModuleView extends AbstractView {
    render ([realmPath, moduleType, moduleName]) {
        const editComponent = new EditSchemaComponent({
            data: { moduleName },
            template: "templates/admin/views/realms/authentication/modules/EditModuleViewTemplate.html",
            getInstance: () => AuthenticationService.authentication.modules.get(realmPath, moduleName, moduleType),
            updateInstance: (values) =>
                AuthenticationService.authentication.modules.update(realmPath, moduleName, moduleType, values)
        });

        this.parentRender(() => { this.$el.append(editComponent.render().$el); });
    }
}

export default EditModuleView;
