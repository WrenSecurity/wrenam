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

import { Button, ButtonGroup, DropdownButton, MenuItem } from "react-bootstrap";
import { camelCase, isEmpty, map } from "lodash";
import PropTypes from "prop-types";
import React from "react";

const AddButton = ({ children, href, title, onClick }) => {
    if (children) {
        const menuItems = map(children, (item) => {
            const { title, ...restProps } = item;
            return (
                <MenuItem key={ title } { ...restProps }>
                    { title }
                </MenuItem>
            );
        });

        return (
            <ButtonGroup>
                <DropdownButton
                    bsStyle="primary"
                    disabled={ isEmpty(menuItems) }
                    id={ camelCase(title) }
                    title={ title }
                >
                    { menuItems }
                </DropdownButton>
            </ButtonGroup>
        );
    } else {
        return (
            <ButtonGroup>
                <Button
                    bsStyle="primary"
                    href={ href }
                    onClick={ onClick }
                >
                    <i className="fa fa-plus" /> { title }
                </Button>
            </ButtonGroup>
        );
    }
};

AddButton.propTypes = {
    children: PropTypes.arrayOf(PropTypes.shape({
        onClick: PropTypes.func,
        href: PropTypes.string,
        title: PropTypes.string.isRequired
    })),
    href: PropTypes.string,
    onClick: PropTypes.func,
    title: PropTypes.string.isRequired
};

export default AddButton;
