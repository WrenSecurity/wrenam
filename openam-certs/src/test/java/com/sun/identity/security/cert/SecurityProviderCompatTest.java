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
 * Copyright Wren Security 2025
 */
package com.sun.identity.security.cert;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.IssuingDistributionPoint;
import org.bouncycastle.asn1.x509.OtherName;
import org.testng.annotations.Test;

/**
 * {@link SecurityProviderCompat} test case.
 */
public class SecurityProviderCompatTest {

    @Test
    public void testGetSubjectAlternativeName() throws Exception {
        X509Certificate cert = loadCertificate("/compat/user.cert.pem");
        assertNotNull(cert);

        GeneralNames san = SecurityProviderCompat.getSubjectAlternativeNames(cert);
        assertNotNull(san);
        assertTrue(san.getNames().length > 0);

        GeneralName name = san.getNames()[0];
        assertNotNull(name);

        OtherName otherName = OtherName.getInstance(name.getName());
        assertEquals(otherName.getTypeID().getId(), "1.3.6.1.4.1.311.20.2.3");
        assertEquals(otherName.getValue().toString(), "john.smith@example.com");
    }

    @Test
    public void testGetCrlDistributionPoints() throws Exception {
        X509Certificate cert = loadCertificate("/compat/user.cert.pem");
        assertNotNull(cert);

        CRLDistPoint dpExt = SecurityProviderCompat.getCrlDistributionPoints(cert);
        assertNotNull(dpExt);

        DistributionPoint[] dps = dpExt.getDistributionPoints();
        assertNotNull(dps);
        assertTrue(dps.length > 0);
    }

    @Test
    public void testGetIssuingDistributionPoint() throws Exception {
        X509CRL crl = loadCRL("/compat/sample.crl.pem");
        assertNotNull(crl);

        IssuingDistributionPoint idpExt = SecurityProviderCompat.getIssuingDistributionPoint(crl);
        assertNotNull(idpExt);

        DistributionPointName dpName = idpExt.getDistributionPoint();
        assertNotNull(dpName);
        assertTrue(dpName.getName() instanceof GeneralNames);

    }

    private X509Certificate loadCertificate(String resource) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(in);
        }
    }

    private X509CRL loadCRL(String resource) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509CRL) cf.generateCRL(in);
        }
    }
}
