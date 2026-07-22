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

import { Button, Col, Form, FormGroup, Panel } from "react-bootstrap";

import { isEqual } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import EditFooter from "org/forgerock/openam/ui/admin/views/realms/common/EditFooter";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Loading from "components/Loading";

class EditGroupMembers extends Component {
    UNSAFE_componentWillReceiveProps (nextProps) {
        if (this.jsonSchemaView && !isEqual(this.props.schema, nextProps.schema)) {
            this.jsonSchemaView.destroy();
            this.jsonSchemaView = null;
        }

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
                <div>
                    <Form className="edit-group-member-selection" horizontal>
                        <div ref={ this.setRef } />
                    </Form>

                    <FormGroup>
                        <Col sm={ 6 } smOffset={ 4 }>
                            <div className="am-btn-action-group">
                                <Button onClick={ this.props.onAddAll }>
                                    { t("common.form.addAll") }
                                </Button>
                                <Button onClick={ this.props.onRemoveAll }>
                                    { t("common.form.removeAll") }
                                </Button>
                            </div>
                        </Col>
                    </FormGroup>
                </div>
            );
        }

        return (
            <Panel className="fr-panel-tab">
                <Panel.Body>{ content }</Panel.Body>
                <Panel.Footer>
                    <EditFooter onSaveClick={ this.handleSave } />
                </Panel.Footer>
            </Panel>
        );
    }
}

EditGroupMembers.propTypes = {
    isFetching: PropTypes.bool.isRequired,
    onAddAll: PropTypes.func.isRequired,
    onRemoveAll: PropTypes.func.isRequired,
    onSave: PropTypes.func.isRequired,
    schema: PropTypes.objectOf(PropTypes.object),
    values: PropTypes.objectOf(PropTypes.object)
};

export default EditGroupMembers;
