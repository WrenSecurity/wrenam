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
 * Copyright 2014 ForgeRock AS.
 * Portions Copyrighted 2024 Wren Security.
 */

package com.sun.identity.saml2.meta;

import static org.testng.Assert.*;

import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.NameIDFormatElement;
import com.sun.identity.saml2.jaxb.metadata.impl.EntityDescriptorElementImpl;
import com.sun.identity.saml2.jaxb.metadata.impl.NameIDFormatElementImpl;
import javax.xml.bind.JAXBException;
import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

public class SAML2MetaUtilsTest {

    private static final String PATH_SEPARATOR = "/";
    private static final String TEST_ENTITY = "someEntity";
    private static final String PREFIX = PATH_SEPARATOR + "abcdefg";
    private static final String TEST_SUB_REALM = "subsub";

    public SAML2MetaUtilsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    @Test
    public void testGetMetaDataByURI_DefaultRealm() {
        final String uri = PREFIX
                + PATH_SEPARATOR + SAML2MetaManager.NAME_META_ALIAS_IN_URI
                + PATH_SEPARATOR + TEST_ENTITY;
        final String result = SAML2MetaUtils.getMetaAliasByUri(uri);
        assertEquals(result, PATH_SEPARATOR + TEST_ENTITY);
    }

    @Test
    public void testGetMetaDataByURI_SubRealm() {
        final String uri = PREFIX
                + PATH_SEPARATOR + SAML2MetaManager.NAME_META_ALIAS_IN_URI
                + PATH_SEPARATOR + TEST_SUB_REALM
                + PATH_SEPARATOR +  TEST_ENTITY;
        final String result = SAML2MetaUtils.getMetaAliasByUri(uri);
        assertEquals(result, PATH_SEPARATOR + TEST_SUB_REALM + PATH_SEPARATOR + TEST_ENTITY);
    }

    @Test
    public void testConvertJAXBToString() throws JAXBException {
        // Document
        EntityDescriptorElement descriptor = new EntityDescriptorElementImpl();
        descriptor.setEntityID("foobar");
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<EntityDescriptor entityID=\"foobar\" xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\"/>\n";
        assertEquals(SAML2MetaUtils.convertJAXBToString(descriptor), expected);
        // Fragment
        NameIDFormatElement nameId = new NameIDFormatElementImpl("urn:oasis:names:tc:SAML:2.0:nameid-format:transient");
        expected = "<NameIDFormat xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\">urn:oasis:names:tc:SAML:2.0:nameid-format:transient</NameIDFormat>";
        assertEquals(SAML2MetaUtils.convertJAXBToString(nameId, false, true), expected);
    }

}
