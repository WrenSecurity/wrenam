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
 * Copyright 2016 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/views/configuration/global/EditGlobalServiceView
 */
define([
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/admin/services/global/ServicesService",
    "org/forgerock/openam/ui/admin/views/common/Backlink",
    "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent"
], (AbstractView, UIUtils, ServicesService, Backlink, EditSchemaComponent) => {
    const EditGlobalServiceView = AbstractView.extend({
        template: "templates/admin/views/configuration/EditGlobalConfigurationBaseTemplate.html",
        render ([serviceType]) {
            const editComponent = new EditSchemaComponent({
                data: { serviceType },

                template: "templates/admin/views/configuration/EditGlobalConfigurationTemplate.html",
                subSchemaTemplate: "templates/admin/views/configuration/global/SubSchemaListTemplate.html",

                getInstance: () => ServicesService.instance.get(serviceType),
                updateInstance: (values) => ServicesService.instance.update(serviceType, values),

                getSubSchemaTypes: () => ServicesService.type.subSchema.type.getAll(serviceType),
                getSubSchemaCreatableTypes: () => ServicesService.type.subSchema.type.getCreatables(serviceType),
                getSubSchemaInstances: () => ServicesService.type.subSchema.instance.getAll(serviceType),
                deleteSubSchemaInstance: (subSchemaType, subSchemaInstance) =>
                    ServicesService.type.subSchema.instance.remove(serviceType, subSchemaType, subSchemaInstance)
            });

            this.parentRender(() => {
                new Backlink().render();
                this.$el.find("[data-global-configuration]").append(editComponent.render().$el);
            });
        }
    });

    return new EditGlobalServiceView();
});
