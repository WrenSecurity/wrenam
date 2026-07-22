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

import { bindActionCreators } from "redux";
import { get } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { get as getGroup, getSchema, update }
    from "org/forgerock/openam/ui/admin/services/realm/identities/AllAuthenticatedService";
import { setSchema } from "store/modules/remote/config/realm/identities/groups/schema";
import { addOrUpdate as addOrUpdateValues } from "store/modules/local/config/realm/identities/groups/values";
import connectWithStore from "components/redux/connectWithStore";
import EditAllAuthenticated from "./EditAllAuthenticated";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class EditAllAuthenticatedContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            isFetching: true
        };
    }

    componentDidMount () {
        const realm = this.props.router.params[0];

        Promise.all([getSchema(realm), getGroup(realm)]).then(([schema, values]) => {
            this.setState({ isFetching: false });
            this.props.setSchema(schema);
            this.props.addOrUpdateValues(values);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    handleSave = (privileges) => {
        const realm = this.props.router.params[0];

        update(realm, { privileges }).then(() => {
            Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    };

    render () {
        return (
            <EditAllAuthenticated
                isFetching={ this.state.isFetching }
                onSave={ this.handleSave }
                schema={ get(this.props.schema, "properties.privileges") }
                values={ get(this.props.values, "privileges") }
            />
        );
    }
}

EditAllAuthenticatedContainer.propTypes = {
    addOrUpdateValues: PropTypes.func.isRequired,
    router: withRouterPropType,
    schema: PropTypes.shape({
        type: PropTypes.string
    }),
    setSchema: PropTypes.func.isRequired,
    values: PropTypes.shape({
        type: PropTypes.string
    })
};

EditAllAuthenticatedContainer = connectWithStore(EditAllAuthenticatedContainer,
    (state) => {
        return {
            schema: state.remote.config.realm.identities.groups.schema,
            values: get(state.local.config.realm.identities.groups.values, "allauthenticatedusers")
        };
    },
    (dispatch) => ({
        setSchema: bindActionCreators(setSchema, dispatch),
        addOrUpdateValues: bindActionCreators(addOrUpdateValues, dispatch)
    })
);
EditAllAuthenticatedContainer = withRouter(EditAllAuthenticatedContainer);

export default EditAllAuthenticatedContainer;
