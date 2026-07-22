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
 * Copyright 2018-2024 ForgeRock AS.
 */

import React, { Component } from "react";
import { PropTypes } from "prop-types";

const withPagination = (WrappedComponent) => {
    return class withPagination extends Component {
        constructor (props) {
            super(props);
            this.state = {
                dataTotalSize: 0,
                page: 1,
                pagedResultsOffset: 0,
                searchTerm: "",
                sizePerPage: 10,
                sortDirection: "+",
                sortKey: ""
            };
        }

        handleSizePerPageChange = (sizePerPage) => {
            this.setState({
                page: 1,
                pagedResultsOffset: 0,
                sizePerPage
            });
        };

        handlePageChange = (page, sizePerPage) => {
            this.setState({
                page,
                pagedResultsOffset: (page - 1) * sizePerPage
            });
        };

        handleSortChange = ({ sortField, sortOrder }) => {
            this.setState({
                sortKey: sortField,
                sortDirection: sortOrder === "asc" ? "+" : "-"
            });
        };

        handleSearchChange = (searchTerm) => {
            this.setState({
                searchTerm,
                page: 1,
                pagedResultsOffset: 0
            });
        };

        handleDataChange = ({ remainingPagedResults, result, totalPagedResults }) => {
            // CREST 2.0 based resources use the convention of returning the remaining count of resources to the caller
            // {remainingPagedResults}. Whereas CREST 3.0 resources use the total resource count {totalPagedResults}.
            const isCrest2Pagination = remainingPagedResults !== -1;
            const isCrest3Pagination = totalPagedResults !== -1;
            let dataTotalSize = result.length;

            if (isCrest2Pagination) {
                dataTotalSize = remainingPagedResults + result.length + this.state.pagedResultsOffset;
            } else if (isCrest3Pagination) {
                dataTotalSize = totalPagedResults;
            }

            this.setState({ dataTotalSize });
        };

        handleDataDelete = (numToRemove) => {
            // This handler will change the dataTotalSize with will in turn trigger the reload of the data from
            // the endpoint.
            const { dataTotalSize, pagedResultsOffset, sizePerPage } = this.state;
            const numItemsFromOffset = dataTotalSize - pagedResultsOffset;
            const lastPage = numItemsFromOffset <= sizePerPage;
            const multiplePages = dataTotalSize / sizePerPage > 1;
            const removedAllItemsFromLastPage = numItemsFromOffset === numToRemove;

            if (multiplePages && lastPage && removedAllItemsFromLastPage) {
                this.handlePageChange(this.state.page - 1, sizePerPage);
            }

            this.setState({ dataTotalSize: dataTotalSize - numToRemove });
        };

        handleTableChange = (type, newState) => {
            if (type === "sort") {
                this.handleSortChange(newState);
            }
        };

        render () {
            return (
                <WrappedComponent
                    pagination={ {
                        dataTotalSize: this.state.dataTotalSize,
                        hidePageListOnlyOnePage: true,
                        onDataChange: this.handleDataChange,
                        onDataDelete: this.handleDataDelete,
                        onPageChange: this.handlePageChange,
                        handleSearchChange: this.handleSearchChange,
                        onSizePerPageList: this.handleSizePerPageChange,
                        onTableChange: this.handleTableChange,
                        page: this.state.page,
                        pagedResultsOffset: this.state.pagedResultsOffset,
                        pagination: true,
                        remote: true,
                        searchTerm: this.state.searchTerm,
                        sizePerPage: this.state.sizePerPage,
                        sortDirection: this.state.sortDirection,
                        sortKey: this.state.sortKey
                    } }
                    { ...this.props }
                />
            );
        }
    };
};

withPagination.displayName = "withPagination";

export const withPaginationPropTypes = PropTypes.shape({
    dataTotalSize: PropTypes.number.isRequired,
    handleSearchChange: PropTypes.func.isRequired,
    onDataChange: PropTypes.func.isRequired,
    onDataDelete: PropTypes.func.isRequired,
    onPageChange: PropTypes.func.isRequired,
    onSizePerPageList: PropTypes.func.isRequired,
    onTableChange: PropTypes.func.isRequired,
    page: PropTypes.number.isRequired,
    pagedResultsOffset: PropTypes.number.isRequired,
    sizePerPage: PropTypes.number.isRequired,
    sortDirection: PropTypes.string.isRequired,
    sortKey: PropTypes.string.isRequired
});

export default withPagination;
