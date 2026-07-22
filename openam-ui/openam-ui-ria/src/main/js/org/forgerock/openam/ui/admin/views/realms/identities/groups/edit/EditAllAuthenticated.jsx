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

import { Form, Panel } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import EditFooter from "org/forgerock/openam/ui/admin/views/realms/common/EditFooter";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Loading from "components/Loading";
import PageHeader from "components/PageHeader";

class EditAllAuthenticated extends Component {
    UNSAFE_componentWillReceiveProps (nextProps) {
        if (this.jsonSchemaView) {
            this.jsonSchemaView.setData(nextProps.values);
            this.jsonSchemaView.render();
        }
    }

    componentDidUpdate () {
        if (!this.jsonSchemaView && this.props.values && this.props.schema) {
            this.jsonSchemaView = new FlatJSONSchemaView({
                schema: new JSONSchema(this.props.schema),
                values: new JSONValues(this.props.values)
            });
            this.jsonForm.appendChild(this.jsonSchemaView.render().el);
        }
    }

    setRef = (element) => {
        this.jsonForm = element;
    };

    handleSave = () => {
        this.props.onSave(this.jsonSchemaView.getData());
    };

    render () {
        let content;

        if (this.props.isFetching) {
            content = <Loading />;
        } else {
            content = (
                <Form horizontal>
                    <div ref={ this.setRef } />
                </Form>
            );
        }

        return (
            <div>
                <PageHeader
                    icon="users"
                    subtitle={ t("console.identities.groups.edit.allAuthenticated.type") }
                    title={ t("console.identities.groups.edit.allAuthenticated.title") }
                />
                <Panel>
                    <Panel.Body>{ content }</Panel.Body>
                    <Panel.Footer>
                        <EditFooter disabled={ this.props.isFetching } onSaveClick={ this.handleSave } />
                    </Panel.Footer>
                </Panel>
            </div>
        );
    }
}

EditAllAuthenticated.propTypes = {
    isFetching: PropTypes.bool.isRequired,
    onSave: PropTypes.func.isRequired,
    schema: PropTypes.shape({
        type: PropTypes.string
    }),
    values: PropTypes.shape({
        type: PropTypes.string
    })
};

export default EditAllAuthenticated;
