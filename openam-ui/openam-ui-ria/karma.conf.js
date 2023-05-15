process.env.CHROME_BIN = require("puppeteer").executablePath();

module.exports = function (config) {
    config.set({
        basePath: ".",
        frameworks: ["mocha", "requirejs"],
        files: [
            { pattern: "target/test-classes/test-main.js" },
            { pattern: "target/test-classes/**/*.js", included: false },
            { pattern: "target/compiled/**/*.js", included: false },
            { pattern: "target/dependencies/libs/**/*.js", included: false },
            { pattern: "target/dependencies-expanded/forgerock-ui-user/libs/**/*.js", included: false },
            { pattern: "node_modules/chai/chai.js", included: false },
            { pattern: "node_modules/sinon-chai/lib/sinon-chai.js", included: false }
        ],
        exclude: [],
        preprocessors: {
            "target/test-classes/**/*.js": ["babel"]
        },
        babelPreprocessor: {
            options: {
                ignore: ["libs/"],
                presets: ["es2015"]
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
