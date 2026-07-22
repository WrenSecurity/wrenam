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

import { Button, Tab, Tabs } from "react-bootstrap";

import { clone, get, set } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import MembersTab from "./EditGroupMembers";
import PageHeader from "components/PageHeader";
import PrivilegesTab from "./EditGroupPrivileges";

class EditGroup extends Component {
    handleMembersSave = (membersData) => {
        const data = clone(this.props.values);

        set(data, "members", membersData);

        this.props.onSave(data);
    };

    handlePrivilegesSave = (privilegesData) => {
        const data = clone(this.props.values);

        set(data, "privileges", privilegesData);

        this.props.onSave(data);
    };

    render () {
        return (
            <div>
                <PageHeader icon="folder" subtitle={ t("console.identities.groups.edit.type") } title={ this.props.id }>
                    <Button onClick={ this.props.onDelete }>
                        { t("common.form.delete") }
                    </Button>
                </PageHeader>
                <Tabs animation={ false } defaultActiveKey={ 1 } id="editGroup">
                    <Tab eventKey={ 1 } title={ t("console.identities.groups.edit.tabs.0") }>
                        <MembersTab
                            isFetching={ this.props.isFetching }
                            onAddAll={ this.props.onAddAll }
                            onRemoveAll={ this.props.onRemoveAll }
                            onSave={ this.handleMembersSave }
                            schema={ get(this.props.schema, "properties.members") }
                            values={ get(this.props.values, "members") }
                        />
                    </Tab>
                    <Tab eventKey={ 2 } title={ t("console.identities.groups.edit.tabs.1") }>
                        <PrivilegesTab
                            isFetching={ this.props.isFetching }
                            onSave={ this.handlePrivilegesSave }
                            schema={ get(this.props.schema, "properties.privileges") }
                            values={ get(this.props.values, "privileges") }
                        />
                    </Tab>
                </Tabs>
            </div>
        );
    }
}

EditGroup.propTypes = {
    id: PropTypes.string.isRequired,
    isFetching: PropTypes.bool.isRequired,
    onAddAll: PropTypes.func.isRequired,
    onDelete: PropTypes.func.isRequired,
    onRemoveAll: PropTypes.func.isRequired,
    onSave: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.object),
    values: PropTypes.objectOf(PropTypes.object)
};

export default EditGroup;
