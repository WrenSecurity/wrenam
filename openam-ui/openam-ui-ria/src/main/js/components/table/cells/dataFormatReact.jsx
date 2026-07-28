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
 * Copyright 2019-2022 ForgeRock AS.
 */

import React from "react";

/**
 * Function that allows a React component to be used a in table's dataFormat attribute
 * Using lodash-identity will return the original unformatted string to the columnTitle if it is required.
 * @see https://allenfang.github.io/react-bootstrap-table/docs.html#dataFormat
 * @param {ReactElement} element React Element to clone.
 * @returns {ReactElement} React Element with the TableHeaderColumn properties passed in as props.
 * @example
 * import { identity } from "lodash";
 * import dataFormatReact from "components/table/cells/dataFormatReact";
 *
 * <Table>
 *     <TableHeaderColumn
 *         columnTitle={ identity }
 *         dataFormat={ dataFormatReact(
 *             <PopoverCell>
 *                 <FontAwesomeIconCell icon="top-notch" />
 *             </PopoverCell>
 *            ) }
 *      ...
 *      </TableHeaderColumn>
 *  </Table>
 */
const dataFormatReact = (element) =>
    (cell, row, rowIdx, formatExtraData) =>
        React.cloneElement(element, { cell, row, formatExtraData, rowIdx });

export default dataFormatReact;
