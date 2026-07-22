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

import { Clearfix, Col, ControlLabel, FormControl, FormGroup, HelpBlock } from "react-bootstrap";
import { uniqueId } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

class FormGroupInput extends Component {
    handleOnChange = (event) => {
        this.props.onChange(event.target.value);
    };

    render () {
        const { isGrid, isValid, value, validationMessage, label, placeholder } = this.props;
        const message = !isValid && validationMessage
            ? <HelpBlock><small dangerouslySetInnerHTML={ { __html: validationMessage } } /></HelpBlock> // eslint-disable-line react/no-danger
            : null;
        const formControl = (
            <FormControl
                onChange={ this.handleOnChange }
                placeholder={ placeholder }
                type="text"
                value={ value }
            />
        );

        return isGrid
            ? (
                <FormGroup controlId={ uniqueId("formGroupInput") } validationState={ isValid ? null : "error" }>
                    <Clearfix>
                        <Col componentClass={ ControlLabel } sm={ 4 }>{ label }</Col>
                        <Col sm={ 6 }>
                            { formControl }
                            { message }
                        </Col>
                    </Clearfix>
                </FormGroup>
            )
            : (
                <FormGroup controlId={ uniqueId("formGroupInput") } validationState={ isValid ? null : "error" }>
                    <Clearfix>
                        <ControlLabel>{ label }</ControlLabel>
                        { formControl }
                        { message }
                    </Clearfix>
                </FormGroup>
            );
    }
}

FormGroupInput.defaultProps = {
    isGrid: true,
    isValid: true
};

FormGroupInput.propTypes = {
    isGrid: PropTypes.bool,
    isValid: PropTypes.bool,
    label: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    placeholder: PropTypes.string,
    validationMessage: PropTypes.string,
    value: PropTypes.string.isRequired
};

export default FormGroupInput;
