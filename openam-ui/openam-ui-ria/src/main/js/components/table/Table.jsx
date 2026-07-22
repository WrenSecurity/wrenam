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
 * Copyright 2017-2024 ForgeRock AS.
 */

import BootstrapTable from "react-bootstrap-table-next";
import paginationFactory from "react-bootstrap-table2-paginator";
import { find, map, without } from "lodash";
import PropTypes from "prop-types";
import React from "react";

import RowSelection from "components/table/selection/RowSelection";

import {
    PLACEHOLDER_TYPES,
    isPlaceholder
} from "org/forgerock/commons/ui/common/util/PlaceholderUtils";

const Table = ({ keyField, onRowClick, onSelectedChange, options = {}, selectedItems, tableRef, ...restProps }) => {
    const handleSelect = (row, isSelected) => {
        const selected = isSelected
            ? [...selectedItems, row]
            : without(selectedItems, find(selectedItems, { [keyField]: row[keyField] }));

        onSelectedChange(selected);
    };
    const handleSelectAll = (isSelected, rows) => onSelectedChange(isSelected ? rows : []);
    const fetchInfo = options.dataTotalSize ? { dataTotalSize: options.dataTotalSize } : undefined;

    // Get the pagination configuration from the options prop
    const pagination = {
        page: options.page,
        sizePerPage: options.sizePerPage,
        totalSize: options.dataTotalSize,
        hidePageListOnlyOnePage: true,
        onSizePerPageChange: options.onSizePerPageList,
        onPageChange: options.onPageChange
    };

    // BootstrapTable requires an onTableChange prop to be passed,
    // so if it does not exist in options, pass an empty function
    const onTableChange = options.onTableChange || function () {};

    for (let i = 0; i < restProps.data.length; i++) {
        const current = restProps.data[i];
        const keys = Object.keys(current);
        for (let j = 0; j < keys.length; j++) {
            const current = restProps.data[i][keys[j]];
            PLACEHOLDER_TYPES.forEach((type) => {
                if (current && current[type]) {
                    try {
                        restProps.data[i][keys[j]] = current[type];
                    } catch (error) {
                        // just ignore
                    }
                }
            });
            if ((!keys[j].startsWith("_")) && (restProps.data[i][keys[j]])) {
                try {
                    if (isPlaceholder(restProps.data[i][keys[j]])) {
                        restProps.data[i][keys[j]] = [restProps.data[i][keys[j]]];
                    }
                } catch (error) {
                    // just ignore
                }
            }
        }
    }

    return (
        <BootstrapTable
            columns={ restProps.columns }
            fetchInfo={ fetchInfo }
            keyField={ keyField }
            onTableChange={ onTableChange }
            pagination={ options.pagination ? paginationFactory(pagination) : undefined }
            ref={ tableRef }
            remote={ options.remote }
            rowEvents={ {
                onClick: onRowClick
            } }
            selectRow={ {
                bgColor: "#f7f7f7",
                className: "active",
                columnWidth: "50px",
                selectionRenderer: ({ checked, disabled, rowIndex }) => ( // eslint-disable-line
                    <RowSelection
                        checked={ checked }
                        disabled={ disabled }
                        rowIndex={ rowIndex }
                    />
                ),
                selectionHeaderRenderer: ({ checked, indeterminate, disabled = false, rowIndex = "Header" }) => ( // eslint-disable-line
                    <RowSelection
                        checked={ indeterminate ? "indeterminate" : checked }
                        disabled={ disabled }
                        rowIndex={ rowIndex }
                    />
                ),
                mode: "checkbox",
                onSelect: handleSelect,
                onSelectAll: handleSelectAll,
                selected: map(selectedItems, keyField)
            } }
            { ...restProps }
        />
    );
};

Table.defaultProps = {
    bordered: false,
    condensed: false,
    hover: true
};

Table.propTypes = {
    bordered: PropTypes.bool,
    condensed: PropTypes.bool,
    hover: PropTypes.bool,
    keyField: PropTypes.string.isRequired,
    onRowClick: PropTypes.func,
    onSelectedChange: PropTypes.func.isRequired,
    options: PropTypes.objectOf(PropTypes.any),
    selectedItems: PropTypes.arrayOf(PropTypes.any).isRequired,
    tableRef: PropTypes.func
};

export default Table;
