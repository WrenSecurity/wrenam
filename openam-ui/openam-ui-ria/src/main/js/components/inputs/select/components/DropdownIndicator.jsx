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
 */

import { components } from "react-select";
import classnames from "classnames";
import PropTypes from "prop-types";
import React from "react";

/*
 * This custom DropdownIndicator component wraps the default react-select v2 componet with one that looks and behaves
 * in the same manner as our other selectors. The react-select v2 DropdownIndicator is styled to look like bootstrap 4,
 * and so looks out of place within our design. For more information @see https://react-select.com/components
 */
const DropdownIndicator = (props) => (
    <components.DropdownIndicator { ...props }>
        <i
            className={
                classnames({
                    "fa": true,
                    "fa-caret-down": !props.selectProps.menuIsOpen,
                    "fa-caret-up": props.selectProps.menuIsOpen
                })
            }
        />
    </components.DropdownIndicator>
);

DropdownIndicator.propTypes = {
    selectProps: PropTypes.shape({
        menuIsOpen: PropTypes.bool.isRequired
    }).isRequired
};

export default DropdownIndicator;
