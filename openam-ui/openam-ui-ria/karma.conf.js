/* global module, process */

process.env.CHROME_BIN = require("puppeteer").executablePath();

module.exports = function (config) {
    config.set({
        basePath: ".",
        frameworks: ["mocha", "requirejs"],
        files: [
            { pattern: "target/test/test-main.js" },
            { pattern: "target/test/**/*.js", included: false },
            { pattern: "target/www/**/*.js", included: false },
            { pattern: "target/ui-compose/libs/**/*.js", included: false },
            { pattern: "node_modules/chai/chai.js", included: false },
            { pattern: "node_modules/sinon-chai/lib/sinon-chai.js", included: false }
        ],
        exclude: [],
        preprocessors: {
            "target/test/**/*.js": ["babel"]
        },
        babelPreprocessor: {
            options: {
                ignore: ["target/test/libs/"],
                presets: [
                    [
                        "@babel/preset-env",
                        { "targets": "last 2 versions, not dead, > 0.2%" }
                    ]
                ]
            }
        },
        reporters: ["notify", "nyan"],
        mochaReporter: {
            output: "autowatch"
        },
        port: 9876,
        colors: true,
        logLevel: config.LOG_INFO,
        autoWatch: true,
        browsers: [process.env.DISABLE_PUPPETEER_SANDBOX ? "ChromeHeadlessNoSandbox" : "ChromeHeadless"],
        singleRun: false,
        customLaunchers: {
            ChromeHeadlessNoSandbox: {
                base: "ChromeHeadless",
                flags: ["--no-sandbox"]
            }
        }
    });
};
