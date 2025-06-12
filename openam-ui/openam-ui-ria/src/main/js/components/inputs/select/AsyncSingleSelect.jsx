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
 * Copyright 2019 ForgeRock AS.
 * Portions copyright 2025 Wren Security.
 */

import { isEmpty } from "lodash";
import { t } from "i18next";
import { Async } from "react-select";
import PropTypes from "prop-types";
import React from "react";

import DropdownIndicator from "./components/DropdownIndicator";
import Option from "./components/options/DefaultOption";
import styles from "./components/styles";

const noOptionsMessage = ({ inputValue }) => {
    if (isEmpty(inputValue)) {
        return t("common.form.searchPrompt");
    }
};
const hideLoadingMessage = () => null;

const AsyncSingleSelect = (props) => (
    <Async
        components={ { DropdownIndicator, Option } }
        isClearable
        isLoading
        loadingMessage={ hideLoadingMessage }
        noOptionsMessage={ noOptionsMessage }
        styles={ styles }
        { ...props }
    />
);

AsyncSingleSelect.propTypes = {
    inputId: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired
};

export default AsyncSingleSelect;
