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

import $ from "jquery";
import { EditorView, basicSetup } from "codemirror";
import { javascript } from "@codemirror/lang-javascript";
import { StreamLanguage } from "@codemirror/language";
import { groovy } from "@codemirror/legacy-modes/mode/groovy";
import { Compartment } from "@codemirror/state";

/**
 * @typedef {"groovy" | "javascript"} SupportedLanguage
 */

/**
 * @typedef {Object} CodeMirrorOptions
 * @property {SupportedLanguage} mode - script language
 * @property {string} value - initial script content
 * @property {Function[]} updateCallbacks - array of callbacks to be called on editor update
 */

const resolveLanguage = (lang) => {
    if (lang === "groovy") {
        return StreamLanguage.define(groovy);
    }
    return javascript();
};

/* eslint-disable valid-jsdoc */
/**
 * CodeMirror script editor utility class.
 *
 * @param {Element} parent Element to attach the editor to.
 * @param {CodeMirrorOptions} [options]
 */
/* eslint-enable valid-jsdoc */
export default function (parent, options) {
    const theme = EditorView.theme({
        "&": { border: "1px solid #dbdbdb" }
    });

    const languageConf = new Compartment();

    const extensions = [
        basicSetup,
        theme,
        languageConf.of(resolveLanguage(options.mode))
    ];

    if (options.updateCallbacks?.length > 0) {
        extensions.push(...options.updateCallbacks.map((cb) => EditorView.updateListener.of(cb)));
    }

    // Create editor view
    const editor = new EditorView({
        parent,
        doc: options.value ?? "",
        extensions
    });

    return {
        /**
         * Editor instance.
         */
        editor,
        /**
         * @returns {string} script content string
         */
        getValue: () => editor.state.doc.toString(),
        /**
         * Set script content to provided string value.
         * @param {string} content script content
         */
        setValue: (content) => {
            editor.dispatch({
                changes: { from: 0, to: editor.state.doc.toString().length, insert: content }
            });
        },
        /**
         * Set script language.
         * @param {SupportedLanguage} lang target script language
         */
        setLanguage: (lang) => {
            editor.dispatch({
                effects: languageConf.reconfigure(resolveLanguage(lang))
            });
        },
        /**
         * Toggle editor's full-screen mode.
         * @param {boolean} fullScreen toggle full-screen on/off
         * @param {Element} fullScreenBarEl full-screen bar element
         */
        toggleFullScreen: (fullScreen, fullScreenBarEl) => {
            if (fullScreen) {
                $(parent).addClass("code-mirror-full-screen");
                $("body").css("overflow-y", "hidden");
            } else {
                $(parent).removeClass("code-mirror-full-screen");
                $("body").css("overflow-y", "scroll");
            }
            $(fullScreenBarEl).toggle(fullScreen);
        }
    };
}
