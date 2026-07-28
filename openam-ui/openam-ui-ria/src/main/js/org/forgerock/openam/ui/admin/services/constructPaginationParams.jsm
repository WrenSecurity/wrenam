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
 * Copyright 2018-2019 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/constructPaginationParams
 */

/**
 * Assembles the pagination query parameter string from the passed in pagination object returned by
 * the react-bootstrap-table.
 * @param {object} [pagination] The pagination object
 * @param {number} pagination.sizePerPage The number of results per page.
 * @param {number} pagination.pagedResultsOffset The paged results offset.
 * @param {string} pagination.sortKey The sort key.
 * @param {string} pagination.sortDirection The sort direction.
 * @returns {string} Returns the pagination query string or empty string.
 */
const constructPaginationParams = (pagination) => {
    if (pagination) {
        const { sizePerPage, pagedResultsOffset, sortKey, sortDirection } = pagination;
        const pagedResultsOffsetParam = pagedResultsOffset ? `&_pagedResultsOffset=${pagedResultsOffset}` : "";
        const sizePerPageParam = `&_pageSize=${sizePerPage}`;
        const sortParams = sortKey && sortDirection ? `&_sortKeys=${encodeURIComponent(sortDirection)}${sortKey}` : "";

        return `${pagedResultsOffsetParam}${sizePerPageParam}${sortParams}`;
    } else {
        return "";
    }
};

export default constructPaginationParams;
