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
 * Portions copyright 2025 Wren Security.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/admin/services/realm/PoliciesService",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ConditionAttrBooleanView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ConditionAttrArrayView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ConditionAttrStringView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ConditionAttrObjectView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ConditionAttrEnumView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ConditionAttrTimeView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ConditionAttrDayView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ConditionAttrDateView",
    "handlebars"
], function ($, _, AbstractView, UIUtils, PoliciesService, BooleanAttr, ArrayAttr, StringAttr, ObjectAttr, EnumAttr,
        TimeAttr, DayAttr, DateAttr, Handlebars) {
    return AbstractView.extend({
        template: "templates/admin/views/realms/authorization/policies/conditions/EditEnvironmentTemplate.html",
        events: {
            "change [data-type-selection]": "changeType"
        },
        data: {},
        i18n: {
            "condition": {
                "key": "console.authorization.policies.edit.conditionTypes.",
                "title": ".title",
                "props": ".props."
            }
        },
        SCRIPT_RESOURCE: "Script",

        render (schema, element, itemID, itemData, editing = false) {
            var self = this,
                hiddenData = {};

            this.setElement(element);

            this.data = $.extend(true, [], schema);
            this.data.itemID = itemID;

            _.each(this.data.conditions, function (condition) {
                condition.i18nKey = $.t(self.i18n.condition.key + condition.title + self.i18n.condition.title);
            });

            this.data.conditions = _.sortBy(this.data.conditions, "i18nKey");

            return UIUtils.compileTemplate(this.template, this.data).then((tpl) => {
                self.$el.append(tpl);
                self.setElement(`#environment_${itemID}`);

                if (editing) {
                    self.$el.addClass("editing");
                }

                if (itemData) {
                    // Temporary fix, the name attribute is being added by the server after the policy is created.
                    // TODO: Serverside solution required
                    delete itemData.name;

                    // Script name is displayed on UI, but script id is saved along with the condition
                    if (itemData.type === self.SCRIPT_RESOURCE) {
                        hiddenData[itemData.type] = itemData.scriptId;
                        self.$el.data("hiddenData", hiddenData);
                    }

                    self.$el.data("itemData", itemData);
                    self.$el.find("select.type-selection:first").val(itemData.type).trigger("change");
                }

                self.$el.find("select.type-selection:first").focus();

                self.$el.find(".info-button").hide();
            });
        },

        createListItem (allEnvironments, item) {
            var self = this,
                itemToDisplay = null,
                itemData = item.data().itemData,
                hiddenData = item.data().hiddenData,
                mergedData = _.merge({}, itemData, hiddenData),
                type;

            item.focus(); // Required to trigger changeInput.
            this.data.conditions = allEnvironments;

            const propertyI18nKey = (type, key) => (
                `${self.i18n.condition.key}${type}${self.i18n.condition.props}${key}`
            );

            if (mergedData && mergedData.type) {
                type = mergedData.type;
                itemToDisplay = {};
                if (type === self.SCRIPT_RESOURCE) {
                    // eslint-disable-next-line max-len
                    itemToDisplay["console.common.type"] = $.t(`${self.i18n.condition.key}${type}${self.i18n.condition.title}`);
                    const i18nKey = propertyI18nKey(type, "scriptId");
                    if (mergedData.scriptId) {
                        return PoliciesService.getScriptById(mergedData.scriptId).done(function (script) {
                            itemToDisplay[i18nKey] = script.name;
                            return self.setListItemHtml(item, itemToDisplay);
                        });
                    }
                    itemToDisplay[i18nKey] = "";
                    return self.setListItemHtml(item, itemToDisplay);
                }

                _.each(mergedData, function (val, key) {
                    if (key === "type") {
                        // eslint-disable-next-line max-len
                        itemToDisplay["console.common.type"] = $.t(`${self.i18n.condition.key}${type}${self.i18n.condition.title}`);
                    } else {
                        itemToDisplay[propertyI18nKey(type, key)] = val;
                    }
                });
                return this.setListItemHtml(item, itemToDisplay);
            }

            return this.setListItemHtml(item, itemToDisplay);
        },

        setListItemHtml (item, itemToDisplay) {
            var self = this;

            return UIUtils.compileTemplate(
                "templates/admin/views/realms/authorization/policies/conditions/ListItem.html",
                { data: itemToDisplay }
            ).then((tpl) => {
                item.find(".item-data").html(tpl);
                self.setElement(`#${item.attr("id")}`);
            });
        },

        changeType (e) {
            e.stopPropagation();
            var self = this,
                itemData = {},
                hiddenData = {},
                selectedType = e.target.value,
                schema = _.find(this.data.conditions, { title: selectedType }) || {},
                delay = self.$el.find(".field-float-pattern").length > 0 ? 500 : 0,
                helperText;

            if (this.$el.data().itemData && this.$el.data().itemData.type === selectedType) {
                itemData = this.$el.data().itemData;
                hiddenData = this.$el.data().hiddenData;
            } else {
                itemData = self.setDefaultJsonValues(schema);
                self.$el.data("itemData", itemData);
                self.$el.data("hiddenData", hiddenData);
            }

            if (itemData) {
                self.animateOut();
                helperText = this.getHelperText(schema);

                // setTimeout needed to delay transitions.
                setTimeout(function () {
                    self.$el.find(".no-float").remove();
                    self.$el.find(".clear-left").remove();

                    if (_.isEmpty(helperText)) {
                        self.$el.find(".info-button").hide();
                    } else {
                        self.$el.find(".info-button")
                            .show()
                            .attr("data-title", helperText.title.toString())
                            .attr("data-content", helperText.content.toString())
                            .popover();
                    }

                    if (!self.$el.parents("#dropbox").length || self.$el.hasClass("editing")) {
                        self.buildHTML(itemData, hiddenData, schema).done(function () {
                            self.animateIn();
                        });
                    }
                }, delay);
            }
        },

        getHelperText (schema) {
            var helperText = {};
            switch (schema.title) {
                case "IPv4": // fall through
                case "IPv6":
                    helperText.title =
                        Handlebars.helpers.t("console.authorization.policies.edit.conditionTypes.ipHelperTitle");
                    helperText.content =
                        Handlebars.helpers.t("console.authorization.policies.edit.conditionTypes.ipHelperContent");
                    break;
                case "SimpleTime":
                    helperText.title =
                        Handlebars.helpers.t(
                            "console.authorization.policies.edit.conditionTypes.SimpleTime.helperTitle");
                    helperText.content =
                        Handlebars.helpers.t(
                            "console.authorization.policies.edit.conditionTypes.SimpleTime.helperContent");
                    break;
                default:
                    break;
            }
            return helperText;
        },

        buildHTML (itemData, hiddenData, schema) {
            var self = this,
                itemDataEl = this.$el.find(".item-data"),
                schemaProps = schema.config.properties,
                i18nKey,
                attributesWrapper,
                htmlBuiltPromises = [];

            const renderConditionAttr = (callback) => {
                const attrBuiltPromise = $.Deferred();
                callback(attrBuiltPromise.resolve);
                htmlBuiltPromises.push(attrBuiltPromise);
            };

            const buildScriptAttr = (resolve) => {
                new ArrayAttr().render({
                    itemData, hiddenData, data: [hiddenData[itemData.type]],
                    title: "scriptId", dataSource: "scripts", multiple: false,
                    i18nKey: `${self.i18n.condition.key}${schema.title}${self.i18n.condition.props}scriptId`
                }, itemDataEl, resolve);
            };

            if (itemData.type === "SimpleTime") {
                attributesWrapper = '<div class="clearfix clear-left" id="conditionAttrTimeDate"></div>';

                renderConditionAttr((resolve) => new TimeAttr().render({ itemData }, itemDataEl, resolve));
                renderConditionAttr((resolve) => new DayAttr().render({ itemData }, itemDataEl, resolve));
                renderConditionAttr((resolve) => new DateAttr().render({ itemData }, itemDataEl, resolve));

                if (!itemData.enforcementTimeZone) {
                    itemData.enforcementTimeZone = "GMT";
                }
                renderConditionAttr((resolve) => {
                    // eslint-disable-next-line max-len
                    const i18nKey = `${self.i18n.condition.key}${schema.title}${self.i18n.condition.props}enforcementTimeZone`;
                    new ArrayAttr().render({
                        itemData,
                        data: [itemData.enforcementTimeZone],
                        title: "enforcementTimeZone",
                        i18nKey,
                        dataSource: "enforcementTimeZone",
                        multiple: false
                    }, itemDataEl, resolve);
                });
            } else if (schema.title === self.SCRIPT_RESOURCE) {
                attributesWrapper = '<div class="no-float"></div>';
                if (itemData && itemData.scriptId) {
                    renderConditionAttr((resolve) => {
                        PoliciesService.getScriptById(itemData.scriptId).done(function (script) {
                            hiddenData[itemData.type] = script.name;
                            buildScriptAttr(resolve);
                        });
                    });
                } else {
                    renderConditionAttr(buildScriptAttr);
                }
            } else {
                attributesWrapper = '<div class="no-float"></div>';

                _.map(schemaProps, function (value, key) {
                    i18nKey = `${self.i18n.condition.key}${schema.title}${self.i18n.condition.props}${key}`;

                    switch (value.type) {
                        case "string": // fall through
                        case "number": // fall through
                        case "integer":
                            renderConditionAttr((resolve) => {
                                new StringAttr().render({
                                    itemData,
                                    data: itemData[key],
                                    title: key,
                                    i18nKey,
                                    schema,
                                    value
                                }, itemDataEl, resolve);
                            });
                            break;
                        case "boolean":
                            renderConditionAttr((resolve) => {
                                new BooleanAttr().render({
                                    itemData,
                                    data: value,
                                    title: key,
                                    i18nKey,
                                    selected: itemData[key]
                                }, itemDataEl, resolve);
                            });
                            break;
                        case "array":
                            renderConditionAttr((resolve) => {
                                new ArrayAttr().render({
                                    itemData,
                                    data: itemData[key],
                                    title: key,
                                    i18nKey
                                }, itemDataEl, resolve);
                            });
                            break;
                        case "object":
                            renderConditionAttr((resolve) => {
                                new ObjectAttr().render({
                                    itemData,
                                    data: itemData[key],
                                    title: key,
                                    i18nKey
                                }, itemDataEl, resolve);
                            });
                            break;
                        default:
                            break;
                    }
                });
            }

            return $.when.apply($, htmlBuiltPromises).done(() => {
                self.$el.find(".condition-attr").wrapAll(attributesWrapper);
            });
        },

        setDefaultJsonValues (schema) {
            var itemData = { type: schema.title };
            _.map(schema.config.properties, function (value, key) {
                switch (value.type) {
                    case "string":
                        if (key === "authenticateToRealm") {
                            itemData[key] = "/";
                        } else if (key !== "startIp" && key !== "endIp") {
                            // OPENAM-5182: we should not submit empty string if IP is missing
                            itemData[key] = "";
                        }
                        break;
                    case "number": // fall through
                    case "integer":
                        itemData[key] = 0;
                        break;
                    case "boolean":
                        itemData[key] = false;
                        break;
                    case "array":
                        itemData[key] = [];
                        break;
                    case "object":
                        itemData[key] = {};
                        break;
                    default:
                        console.error("Unexpected data type:", key, value);
                        break;
                }
            });

            return itemData;
        },

        animateOut () {
            // hide all items except the title selector
            this.$el.find(".no-float").fadeOut(500);
            this.$el.find(".clear-left").fadeOut(500);
            this.$el.find(".field-float-pattern, .field-float-selectize, .timezone-field")
                .find("label").removeClass("showLabel")
                .next("input").addClass("placeholderText");

            this.$el.find(".field-float-select select:not(.type-selection)").addClass("placeholderText")
                .prev("label").removeClass("showLabel");

            this.$el.removeClass("invalid-rule");
        },

        animateIn () {
            var self = this;
            setTimeout(function () {
                self.$el.find(".field-float-pattern, .field-float-selectize, .timezone-field")
                    .find("label").addClass("showLabel")
                    .next("input, div input").removeClass("placeholderText").prop("readonly", false);

                self.$el.find(".field-float-select select:not(.type-selection)").removeClass("placeholderText")
                    .prop("readonly", false).prev("label").addClass("showLabel");
            }, 10);
        }
    });
});
