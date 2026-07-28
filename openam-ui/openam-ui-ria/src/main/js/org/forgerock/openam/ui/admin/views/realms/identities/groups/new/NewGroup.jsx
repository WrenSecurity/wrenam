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
import React from "react";

import CreateFooter from "org/forgerock/openam/ui/admin/views/realms/common/CreateFooter";
import FormGroupInput from "org/forgerock/openam/ui/admin/views/realms/common/FormGroupInput";
import PageHeader from "components/PageHeader";
import Router from "org/forgerock/commons/ui/common/main/Router";

const NewGroup = ({ id, isCreateAllowed, isValidId, onCreate, onIdChange }) => {
    return (
        <div>
            <PageHeader title={ t("console.identities.groups.new.title") } />
            <Panel>
                <Panel.Body>
                    <Form horizontal>
                        <FormGroupInput
                            isValid={ isValidId }
                            label={ t("console.identities.groups.new.groupId") }
                            onChange={ onIdChange }
                            validationMessage={ t("console.common.validation.invalidCharacters") }
                            value={ id }
                        />
                    </Form>
                </Panel.Body>
                <Panel.Footer>
                    <CreateFooter
                        backRoute={ Router.configuration.routes.realmsIdentities }
                        disabled={ !isCreateAllowed }
                        onCreateClick={ onCreate }
                    />
                </Panel.Footer>
            </Panel>
        </div>
    );
};

NewGroup.propTypes = {
    id: PropTypes.string.isRequired,
    isCreateAllowed: PropTypes.bool.isRequired,
    isValidId: PropTypes.bool.isRequired,
    onCreate: PropTypes.func.isRequired,
    onIdChange: PropTypes.func.isRequired
};

export default NewGroup;
