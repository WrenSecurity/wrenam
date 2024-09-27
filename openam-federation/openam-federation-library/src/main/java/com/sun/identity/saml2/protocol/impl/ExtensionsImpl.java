/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * Portions Copyrighted 2024 Wren Security.
 */

package com.sun.identity.saml2.protocol.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.JAXBException;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.xml.bind.JAXBObject;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.protocol.Extensions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The class defines methods for adding protocol message extension elements.
 */
public class ExtensionsImpl implements Extensions {

    private boolean isMutable = false;
    private List<Object> extensionsList = null;

    public ExtensionsImpl() {
        isMutable = true;
    }

    /**
     * Constructor to create the <code>Extensions</code> Object.
     *
     * @param element the Document Element of <code>Extensions</code> object.
     * @throws SAML2Exception if <code>Extensions</code> cannot be created.
     */
    public ExtensionsImpl(Element element) throws SAML2Exception {
        parseElement(element);
    }

    /**
     * Constructor to create the <code>Extensions</code> Object.
     *
     * @param xmlString the XML String representation of this object.
     * @throws SAML2Exception if <code>Extensions</code> cannot be created.
     */
    public ExtensionsImpl(String xmlString) throws SAML2Exception {
        Document xmlDocument = XMLUtils.toDOMDocument(xmlString,SAML2SDKUtils.debug);
        if (xmlDocument == null) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(xmlDocument.getDocumentElement());
    }

    /**
     * Sets the <code>Extensions</code> object.
     *
     * @param value List of XML Strings <code>Extensions</code> objects
     * @throws SAML2Exception if the object is immutable.
     * @see #getAny()
     */
    public void setAny(List<Object> value) throws SAML2Exception {
        if (isMutable) {
            extensionsList = value;
        } else {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }

    /**
     * Returns the list of <code>Extensions</code> object.
     *
     * @return a List of XML Strings <code>Extensions</code> objects.
     * @see #setAny(List)
     */
    public List<Object> getAny() {
        return extensionsList;
    }

    /**
     * Returns a String representation of this object.
     *
     * @return a String representation of this object.
     * @throws SAML2Exception if cannot convert to String.
     */
    public String toXMLString() throws SAML2Exception {
        return toXMLString(true,false);
    }

    /**
     * Returns a String representation of this object.
     *
     * @param includeNSPrefix determines whether or not the namespace
     *	      qualifier is prepended to the Element when converted
     * @param declareNS determines whether or not the namespace is declared
     *	      within the Element.
     * @return the String representation of this Object.
     * @throws SAML2Exception if cannot convert to String.
     */
    public String toXMLString(boolean includeNSPrefix, boolean declareNS) throws SAML2Exception {
        if (extensionsList == null || extensionsList.isEmpty()) {
            return null;
        }
        StringBuffer xmlString = new StringBuffer(500);
        xmlString.append(SAML2Constants.START_TAG);
        if (includeNSPrefix) {
            xmlString.append(SAML2Constants.PROTOCOL_PREFIX);
        }
        xmlString.append(SAML2Constants.EXTENSIONS);
        if (declareNS) {
            xmlString.append(SAML2Constants.PROTOCOL_DECLARE_STR);
        }
        xmlString.append(SAML2Constants.END_TAG);
        for (Object extension : extensionsList) {
            String content = serializeExtension(extension);
            if (content != null) {
                xmlString.append(SAML2Constants.NEWLINE).append(content);
            }
        }
        xmlString.append(SAML2Constants.NEWLINE)
                .append(SAML2Constants.SAML2_END_TAG)
                .append(SAML2Constants.EXTENSIONS)
                .append(SAML2Constants.END_TAG);
        return xmlString.toString();
    }

    /**
     * Makes this object immutable.
     */
    public void makeImmutable() {
        if (isMutable) {
            isMutable = false;
        }
    }

    /**
     * Returns value true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable() {
        return isMutable;
    }

    /**
     * Parse the specified Extension DOM element.
     */
    private void parseElement(Element element) {
        NodeList nList = element.getChildNodes();
        if (extensionsList == null || extensionsList.isEmpty()) {
            extensionsList = new ArrayList<>();
        }
        if (nList != null && nList.getLength() > 0) {
            for (int i = 0; i < nList.getLength(); i++) {
                Node childNode = nList.item(i);
                if (childNode.getLocalName() != null) {
                    extensionsList.add(XMLUtils.print(childNode));
                }
            }
            if (extensionsList != null && !extensionsList.isEmpty()) {
                extensionsList = Collections.unmodifiableList(extensionsList);
            }
        }
    }

    /**
     * Serialize the specified extension object into string value.
     */
    private String serializeExtension(Object extension) {
        if (extension == null) {
            return null;
        }
        if (extension instanceof String) {
            return (String) extension;
        }
        if (extension instanceof JAXBObject) {
            try {
                return SAML2MetaUtils.convertJAXBToString(extension, false, true);
            } catch (JAXBException e) {
                throw new IllegalStateException("Failed to serialize JAXB object.", e);
            }
        }
        return null;
    }

}
