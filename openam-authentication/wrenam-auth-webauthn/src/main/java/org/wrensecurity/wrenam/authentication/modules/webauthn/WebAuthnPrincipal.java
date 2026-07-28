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
 * Copyright 2025 Wren Security. All rights reserved.
 */
package org.wrensecurity.wrenam.authentication.modules.webauthn;

import java.io.Serializable;
import java.security.Principal;

/**
 * WebAuthnPrincipal principal representation.
 */
public class WebAuthnPrincipal implements Principal, Serializable {

    private final String name;

    public WebAuthnPrincipal(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Principal's name cannot be null");
        }
        this.name = name;
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) {
            return true;
        }
        if (another == null || getClass() != another.getClass()) {
            return false;
        }
        WebAuthnPrincipal that = (WebAuthnPrincipal) another;
        return name.equals(that.name);
    }

    @Override
    public String toString() {
        return "WebAuthnPrincipal: " + name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String getName() {
        return name;
    }

}
