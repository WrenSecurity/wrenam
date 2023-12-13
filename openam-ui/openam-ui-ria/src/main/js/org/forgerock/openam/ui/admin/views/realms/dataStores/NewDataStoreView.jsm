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
 * Copyright 2023 Wren Security.
 */

import _ from "lodash";
import "bootstrap-tabdrop"; // jquery dependencies
import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import DataStoresService from "org/forgerock/openam/ui/admin/services/realm/DataStoresService";
import SelectComponent from "org/forgerock/openam/ui/common/components/SelectComponent";

function toggleCreateDisabled (el, value) {
    el.find("[data-create]").prop("disabled", value);
}

class NewDataStoreView extends AbstractView {
    constructor () {
        super();

        this.template = "templates/admin/views/realms/dataStores/NewDataStoreTemplate.html";
        this.partials = [
            "partials/alerts/_Alert.html"
        ];
        this.events = {
            "change [data-dataStore-id]": "onDataStoreIdChange",
            "keyup  [data-dataStore-id]": "onDataStoreIdChange",
            "click [data-create]": "onCreateClick"
        };
    }
    render (args, callback) {
        this.data.realmPath = args[0];
        this.data.id = "";

        DataStoresService.getCreatables(this.data.realmPath).then((creatableTypes) => {
            this.data.creatableTypes = creatableTypes;

            this.parentRender(() => {
                const selectComponent = new SelectComponent({
                    options: creatableTypes,
                    onChange: (option) => {
                        this.selectDataStore(option._id);
                        this.validateDataStoreProps();
                    },
                    searchFields: ["name"],
                    labelField: "name",
                    placeholderText: $.t("console.dataStores.edit.dataStoreTypes")
                });
                this.$el.find("[data-dataStore-type]").append(selectComponent.render().el);
                this.$el.find("[autofocus]").focus();
                if (callback) {
                    callback();
                }
            });
        });
    }
    selectDataStore (dataStore) {
        toggleCreateDisabled(this.$el, true);

        if (dataStore !== this.data.type && this.jsonSchemaView) {
            this.jsonSchemaView.remove();
        }

        if (!_.isEmpty(dataStore)) {
            this.data.type = dataStore;

            DataStoresService.getInitialState(this.data.realmPath, this.data.type).then((response) => {
                this.jsonSchemaView = new FlatJSONSchemaView({
                    schema: response.schema,
                    values: response.values,
                    showOnlyRequiredAndEmpty: true,
                    onRendered: () => this.validateDataStoreProps.call(this)
                });
                $(this.jsonSchemaView.render().el).appendTo(this.$el.find("[data-json-form]"));
            }, () => {
                toggleCreateDisabled(this.$el, true);
            });
        }
    }
    onDataStoreIdChange (event) {
        this.data.id = event.target.value;
        this.validateDataStoreProps();
    }
    validateDataStoreProps () {
        const dataStoreId = this.data.id;
        let isNameValid = true;

        if (dataStoreId.indexOf(" ") !== -1) {
            isNameValid = false;
            Messages.addMessage({
                type: Messages.TYPE_DANGER,
                message: $.t("console.dataStores.edit.dataStoreIdValidationError")
            });
        }
        const isValid = isNameValid && !!dataStoreId && !!this.data.type;
        toggleCreateDisabled(this.$el, !isValid);
    }
    onCreateClick () {
        DataStoresService.create(
            this.data.realmPath,
            this.data.type,
            Object.assign({}, this.jsonSchemaView.getData(), { _id: this.data.id })
        )
            .then(() => {
                Router.routeTo(Router.configuration.routes.realmsDataStoreEdit, {
                    args: _.map([this.data.realmPath, this.data.type, this.data.id], encodeURIComponent),
                    trigger: true
                });
            }, (response) => {
                Messages.addMessage({
                    response,
                    type: Messages.TYPE_DANGER
                });
            });
    }
}

export default NewDataStoreView;
