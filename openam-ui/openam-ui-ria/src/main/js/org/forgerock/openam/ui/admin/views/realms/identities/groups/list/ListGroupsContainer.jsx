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
 * Copyright 2018-2022 ForgeRock AS.
 */
import { bindActionCreators } from "redux";
import { findIndex, isEqual, map, values } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { get, getAll, remove } from "org/forgerock/openam/ui/admin/services/realm/identities/GroupsService";
import { setInstances } from "store/modules/remote/config/realm/identities/groups/instances";
import connectWithStore from "components/redux/connectWithStore";
import ListGroups from "./ListGroups";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import showConfirmationBeforeAction from "org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeAction";
import withPagination, { withPaginationPropTypes }
    from "org/forgerock/openam/ui/admin/views/realms/common/withPagination";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class ListGroupsContainer extends Component {
    constructor () {
        super();

        this.state = {
            isFetching: true
        };
    }

    componentDidMount () {
        this.handleTableDataChange(this.props.pagination);
    }

    UNSAFE_componentWillReceiveProps (nextProps) {
        if (!isEqual(this.props.pagination, nextProps.pagination)) {
            this.handleTableDataChange(nextProps.pagination);
        }
    }

    handleDelete = (items) => {
        const realm = this.props.router.params[0];

        const hasGroupsUsers = (realm, ids) => {
            return Promise.all(map(ids, (id) => get(realm, id))).then((groups) => {
                const userCounts = map(groups, (group) => group.members.uniqueMember.length);
                return findIndex(userCounts, (count) => count > 0) >= 0;
            });
        };

        const removeUsers = (realm, ids) => {
            remove(realm, ids).then(() => {
                Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
                this.props.pagination.onDataDelete(ids.length);
            }, (response) => {
                Messages.addMessage({ response, type: Messages.TYPE_DANGER });
            });
        };

        const ids = map(items, "_id");
        hasGroupsUsers(realm, ids).then((hasExistingUsers) => {
            const message = hasExistingUsers
                ? t("console.identities.groups.confirmDeleteSelectedWithUsers", { count: ids.length })
                : t("console.identities.groups.confirmDeleteSelected", { count: ids.length });

            showConfirmationBeforeAction({ message }, () => {
                removeUsers(realm, ids);
            });
        });
    };

    handleEdit = (e, item) => {
        const id = item._id;
        const realm = this.props.router.params[0];

        Router.routeTo(Router.configuration.routes.realmsIdentitiesGroupsEdit, {
            args: map([realm, id], encodeURIComponent),
            trigger: true
        });
    };

    handleTableDataChange = (pagination) => {
        const realm = this.props.router.params[0];
        const additionalParams = {
            fields: ["username"],
            pagination
        };
        getAll(realm, additionalParams).then((response) => {
            this.setState({ isFetching: false });
            this.props.pagination.onDataChange(response);
            this.props.setInstances(response.result);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    };

    render () {
        const realm = this.props.router.params[0];
        const newHref = Router.getLink(Router.configuration.routes.realmsIdentitiesGroupsNew, [
            encodeURIComponent(realm)
        ]);

        return (
            <ListGroups
                isFetching={ this.state.isFetching }
                items={ this.props.groups }
                newHref={ `#${newHref}` }
                onDelete={ this.handleDelete }
                onRowClick={ this.handleEdit }
                options={ {
                    ...this.props.pagination
                } }
            />
        );
    }
}

ListGroupsContainer.propTypes = {
    groups: PropTypes.arrayOf(PropTypes.object),
    pagination: withPaginationPropTypes,
    router: withRouterPropType,
    setInstances: PropTypes.func.isRequired
};

ListGroupsContainer = connectWithStore(ListGroupsContainer,
    (state) => ({
        groups: values(state.remote.config.realm.identities.groups.instances)
    }),
    (dispatch) => ({
        setInstances: bindActionCreators(setInstances, dispatch)
    })
);
ListGroupsContainer = withRouter(ListGroupsContainer);
ListGroupsContainer = withPagination(ListGroupsContainer);

export default ListGroupsContainer;
