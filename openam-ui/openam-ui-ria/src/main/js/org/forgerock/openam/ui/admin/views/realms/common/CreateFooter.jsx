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
 * Copyright 2017-2019 ForgeRock AS.
 */

import { Button, Clearfix } from "react-bootstrap";
import { map } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

const CreateFooter = ({ backRoute, backRouteArgs, disabled, onCreateClick, router }) => {
    const realm = router.params[0];
    const backLinkArguments = map([realm, ...backRouteArgs], encodeURIComponent);
    const backLink = `#${Router.getLink(backRoute, backLinkArguments)}`;

    return (
        <Clearfix>
            <div className="pull-right">
                <div className="am-btn-action-group">
                    <Button href={ backLink } >
                        { t ("common.form.cancel") }
                    </Button>
                    <Button
                        bsStyle="primary"
                        disabled={ disabled }
                        onClick={ onCreateClick }
                    >
                        { t("common.form.create") }
                    </Button>
                </div>
            </div>
        </Clearfix>
    );
};

CreateFooter.defaultProps = {
    backRouteArgs: [],
    disabled: false
};

CreateFooter.propTypes = {
    backRoute: PropTypes.shape({
        url: PropTypes.oneOfType([
            PropTypes.string,
            PropTypes.instanceOf(RegExp)
        ]).isRequired,
        pattern: PropTypes.string.isRequired
    }).isRequired,
    backRouteArgs: PropTypes.arrayOf(PropTypes.string),
    disabled: PropTypes.bool,
    onCreateClick: PropTypes.func.isRequired,
    router: withRouterPropType
};

export default withRouter(CreateFooter);
