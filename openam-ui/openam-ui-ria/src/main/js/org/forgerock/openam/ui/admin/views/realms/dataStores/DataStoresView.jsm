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

import $ from "jquery";
import _ from "lodash";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import { getAll, getCreatables, remove } from "org/forgerock/openam/ui/admin/services/realm/DataStoresService";
import Router from "org/forgerock/commons/ui/common/main/Router";
import showConfirmationBeforeAction from "org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeAction";
import Promise from "org/forgerock/openam/ui/common/util/Promise";

function getDataStoreTypeAndIdFromElement (element) {
    const tr = $(element).closest("tr");
    return [tr.data("datastoreType"), tr.data("datastoreId")];
}

function deleteDataStores (typesAndIds) {
    showConfirmationBeforeAction({
        message: $.t("console.dataStores.list.confirmDeleteSelected", { count: typesAndIds.length })
    }, () => {
        remove(this.data.realmPath, typesAndIds).then(() => {
            this.rerender();
        }, (response) => {
            Messages.addMessage({ type: Messages.TYPE_DANGER, response });
            this.rerender();
        });
    });
}

class DataStoresView extends AbstractView {
    constructor () {
        super();

        this.template = "templates/admin/views/realms/dataStores/DataStoresTemplate.html";
        this.events = {
            "change [data-select-dataStore]": "dataStoreSelected",
            "click [data-delete-dataStore]":  "onDeleteSingle",
            "click [data-delete-dataStores]": "onDeleteMultiple",
            "click [data-add-dataStore]":     "onAddDataStore"
        };
    }
    dataStoreSelected (event) {
        const anyDataStoresSelected = this.$el.find("input[type=checkbox]").is(":checked");
        const row = $(event.currentTarget).closest("tr");

        row.toggleClass("selected");
        this.$el.find("[data-delete-dataStores]").prop("disabled", !anyDataStoresSelected);
    }
    onAddDataStore (event) {
        event.preventDefault();
        Router.routeTo(Router.configuration.routes.realmsDataStoreNew, {
            args: [encodeURIComponent(this.data.realmPath)],
            trigger: true
        });
    }
    onDeleteSingle (event) {
        event.preventDefault();
        const typeAndId = getDataStoreTypeAndIdFromElement(event.currentTarget);
        _.bind(deleteDataStores, this)([typeAndId]);
    }
    onDeleteMultiple (event) {
        event.preventDefault();
        const typesAndIds = _(this.$el.find("input[type=checkbox]:checked")).toArray()
            .map(getDataStoreTypeAndIdFromElement).value();
        _.bind(deleteDataStores, this)(typesAndIds);
    }
    validateAddButton (creatables) {
        if (!_.isEmpty(creatables)) {
            return;
        }
        this.$el.find("[data-add-dataStore]").prop("disabled", true).popover({
            trigger : "hover",
            container : "body",
            placement : "top",
            content: $.t("console.dataStores.edit.unavaliable")
        });
    }
    render (args, callback) {
        this.data.args = args;
        this.data.realmPath = args[0];

        Promise.all([
            getAll(this.data.realmPath),
            getCreatables(this.data.realmPath)
        ]).then((data) => {
            this.data.dataStores = data[0];
            this.parentRender(() => {
                this.validateAddButton(data[1]);
                if (callback) {
                    callback();
                }
            });
        });
    }
    rerender () {
        this.render(this.data.args);
    }
}

export default DataStoresView;
