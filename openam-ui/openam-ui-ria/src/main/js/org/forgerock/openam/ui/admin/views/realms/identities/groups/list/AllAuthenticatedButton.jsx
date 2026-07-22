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

import { Button } from "react-bootstrap";
import { t } from "i18next";
import React from "react";

import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

const AllAuthenticatedButton = ({ router }) => {
    const realm = router.params[0];
    const href = `#${Router.getLink(Router.configuration.routes.realmsIdentitiesAllAuthenticatedEdit, [
        encodeURIComponent(realm), "allAuthenticatedIdentities"
    ])}`;

    return (
        <Button
            bsStyle="link"
            className="pull-right"
            href={ href }
        >
            <i className="fa fa-users" /> { t("console.identities.groups.list.allAuthenticatedButton") }
        </Button>
    );
};

AllAuthenticatedButton.propTypes = {
    router: withRouterPropType
};

export default withRouter(AllAuthenticatedButton);
