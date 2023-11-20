/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.1.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.1.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2023 Wren Security
 */
package org.wrensecurity.wrenam.authentication.modules.duo;

public enum DuoAuthStatus {

    /**
     * Authentication succeeded. Application should grant access to the user.
     */
    ALLOW,

    /**
     * Authentication denied. Application should deny access to the user.
     */
    DENY,

    /**
     * Authentication is still in-progress. Application should poll again until it finishes.
     */
    WAITING

}
