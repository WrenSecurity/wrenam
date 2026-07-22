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

import { isEmpty, isEqual, map, omit } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

import ListToolbar from "./ListToolbar";
import ListCallToAction from "./ListCallToAction";
import Loading from "components/Loading";
import Table from "components/table/Table";

class List extends Component {
    constructor () {
        super();

        this.state = { selectedItems: [] };
    }

    UNSAFE_componentWillReceiveProps (nextProps) {
        const listHasChanged = !isEqual(
            map(this.props.items, this.props.keyField).sort(),
            map(nextProps.items, this.props.keyField).sort()
        );

        if (listHasChanged) {
            // deselect everything in case of pagination, items deletion, etc.
            this.setState({ selectedItems: [] });
        }
    }

    handleDelete = () => {
        this.props.onDelete(this.state.selectedItems);
    };

    handleSelectedChange = (items) => {
        this.setState({ selectedItems: items });
    };

    render () {
        let content;
        const parentProps = omit(this.props, "children");

        if (this.props.isFetching) {
            content = <Loading />;
        } else if (isEmpty(this.props.items)) {
            content = (
                <ListCallToAction { ...parentProps } />
            );
        } else {
            const tableProperties = {
                ...parentProps,
                data: this.props.items,
                onSelectedChange: this.handleSelectedChange,
                selectedItems: this.state.selectedItems
            };
            const toolbar = (
                <ListToolbar
                    { ...parentProps }
                    isDeleteDisabled={ !this.state.selectedItems.length }
                    numberSelected={ this.state.selectedItems.length }
                    onDelete={ this.handleDelete }
                />
            );
            content = (
                <div>
                    { toolbar }
                    <Table { ...tableProperties } />
                </div>
            );
        }

        return content;
    }
}

List.defaultProps = {
    keyField: "_id"
};

List.propTypes = {
    columns: PropTypes.arrayOf(PropTypes.object).isRequired,
    isFetching: PropTypes.bool.isRequired,
    items: PropTypes.arrayOf(PropTypes.object).isRequired,
    keyField: PropTypes.string,
    onDelete: PropTypes.func.isRequired
};

export default List;
