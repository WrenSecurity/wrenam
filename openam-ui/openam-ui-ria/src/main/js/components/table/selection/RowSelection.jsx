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
 * Copyright 2017-2022 ForgeRock AS.
 */

import PropTypes from "prop-types";
import React, { Component } from "react";

import HeaderSelection from "./HeaderSelection";

/**
* Table row selection component.
* @see https://github.com/AllenFang/react-bootstrap-table/blob/master/examples/js/selection/custom-multi-select-table.js
* @returns {ReactElement} react row selection component
*/
class RowSelection extends Component {
    handleSelect = () => {};
    render () {
        /* eslint-disable jsx-a11y/label-has-associated-control */
        if (this.props.rowIndex === "Header") {
            return (
                <div className="checkbox">
                    <HeaderSelection { ...this.props } />
                    <label htmlFor={ `checkbox${this.props.rowIndex}` } />
                </div>
            );
        } else {
            return (
                <div className="checkbox">
                    <input
                        checked={ this.props.checked }
                        disabled={ this.props.disabled }
                        id={ `checkbox${this.props.rowIndex}` }
                        name={ `checkbox${this.props.rowIndex}` }
                        onChange={ this.handleSelect }
                        type="checkbox"
                    />
                    <label htmlFor={ `checkbox${this.props.rowIndex}` } />
                </div>
            );
        }
        /* eslint-enable jsx-a11y/label-has-associated-control */
    }
}

RowSelection.propTypes = {
    checked: PropTypes.oneOf([true, false, "indeterminate"]).isRequired,
    disabled: PropTypes.bool.isRequired,
    rowIndex: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number
    ])
};

export default RowSelection;
