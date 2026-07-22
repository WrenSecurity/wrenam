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

import classnames from "classnames";
import PropTypes from "prop-types";
import React from "react";

const FontAwesomeIconCell = ({ icon, isEditable, cell, children, row, formatExtraData, rowIdx }) => {
    const applyDataFormat = (format) => {
        return format ? React.cloneElement(format, { cell, row, formatExtraData, rowIdx }) : cell;
    };

    return (
        <span
            className={ classnames({
                "am-table-icon-cell": true,
                "am-table-icon-cell-editable": isEditable
            }) }
        >
            <span className="fa-stack fa-lg am-table-icon-cell-stack">
                <i className="fa fa-circle fa-stack-2x text-primary" />
                <i className={ `fa fa-${icon} fa-stack-1x fa-inverse` } />
            </span>
            { " " }
            <span>{ applyDataFormat(children) }</span>
        </span>
    );
};

FontAwesomeIconCell.defaultProps = {
    isEditable: false
};

FontAwesomeIconCell.propTypes = {
    cell: PropTypes.oneOfType([
        PropTypes.array,
        PropTypes.object,
        PropTypes.string
    ]),
    children: PropTypes.node,
    formatExtraData: PropTypes.objectOf(
        PropTypes.any
    ),
    icon: PropTypes.string.isRequired,
    isEditable: PropTypes.bool,
    row: PropTypes.objectOf(
        PropTypes.any
    ),
    rowIdx: PropTypes.number
};

export default FontAwesomeIconCell;
