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

import { isEmpty, map } from "lodash";
import React, { Component } from "react";

import { create } from "org/forgerock/openam/ui/admin/services/realm/identities/GroupsService";
import isValidId from "org/forgerock/openam/ui/admin/views/realms/common/isValidId";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewGroup from "./NewGroup";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class NewGroupContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            id: ""
        };
    }

    handleCreate = () => {
        const realm = this.props.router.params[0];

        create(realm, this.state.id).then(() => {
            Router.routeTo(Router.configuration.routes.realmsIdentitiesGroupsEdit,
                { args: map([realm, this.state.id], encodeURIComponent), trigger: true });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    };

    handleIdChange = (id) => {
        this.setState({ id });
    };

    render () {
        const validId = isValidId(this.state.id);
        const createAllowed = validId && !isEmpty(this.state.id);

        return (
            <NewGroup
                id={ this.state.id }
                isCreateAllowed={ createAllowed }
                isValidId={ validId }
                onCreate={ this.handleCreate }
                onIdChange={ this.handleIdChange }
            />
        );
    }
}

NewGroupContainer.propTypes = {
    router: withRouterPropType
};

NewGroupContainer = withRouter(NewGroupContainer);

export default NewGroupContainer;
