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

import { ButtonToolbar, Panel } from "react-bootstrap";
import { identity, isEmpty, omit } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

import AllAuthenticatedButton from
    "org/forgerock/openam/ui/admin/views/realms/identities/groups/list/AllAuthenticatedButton";
import dataFormatReact from "components/table/cells/dataFormatReact";
import FontAwesomeIconCell from "components/table/cells/FontAwesomeIconCell";
import List from "components/list/List";

const ListGroups = (props) => {
    const allAuthenticatedToolbar = isEmpty(props.items)
        ? (
            <ButtonToolbar className="page-toolbar">
                <AllAuthenticatedButton />
            </ButtonToolbar>
        )
        : null;
    const columns = [{
        title: identity,
        dataField: "username",
        formatter: dataFormatReact(
            <FontAwesomeIconCell icon="folder" />
        ),
        sort: true,
        text: t("console.identities.groups.list.grid.0")
    }];
    return (
        <Panel className="fr-panel-tab">
            <Panel.Body>
                { allAuthenticatedToolbar }
                <List
                    { ...omit(props, "children") }
                    addButton={ {
                        href: props.newHref,
                        title: t("console.identities.groups.list.callToAction.button")
                    } }
                    additionalButtons={ <AllAuthenticatedButton /> }
                    columns={ columns }
                    description={ t("console.identities.groups.list.callToAction.description") }
                    keyField="username"
                    onPageChange={ props.onPageChange }
                    title={ t("console.identities.groups.list.callToAction.title") }
                />
            </Panel.Body>
        </Panel>
    );
};

ListGroups.propTypes = {
    items: PropTypes.arrayOf(PropTypes.object).isRequired,
    newHref: PropTypes.string.isRequired,
    onPageChange: PropTypes.func.isRequired
};

export default ListGroups;
