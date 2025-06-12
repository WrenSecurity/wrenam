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
 * Copyright 2017-2019 ForgeRock AS.
 */

import { map, uniqueId } from "lodash";
import PropTypes from "prop-types";
import React from "react";

/**
 * This function creates an array of snippets, with the odd array snippets being the ones with matched characters,
 * and the even snippets being the characters inbetween. The matched snippets are then wrapped in the <strong> element.
 * @param {string} children The string to which the emphasized text will be applied.
 * @param {string} match The characters of the string to emphasize.
 * @returns {Array<string|ReactElement>} An array of alternating strings and react elements.
 * @example
 * Given the string "/applications",
 *      a match of "a" will return the snippet array ["/", "a", "pplic", "a", "tions"]
 *      a match of "/AP" will return the snippet array ["", "/ap", "plications"]
 */
function emphasizeMatchingText (children, match) {
    const isOdd = (number) => (number % 2) === 1;
    const snippets = children.split(new RegExp(`(${match})`, "gi"));
    return map(snippets, (snippet, index) => {
        const key = uniqueId(`emphasizedText${snippet}`);
        return isOdd(index) ? <strong key={ key }>{snippet}</strong> : snippet;
    });
}

const EmphasizedText = ({ children, match }) => {
    if (!children) {
        return "";
    }
    if (match) {
        return <span>{ emphasizeMatchingText(children, match) }</span>;
    } else {
        return <span>{ children }</span>;
    }
};

EmphasizedText.propTypes = {
    children: PropTypes.string,
    match: PropTypes.string
};

export default EmphasizedText;
