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
 * Portions copyright 2014-2016 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/openam/ui/admin/models/authorization/PolicySetModel",
    "org/forgerock/openam/ui/admin/views/realms/authorization/common/StripedListView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/PoliciesView",
    "org/forgerock/openam/ui/admin/services/realm/PoliciesService",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "bootstrap-tabdrop",
    "selectize"
], function ($, _, PolicySetModel, StripedListView, PoliciesView, PoliciesService, FormHelper, Messages, AbstractView,
        EventManager, Router, Constants, UIUtils) {
    return AbstractView.extend({
        partials: [
            "partials/util/_HelpLink.html"
        ],
        APPLICATION_TYPE: "iPlanetAMWebAgentService",
        validationFields: ["name", "resourceTypeUuids"],
        events: {
            "click [data-save]": "submitForm",
            "click [data-delete]": "onDeleteClick"
        },

        initialize () {
            AbstractView.prototype.initialize.call(this);
            this.model = null;
        },

        onModelSync () {
            this.renderAfterSyncModel();
        },

        render (args, callback) {
            var policySetName = args[1];

            this.realmPath = args[0];

            if (callback) {
                this.renderCallback = callback;
            }

            this.appTypePromise = PoliciesService.getApplicationType(this.APPLICATION_TYPE);
            this.envConditionsPromise = PoliciesService.getEnvironmentConditions();
            this.subjConditionsPromise = PoliciesService.getSubjectConditions();
            this.decisionCombinersPromise = PoliciesService.getDecisionCombiners();
            this.resourceTypesPromise = PoliciesService.listResourceTypes();

            if (policySetName) {
                this.template = "templates/admin/views/realms/authorization/policySets/EditPolicySetTemplate.html";
                this.data.headerActions = [
                    { actionPartial: "form/_Button", data:"delete", title:"common.form.delete", icon:"fa-times" },
                    { actionPartial: "util/_HelpLink", helpLink: "backstage.authz.policySets" }
                ];
                this.model = new PolicySetModel({ name: policySetName });
                this.listenTo(this.model, "sync", this.onModelSync);
                this.model.fetch();
            } else {
                this.data.newEntity = true;
                this.template = "templates/admin/views/realms/authorization/policySets/NewPolicySetTemplate.html";
                this.model = new PolicySetModel();
                this.data.headerActions = [{ actionPartial: "util/_HelpLink", helpLink: "backstage.authz.policySets" }];
                this.listenTo(this.model, "sync", this.onModelSync);
                this.renderAfterSyncModel();
            }
        },

        renderAfterSyncModel () {
            this.data.entity = this.model.attributes;
            this.data.displayName = this.model.displayName;

            if (!this.data.entity.realm) {
                this.data.entity.realm = this.realmPath;
            }

            this.renderApplication();
        },

        renderApplication () {
            var self = this,
                parentRenderCallback = function () {
                    self.parentRender(function () {
                        PoliciesView.render({
                            realmPath: self.realmPath,
                            policySetModel: self.model
                        }, function () {
                            self.$el.find(".tab-menu .nav-tabs").tabdrop();
                            UIUtils.fillTemplateWithData(
                                "templates/admin/views/realms/authorization/policySets/PolicySetSettingsTemplate.html",
                                self.data,
                                function (tpl) {
                                    self.$el.find("#policySetSettings").append(tpl);
                                    self.populateResourceTypes();
                                    FormHelper.setActiveTab(self);
                                    self.$el.find("#policySetSettings [autofocus]").focus();
                                    if (self.renderCallback) {
                                        self.renderCallback();
                                    }
                                });
                        });
                    });
                },
                populateAvailableResourceTypes = function (resourceTypes) {
                    var options = {};

                    options.allResourceTypes = resourceTypes;
                    options.availableResourceTypes = _.filter(resourceTypes, function (item) {
                        return !_.contains(self.data.entity.resourceTypeUuids, item.uuid);
                    });

                    options.selectedResourceTypes = _.findByValues(options.allResourceTypes, "uuid",
                        self.data.entity.resourceTypeUuids);

                    options.selectedResourceTypesInitial = _.clone(options.selectedResourceTypes);

                    return options;
                };

            if (this.model.id) {
                this.resourceTypesPromise.done(function (resourceTypes) {
                    _.extend(self.data, { options: populateAvailableResourceTypes(resourceTypes.result) });
                    parentRenderCallback();
                });
            } else {
                // Fill in the necessary information about application
                $.when(this.appTypePromise, this.envConditionsPromise, this.subjConditionsPromise,
                    this.decisionCombinersPromise, this.resourceTypesPromise)
                    .done(function (appType, envConditions, subjConditions, decisionCombiners, resourceTypes) {
                        self.data.entity.applicationType = self.APPLICATION_TYPE;
                        self.processConditions(self.data, envConditions[0].result, subjConditions[0].result);
                        self.data.entity.entitlementCombiner = decisionCombiners[0].result[0].title;
                        _.extend(self.data, { options: populateAvailableResourceTypes(resourceTypes[0].result) });
                        parentRenderCallback();
                    });
            }
        },

        populateResourceTypes () {
            var self = this;

            this.resTypesSelection = this.$el.find("#resTypesSelection").selectize({
                sortField: "name",
                valueField: "uuid",
                labelField: "name",
                searchField: "name",
                options: this.data.options.allResourceTypes,
                onChange (value) {
                    self.data.entity.resourceTypeUuids = value;
                }
            });
        },

        processConditions (data, envConditions, subjConditions) {
            if (!data.entityId) {
                data.entity.conditions = this.populateConditions(envConditions, envConditions);
                data.entity.subjects = this.populateConditions(subjConditions, subjConditions);
            }
        },

        populateConditions (selected, available) {
            var result = [];
            _.each(available, function (cond) {
                result.push(cond.title);
            });
            return result;
        },

        submitForm (e) {
            e.preventDefault();

            var self = this,
                savePromise,
                nonModifiedAttributes = _.clone(this.model.attributes);

            this.updateFields();
            this.activeTabId = this.$el.find(".tab-menu li.active a").attr("href");

            _.extend(this.model.attributes, this.data.entity);
            savePromise = this.model.save();

            if (savePromise) {
                savePromise
                    .done(function () {
                        if (self.data.newEntity) {
                            Router.routeTo(Router.configuration.routes.realmsPolicySetEdit, {
                                args: _.map([self.realmPath, self.model.id], encodeURIComponent),
                                trigger: true
                            });
                        } else {
                            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                        }
                    });
            } else {
                _.extend(this.model.attributes, nonModifiedAttributes);
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.validationError);
            }
        },

        onDeleteClick (e) {
            e.preventDefault();

            FormHelper.showConfirmationBeforeDeleting({ type: $.t("console.authorization.common.policySet") },
                _.bind(this.deletePolicySet, this));
        },

        deletePolicySet () {
            var self = this,
                onSuccess = function () {
                    Router.routeTo(Router.configuration.routes.realmsPolicySets, {
                        args: [encodeURIComponent(self.realmPath)],
                        trigger: true
                    });
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                },
                onError = function (model, response) {
                    Messages.addMessage({
                        response,
                        type: Messages.TYPE_DANGER
                    });
                };

            this.model.destroy({
                success: onSuccess,
                error: onError,
                wait: true
            });
        },

        updateFields () {
            const dataFields = this.$el.find("[data-field]");

            _.each(dataFields, (field) => {
                const dataField = field.getAttribute("data-field");

                if (field.type === "checkbox") {
                    if (field.checked) {
                        this.data.entity[dataField].push(field.value);
                    }
                } else {
                    this.data.entity[dataField] = _.trim(field.value);
                }
            });
        }
    });
});
