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
 * Copyright 2015-2016 ForgeRock AS.
 */

require.config({
    map: {
        "*" : {
            "ThemeManager" : "org/forgerock/openam/ui/common/util/ThemeManager",
            "Router": "org/forgerock/openam/ui/common/SingleRouteRouter"
        }
    },
    paths: {
        "handlebars": "libs/handlebars",
        "i18next": "libs/i18next",
        "jquery": "libs/jquery",
        "lodash": "libs/lodash",
        "redux": "libs/redux",
        "text": "libs/text",
        "underscore": "libs/underscore"
    },
    shim: {
        "handlebars": {
            exports: "handlebars"
        },
        "lodash": {
            exports: "_"
        }
    }
});

require([
    "jquery",
    "handlebars",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/common/util/Constants",
    "text!templates/user/DeviceTemplate.html",
    "text!templates/user/DeviceDoneTemplate.html",
    "text!templates/common/LoginBaseTemplate.html",
    "text!templates/common/FooterTemplate.html",
    "text!templates/common/LoginHeaderTemplate.html",
    "org/forgerock/commons/ui/common/main/i18nManager",
    "ThemeManager",
    "Router"
], function ($, HandleBars, Configuration, Constants, DeviceTemplate, DeviceDoneTemplate,
        LoginBaseTemplate, FooterTemplate, LoginHeaderTemplate, i18nManager, ThemeManager, Router) {
    var data = window.pageData,
        template = data.done ? DeviceDoneTemplate : DeviceTemplate;

    i18nManager.init({
        paramLang: {
            locale: data.locale
        },
        defaultLang: Constants.DEFAULT_LANGUAGE,
        nameSpace: "device"
    });

    Configuration.globalData = { realm : data.realm };
    Router.currentRoute = {
        navGroup: "user"
    };

    ThemeManager.getTheme().always(function (theme) {
        data.theme = theme;

        $("#wrapper").html(HandleBars.compile(LoginBaseTemplate)(data));
        $("#footer").html(HandleBars.compile(FooterTemplate)(data));
        $("#loginBaseLogo").html(HandleBars.compile(LoginHeaderTemplate)(data));
        $("#content").html(HandleBars.compile(template)(data));
    });
});
