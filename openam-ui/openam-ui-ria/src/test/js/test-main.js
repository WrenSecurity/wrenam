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

(function () {
    var TEST_REGEXP = /(spec|test)\.js$/i,
        allTestFiles = Object.keys(window.__karma__.files).filter(function (file) {
            return TEST_REGEXP.test(file);
        });

    require.config({
        baseUrl: "/base/target/www",

        map: {
            "*": {
                // TODO: Remove this when there are no longer any references to the "underscore" dependency
                "underscore": "lodash"
            }
        },
        paths: {
            "backbone": "/base/target/ui-compose/libs/backbone",
            "handlebars": "/base/target/ui-compose/libs/handlebars",
            "jquery": "/base/target/ui-compose/libs/jquery",
            "lodash": "/base/target/ui-compose/libs/lodash",
            "moment": "/base/target/ui-compose/libs/moment",
            "i18next": "/base/target/ui-compose/libs/i18next",
            "redux": "/base/target/www/libs/redux",
            "chai": "/base/node_modules/chai/chai",
            "sinon-chai": "/base/node_modules/sinon-chai/lib/sinon-chai",
            "sinon": "/base/target/test/libs/sinon",
            "squire": "/base/target/test/libs/squire"
        },
        shim: {
            "lodash": {
                exports: "_"
            }
        }
    });

    require(["chai", "sinon-chai"].concat(allTestFiles), function (chai, chaiSinon) {
        chai.use(chaiSinon);

        window.expect = chai.expect;
        window.__karma__.start();
    });
}());
