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
 * Copyright 2024 Wren Security.
 */

/* global __dirname, __filename, process */

const {
    useBuildScripts,
    useEslint,
    useLocalResources,
    useModuleResources,
    useBuildModule,
    useLessStyles,
    useBuildRequire
} = require("@wrensecurity/commons-ui-build");
const gulp = require("gulp");
const { join } = require("path");
const KarmaServer = require("karma").Server;
const replace = require("gulp-replace");
const argv = require("yargs").argv;

const babelConfig = require("./.babelrc.json");

const TARGET_PATH = "target/www";
const TESTS_PATH = "target/test";

const MODULE_RESOURCES = {
    "requirejs-text/text.js": "libs/text.js",
    "selectize/dist/js/selectize.min.js": "libs/selectize-non-standalone.js",
    "microplugin/src/microplugin.js": "libs/microplugin.js",
    "json-editor/dist/jsoneditor.min.js": "libs/jsoneditor.js",
    "redux/dist/redux.min.js": "libs/redux.js",
    "react-bootstrap/dist/react-bootstrap.min.js": "libs/react-bootstrap.js",
    "classnames/index.js": "libs/classnames.js",
    "react-select/dist/react-select.min.js": "libs/react-select.js",
    "react-select/dist/react-select.min.css": "css/react-select.css",
    "backbone.paginator/lib/backbone.paginator.min.js": "libs/backbone.paginator.js",
    "handlebars/dist/handlebars.amd.min.js": "libs/handlebars.js"
};

const LOCAL_RESOURCES = {
    "css/*": "css",
    "js/*": "libs"
};

const DEPLOY_DIR = `${process.env.OPENAM_HOME}/XUI`;

gulp.task("eslint", useEslint(
    {
        src: [
            "src/main/js/**/*.{js,jsm,jsx}",
            "!src/main/js/libs/**/*.js",
            "src/test/js/**/*.js",
            "!src/test/js/libs/**/*.js"
        ]
    }
));

gulp.task("build:styles", useLessStyles({
    "target/www/css/structure.less": "css/structure.css",
    "target/www/css/theme.less": "css/theme.css",
    "target/www/css/styles-admin.less": "css/styles-admin.css"
}, { base: join(TARGET_PATH, "css"), dest: TARGET_PATH }));

gulp.task("build:assets", useLocalResources({ "src/main/resources/**": "" }, { dest: TARGET_PATH }));

gulp.task("build:compose", useLocalResources({ "target/ui-compose/**": "" }, { dest: TARGET_PATH }));

gulp.task("build:libs", async () => {
    await useModuleResources(MODULE_RESOURCES, { path: __filename, dest: TARGET_PATH })();
    // TODO Why are libs in two different folders "libs" and "src/main/js/libs"?
    await useLocalResources(LOCAL_RESOURCES, { base: "libs", dest: TARGET_PATH })();
    await useLocalResources({ "src/main/js/libs/*": "libs" }, { dest: TARGET_PATH })();
});

gulp.task("build:scripts", useBuildScripts({
    src: ["src/main/js/**/*.js", "!src/main/js/libs/**/*.js"],
    dest: TARGET_PATH,
    presets: babelConfig.presets,
    plugins: [
        ["@babel/plugin-transform-classes", { "loose": true }]
    ]
}));

gulp.task("build:scriptsJSM", useBuildScripts(
    {
        src: ["src/main/js/**/*.{jsm,jsx}", "!src/main/js/org/forgerock/openam/ui/admin/utils/CodeMirror.jsm"],
        dest: TARGET_PATH,
        presets: babelConfig.presets,
        plugins: [
            ["@babel/plugin-transform-classes", { "loose": true }],
            "@babel/plugin-transform-modules-amd"
        ]
    }
));

gulp.task("build:bundle", useBuildRequire({
    base: TARGET_PATH,
    src: "src/main/js/main.js",
    dest: join(TARGET_PATH, "main.js"),
    exclude: [
        // Excluded from optimization so that the UI can be customized without having to repackage it.
        "config/AppConfiguration",
        "config/ThemeConfiguration"
    ]
}));

gulp.task("build:editor", useBuildModule({
    id: "org/forgerock/openam/ui/admin/utils/CodeMirror",
    src: "src/main/js/org/forgerock/openam/ui/admin/utils/CodeMirror.jsm",
    dest: join(TARGET_PATH, "org/forgerock/openam/ui/admin/utils/CodeMirror.js"),
    external: ["jquery"]
}));

/**
 * Include the version of AM in the index file.
 *
 * This is needed to force the browser to refetch JavaScript files when a new version of AM is deployed.
 */
gulp.task("build:version", () => (
    gulp.src(`${TARGET_PATH}/index.html`)
        .pipe(replace("${version}", argv["target-version"] || "dev"))
        .pipe(gulp.dest(TARGET_PATH))
));

gulp.task("test:scripts", useLocalResources({ "src/test/js/**": "" }, { dest: TESTS_PATH }));

gulp.task("test:libs", useModuleResources({ "sinon/pkg/sinon.js": "libs/sinon.js" }, { dest: TESTS_PATH }));

gulp.task("test:karma", () => (
    new KarmaServer({
        configFile: `${__dirname}/karma.conf.js`,
        singleRun: true,
        reporters: ["progress"]
    }).start()
));

gulp.task("build", gulp.series(
    gulp.parallel(
        "build:assets",
        "build:scripts",
        "build:scriptsJSM",
        "build:compose",
        "build:editor",
        "build:libs"
    ),
    gulp.parallel(
        "build:styles",
        "build:version"
    )
));

gulp.task("test", gulp.series(
    "test:scripts",
    "test:libs",
    "test:karma"
));

gulp.task("deploy", () => gulp.src(`${TARGET_PATH}/**/*`).pipe(gulp.dest(DEPLOY_DIR)));

gulp.task("watch", () => {
    gulp.watch(
        "src/main/resources/**",
        gulp.series("build:assets", gulp.parallel("build:styles", "build:version"), "deploy")
    );
    gulp.watch(
        ["src/main/js/**/*.js", "!src/main/js/libs/**/*.js"],
        gulp.series("build:scripts", "deploy")
    );
    gulp.watch(
        ["src/main/js/**/*.{jsm,jsx}", "!src/main/js/org/forgerock/openam/ui/admin/utils/CodeMirror.jsm"],
        gulp.series("build:scriptsJSM", "deploy")
    );
    gulp.watch(
        "src/main/js/org/forgerock/openam/ui/admin/utils/CodeMirror.jsm",
        gulp.series("build:editor", "deploy")
    );
});

gulp.task("dev", gulp.series("build", "deploy", "watch"));
gulp.task("prod", gulp.series("eslint", "build", "build:bundle", "test"));

gulp.task("default", gulp.series("dev"));
