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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2024 Wren Security.
 */

/**
 * @module org/forgerock/openam/ui/user/login/logout
 */

import $ from "jquery";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import {
    get as getSessionToken, remove as removeSessionToken
} from "org/forgerock/openam/ui/user/login/tokens/SessionToken";
import { isSessionValid, logout as serviceLogout } from "org/forgerock/openam/ui/user/services/SessionService";

const logout = () => {
    const sessionToken = getSessionToken();
    Configuration.setProperty("loggedUser", null);

    if (sessionToken) {
        return isSessionValid().then((isValid) => {
            if (isValid) {
                return serviceLogout();
            } else {
                return $.Deferred().resolve();
            }
        })
            .then(removeSessionToken, removeSessionToken);
    } else {
        return $.Deferred().resolve();
    }
};

export default logout;
