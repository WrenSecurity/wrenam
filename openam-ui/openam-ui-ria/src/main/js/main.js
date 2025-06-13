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
 * Portions copyright 2011-2016 ForgeRock AS.
 * Portions copyright 2024-2025 Wren Security.
 */

require.config({
    map: {
        "*" : {
            "Footer"            : "org/forgerock/openam/ui/common/components/Footer",
            "ThemeManager"      : "org/forgerock/openam/ui/common/util/ThemeManager",
            "LoginView"         : "org/forgerock/openam/ui/user/login/RESTLoginView",
            "UserProfileView"   : "org/forgerock/commons/ui/user/profile/UserProfileView",
            "ForgotUsernameView": "org/forgerock/openam/ui/user/anonymousProcess/ForgotUsernameView",
            "PasswordResetView" : "org/forgerock/openam/ui/user/anonymousProcess/PasswordResetView",
            "LoginDialog"       : "org/forgerock/openam/ui/user/login/RESTLoginDialog",
            "NavigationFilter"  : "org/forgerock/openam/ui/common/components/navigation/filters/RouteNavGroupFilter",
            "Router"            : "org/forgerock/commons/ui/common/main/Router",
            "RegisterView"      : "org/forgerock/openam/ui/user/anonymousProcess/SelfRegistrationView",
            "KBADelegate"       : "org/forgerock/openam/ui/user/services/KBADelegate"
        }
    },
    paths: {
        "autosizeInput": "libs/jquery.autosize.input",

        "backbone"           : "libs/backbone",
        "backbone.paginator" : "libs/backbone.paginator",
        "backbone-relational": "libs/backbone-relational",

        "backgrid"          : "libs/backgrid",
        "backgrid-filter"   : "libs/backgrid-filter",
        "backgrid.paginator": "libs/backgrid-paginator",
        "backgrid-selectall": "libs/backgrid-select-all",

        "bootstrap"               : "libs/bootstrap",
        "bootstrap-datetimepicker": "libs/bootstrap-datetimepicker",
        "bootstrap-dialog"        : "libs/bootstrap-dialog",
        "bootstrap-tabdrop"       : "libs/bootstrap-tabdrop",

        "classnames"       : "libs/classnames",
        "clockPicker"      : "libs/bootstrap-clockpicker",
        "create-react-class": "libs/create-react-class",
        "doTimeout"        : "libs/jquery.ba-dotimeout",
        "emotion"          : "libs/emotion",
        "form2js"          : "libs/form2js",
        "handlebars"       : "libs/handlebars",
        "i18next"          : "libs/i18next",
        "jquery"           : "libs/jquery",
        "js2form"          : "libs/js2form",
        "jsonEditor"       : "libs/jsoneditor",
        "lodash"           : "libs/lodash",
        "microplugin"      : "libs/microplugin",
        "moment"           : "libs/moment",
        "popoverclickaway" : "libs/popover-clickaway",
        "prop-types"       : "libs/prop-types",
        "qrcode"           : "libs/qrcode",
        "react-bootstrap"  : "libs/react-bootstrap",
        "react-dom"        : "libs/react-dom",
        "react"            : "libs/react",
        "react-input-autosize": "libs/react-input-autosize",
        "react-redux"      : "libs/react-redux",
        "react-select"     : "libs/react-select",
        "redux"            : "libs/redux",
        "redux-actions"    : "libs/redux-actions",
        "selectize"        : "libs/selectize-non-standalone",
        "sifter"           : "libs/sifter",
        "sortable"         : "libs/jquery-nestingSortable",
        "spin"             : "libs/spin",
        "text"             : "libs/text",
        "underscore"       : "libs/underscore",
        "xdate"            : "libs/xdate"
    },
    shim: {
        "autosizeInput": {
            deps: ["jquery"],
            exports: "autosizeInput"
        },
        "backbone": {
            deps: ["underscore"],
            exports: "Backbone"
        },
        "backbone.paginator": {
            deps: ["backbone"]
        },
        "backbone-relational": {
            deps: ["backbone"]
        },

        "backgrid": {
            deps: ["jquery", "underscore", "backbone"],
            exports: "Backgrid"
        },
        "backgrid-filter": {
            deps: ["backgrid"]
        },
        "backgrid.paginator": {
            deps: ["backgrid", "backbone.paginator"]
        },
        "backgrid-selectall": {
            deps: ["backgrid"]
        },

        "bootstrap": {
            deps: ["jquery"]
        },
        "bootstrap-dialog": {
            deps: ["jquery", "underscore", "backbone", "bootstrap"]
        },
        "bootstrap-tabdrop": {
            deps: ["jquery", "bootstrap"]
        },

        "clockPicker": {
            deps: ["jquery"],
            exports: "clockPicker"
        },
        "doTimeout": {
            deps: ["jquery"],
            exports: "doTimeout"
        },
        "form2js": {
            exports: "form2js"
        },
        "js2form": {
            exports: "js2form"
        },
        "jsonEditor": {
            exports: "JSONEditor"
        },
        "moment": {
            exports: "moment"
        },
        "qrcode": {
            exports: "qrcode"
        },
        "selectize": {
            /**
             * sifter, microplugin is additional dependencies for fix release build.
             * @see https://github.com/brianreavis/selectize.js/issues/417
             */
            deps: ["jquery", "sifter", "microplugin"]
        },
        "spin": {
            exports: "spin"
        },
        "lodash": {
            exports: "_"
        },
        "xdate": {
            exports: "xdate"
        },
        "sortable": {
            deps: ["jquery"]
        }
    }
});

require([
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",

    // other modules that are necessary to include to startup the app
    "jquery",
    "lodash",
    "backbone",
    "handlebars",
    "i18next",
    "spin",
    "org/forgerock/commons/ui/common/main",
    "org/forgerock/openam/ui/main",
    "config/main",
    "store/index"
], (Constants, EventManager) => {
    EventManager.sendEvent(Constants.EVENT_DEPENDENCIES_LOADED);
});
