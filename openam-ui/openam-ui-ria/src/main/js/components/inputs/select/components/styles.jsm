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
 * Copyright 2019-2021 ForgeRock AS.
 */

/*
 * Styled to look like our other selectors which are a themed bootstrap 3 mixed with react-select v1.0 or the jquery
 * selectize plugin. For more information about stylying react-select v2 @see https://react-select.com/styles
 */
const styles = {
    option: (base, state) => ({
        ...base,
        color: "#333",
        backgroundColor: state.isSelected ? "#b9d4cf" : state.isFocused ? "#f1f7f6" : "#fff"
    }),
    clearIndicator: (base) => ({
        ...base,
        padding: 6
    }),
    control: (base, state) => ({
        ...base,
        border: state.isFocused && !state.menuIsOpen ? "1px solid #66afe9" : "1px solid #ccc",
        ":hover": {
            border: state.isFocused && !state.menuIsOpen ? "1px solid #66afe9" : "1px solid #ccc"
        },
        minHeight: 34,
        outline: state.isFocused && !state.menuIsOpen ? 0 : base.outline,
        boxShadow: state.isFocused && !state.menuIsOpen
            ? "inset 0 1px 1px rgba(0,0,0,.075), 0 0 8px rgba(102, 175, 233, 0.6)"
            : 0
    }),
    groupHeading: (base) => ({
        ...base,
        color: "#457d78",
        background: "#fff",
        textTransform: "none",
        fontSize: "1em"
    }),
    group: (base) => ({
        ...base,
        borderBottom: "1px solid #ccc"
    }),
    indicatorSeparator: (base, state) => ({
        ...base,
        visibility: state.hasValue && !state.selectProps.isClearable ? "hidden" : base.visibility
    }),
    valueContainer: (base, state) => ({
        ...base,
        padding: state.isMulti && state.hasValue ? "2px" : base.padding
    }),
    menu: (base) => ({
        ...base,
        zIndex: 3
    }),
    noOptionsMessage: (base) => ({
        ...base,
        textAlign: "left"
    }),
    multiValue: (base) => ({
        ...base,
        border: "1px solid rgba(81,147,135,.24)",
        color: "#457d78",
        background: "#f1f6f5"
    }),
    multiValueLabel: (base) => ({
        ...base,
        color: "#457d78"
    }),
    multiValueRemove: (base) => ({
        ...base,
        borderLeft: "1px solid #d5e5e2",
        ":hover": {
            background: "#e4edeb",
            cursor: "pointer"
        }
    })
};

export default styles;
