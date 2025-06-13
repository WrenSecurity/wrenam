/* global module */

module.exports = {
    root: true,
    "extends": [
        "@wrensecurity/eslint-config",
        "@wrensecurity/eslint-config/react"
    ],
    parser: "@babel/eslint-parser",
    parserOptions: {
        ecmaVersion: 6,
        sourceType: "module"
    },
    env: {
        amd: true,
        browser: true,
        es6: true
    },
    settings: {
        react: {
            version: "16.9.0"
        }
    },
    rules: {
        /*
         * --------------------------------------------------------------------------------
         * ERROR RULES
         *
         * These are rules we're sure about. They will cause the build to fail.
         * --------------------------------------------------------------------------------
         */
        "array-bracket-spacing": [2, "never"],
        "arrow-parens": [2, "always"],
        "arrow-spacing": 2,
        "block-spacing": [2, "always"],
        "brace-style": [2, "1tbs", { "allowSingleLine": true }],
        "camelcase": [2, {
            "properties": "always"
        }],
        "comma-spacing": [2, {
            "before": false,
            "after": true
        }],
        "comma-style": 2,
        "constructor-super": 2,
        "dot-location": [2, "property"],
        "eol-last": 2,
        "guard-for-in": 2,
        "indent": [2, 4, {
            "FunctionDeclaration": {
                "parameters": 2
            },
            "FunctionExpression": {
                "parameters": 2
            },
            "SwitchCase": 1,
            "VariableDeclarator": 1
        }],
        "max-len": [2, 120, 4, {
            "ignoreComments": true
        }],
        "new-cap": [2, {
            "capIsNew": false
        }],
        "new-parens": 2,
        "no-alert": 2,
        "no-bitwise": 2,
        "no-catch-shadow": 2,
        "no-confusing-arrow": 2,
        "no-continue": 2,
        "no-empty-character-class": 2,
        "no-empty-pattern": 2,
        "no-extend-native": 2,
        "no-implicit-globals": 2,
        "no-lonely-if": 2,
        "no-mixed-spaces-and-tabs": 2,
        "no-multiple-empty-lines": 2,
        "no-multi-spaces": 2,
        "no-multi-str": 2,
        "no-native-reassign": 2,
        "no-self-assign": 2,
        "no-trailing-spaces": 2,
        "no-unmodified-loop-condition": 2,
        "no-useless-escape": 2,
        "no-void": 2,
        "no-whitespace-before-property": 2,
        "object-curly-spacing": [2, "always"],
        "object-shorthand": 2,
        "operator-linebreak": 2,
        "prefer-const": 2,
        "prefer-template": 2,
        "quotes": [2, "double", "avoid-escape"],
        "semi-spacing": [2, {
            "before": false,
            "after": true
        }],
        "space-before-blocks": [2, "always"],
        "space-before-function-paren": [2, "always"],
        "space-in-parens": [2, "never"],
        "space-infix-ops": [2, {
            "int32Hint": false
        }],
        "space-unary-ops": 2,
        "template-curly-spacing": 2,
        "valid-jsdoc": [2, {
            "prefer": {
                "return": "returns"
            },
            "requireReturn": false
        }],
        "yoda": [2, "never"]

        // TODO: Need an abstraction for logging before we can enable this.
        //"no-console": 0
        //"no-param-reassign": 0
    },
    overrides: [
        {
            // ESM specific rules
            files: ["*.jsm", "*.jsx"],
            rules: {
                "no-var": 2,
                "prefer-arrow-callback": 2,
                "prefer-spread": 2
            }
        },
        {
            // react specific rules
            files: ["*.jsx"],
            rules: {
                "no-class-assign": 0
            }
        }
    ]
};
