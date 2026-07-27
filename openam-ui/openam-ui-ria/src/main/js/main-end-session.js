/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.1.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.1.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2026 Wren Security
 */

require.config({
    map: {
        "*": {
            "ThemeManager": "org/forgerock/openam/ui/common/util/ThemeManager",
            "Router": "org/forgerock/openam/ui/common/SingleRouteRouter"
        }
    },
    paths: {
        "handlebars": "libs/handlebars",
        "i18next": "libs/i18next",
        "jquery": "libs/jquery",
        "lodash": "libs/lodash",
        "redux": "libs/redux",
        "text": "libs/text"
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
    "lodash",
    "handlebars",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/i18nManager",
    "ThemeManager",
    "Router"
], function ($, _, Handlebars, Configuration, Constants, i18nManager, ThemeManager, Router) {
    let templatePaths = [
        "templates/user/EndSessionConfirmationTemplate.html",
        "templates/user/EndSessionCanceledTemplate.html",
        "templates/user/EndSessionDoneTemplate.html",
        "templates/user/EndSessionErrorTemplate.html",
        "templates/common/LoginBaseTemplate.html",
        "templates/common/FooterTemplate.html",
        "templates/common/LoginHeaderTemplate.html"
    ];
    const data = window.pageData || {};
    const i18nReady = i18nManager.init({
        paramLang: {
            locale: data.locale || Constants.DEFAULT_LANGUAGE
        },
        defaultLang: Constants.DEFAULT_LANGUAGE,
        nameSpace: "endSession"
    });

    Configuration.globalData = {
        realm: data.realm
    };
    Router.currentRoute = {
        navGroup: "user"
    };

    ThemeManager.getTheme().always(function (theme) {
        // add prefix to templates for custom theme when path is defined
        const themePath = Configuration.globalData.theme.path;
        templatePaths = _.map(templatePaths, function (templatePath) {
            return `text!${themePath}${templatePath}`;
        });
        require(templatePaths, function (EndSessionConfirmationTemplate, EndSessionCanceledTemplate,
                EndSessionDoneTemplate, EndSessionErrorTemplate, LoginBaseTemplate, FooterTemplate,
                LoginHeaderTemplate) {
            data.theme = theme;
            i18nReady.then(function () {
                let contentTemplate;
                switch (data.stage) {
                    case "canceled":
                        contentTemplate = EndSessionCanceledTemplate;
                        break;
                    case "done":
                        contentTemplate = EndSessionDoneTemplate;
                        break;
                    case "error":
                        contentTemplate = EndSessionErrorTemplate;
                        break;
                    default:
                        contentTemplate = EndSessionConfirmationTemplate;
                        break;
                }
                const render = (template) => Handlebars.compile(template)(data);
                $("#wrapper").html(render(LoginBaseTemplate));
                $("#loginBaseLogo").html(render(LoginHeaderTemplate));
                $("#content").html(render(contentTemplate));
                $("#footer").html(render(FooterTemplate));
            });
        });
    });
});
