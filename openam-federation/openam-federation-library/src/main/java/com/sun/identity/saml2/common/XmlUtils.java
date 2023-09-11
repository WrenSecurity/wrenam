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
 * Portions Copyright 2023 Wren Security.
 */
package com.sun.identity.saml2.common;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * XML-manipulation utility methods.
 */
public class XmlUtils {

    /**
     * Create namespace element with provided parameters.
     */
    public static Element createNamespaceElement(Document document, String prefix, String value) {
        Element ctx = document.createElementNS(null, "namespaceContext");
        ctx.setAttributeNS(SAML2Constants.NS_XML, "xmlns:" + prefix, value);
        return ctx;
    }

}
