/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: CertAuthPrincipal.java,v 1.2 2008/06/25 05:41:55 qcheng Exp $
 *
 * Portions Copyrighted [2011] [ForgeRock AS]
 * Portions Copyrighted 2025 Wren Security
 */
package com.sun.identity.authentication.modules.cert;

import java.security.Principal;

public class CertAuthPrincipal implements Principal, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    public CertAuthPrincipal(String name) {
        if (name == null) {
            throw new NullPointerException("illegal null input");
        }
        this.name = name;
    }

    /**
     * Returns the username for this <code>CertAuthPrincipal</code>.
     *
     * @return the username for this <code>CertAuthPrincipal</code>
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns a string representation of this <code>CertAuthPrincipal</code>.
     *
     * @return a string representation of this <code>CertAuthPrincipal</code>.
     */
    @Override
    public String toString() {
        return("CertAuthPrincipal:  " + name);
    }

    /**
     * Compares the specified Object with this <code>CertAuthPrincipal</code>
     * for equality.  Returns <code>true</code> if the given object is also a
     * <code>CertAuthPrincipal</code> and the two CertAuthPrincipals
     * have the same user name.
     *
     * @param o Object to be compared for equality with this
     *        <code>CertAuthPrincipal</code>.
     * @return <code>true</code> if the specified Object is equal equal to this
     *         <code>CertAuthPrincipal</code>.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        if (!(o instanceof CertAuthPrincipal)) {
            return false;
        }

        CertAuthPrincipal that = (CertAuthPrincipal)o;
        return this.getName().equals(that.getName());
    }

    /**
     * Returns a hash code for this <code>CertAuthPrincipal</code>.
     *
     * @return a hash code for this <code>CertAuthPrincipal</code>.
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
