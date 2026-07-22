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

import { connect } from "react-redux";
import React from "react";

import store from "store";

const getDisplayName = (WrappedComponent) => {
    return WrappedComponent.displayName || WrappedComponent.name || "Component";
};

/**
 * A HoC (higher-order component) that wraps another component to provide `this.props.store`.
 * Pass in your component and it will return the wrapped component.
 * @param {ReactComponent} WrappedComponent Component to wrap
 * @returns {ReactComponent} Wrapped component
 * @example
 * import connectWithStore from "components/redux/connectWithStore"
 * import store from "store";
 *
 * class MyReactComponent extends Component { ... }
 *
 * const mapStateToProps = (state) => { ... }
 * const mapDispatchToProps = (dispatch) => { ... )
 *
 * export default connectWithStore(MyReactComponent, mapStateToProps, mapDispatchToProps)
 * @see https://github.com/reactjs/react-redux/blob/master/docs/api.md#connectmapstatetoprops-mapdispatchtoprops-mergeprops-options
 */
const connectWithStore = (WrappedComponent, ...args) => {
    const ConnectedWrappedComponent = connect(...args)(WrappedComponent);

    const component = (props) => <ConnectedWrappedComponent { ...props } store={ store } />;
    component.displayName = `connectWithStore(${getDisplayName(WrappedComponent)})`;
    component.WrappedComponent = WrappedComponent;

    return component;
};

export default connectWithStore;
