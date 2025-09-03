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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CRLException;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.IssuingDistributionPoint;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CRLHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Utility methods for X.509 certificate and CRL handling using Bouncy Castle.
 *
 * <p>
 * This class replaces usage of sun.security.x509 and related internal APIs. The main idea behind this
 * component is to contain the most of the certificate and CRL manipulation logic in this unit-testable class.
 */
public final class SecurityProviderCompat {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private SecurityProviderCompat() {}

    /**
     * Get BC certificate factory.
     *
     * @return Certificate factory instance.
     */
    public static CertificateFactory getCertificateFactory() throws NoSuchProviderException, CertificateException{
        return CertificateFactory.getInstance("X509", BouncyCastleProvider.PROVIDER_NAME);
    }

    /**
     * Get BC certification path validator.
     *
     * @return Certification path validator instance.
     */
    public static CertPathValidator getPathValidator() throws NoSuchProviderException, NoSuchAlgorithmException {
        return CertPathValidator.getInstance("PKIX", BouncyCastleProvider.PROVIDER_NAME);
    }

    /**
     * Get the Subject Alternative Names extension from the given certificate.
     *
     * @param cert X509 certificate to extract the SAN from. Never null.
     * @return Subject Alternative Names or null if not present in the certificate.
     * @throws IOException if parsing fails.
     */
    public static GeneralNames getSubjectAlternativeNames(X509Certificate cert) throws IOException {
        try {
            X509CertificateHolder holder = new JcaX509CertificateHolder(cert);
            Extension ext = holder.getExtension(Extension.subjectAlternativeName);
            if (ext == null) {
                return null;
            }
            ASN1Primitive value = ASN1Primitive.fromByteArray(ext.getExtnValue().getOctets());
            return GeneralNames.getInstance(value);
        } catch (CertificateEncodingException e) {
            throw new IOException("Failed to parse certificate", e);
        }
    }

    /**
     * Get CRL Distribution Points extension from the given certificate.
     *
     * @param cert X509 certificate to extract CRL distribution point from. Never null.
     * @return CRL Distribution Points or null the if not present in the certificate.
     * @throws IOException if parsing fails.
     */
    public static CRLDistPoint getCrlDistributionPoints(X509Certificate cert) throws IOException {
        try {
            X509CertificateHolder holder = new JcaX509CertificateHolder(cert);
            Extension ext = holder.getExtension(Extension.cRLDistributionPoints);
            if (ext == null) {
                return null;
            }
            ASN1Primitive value = ASN1Primitive.fromByteArray(ext.getExtnValue().getOctets());
            return CRLDistPoint.getInstance(value);
        } catch (CertificateEncodingException e) {
            throw new IOException("Failed to parse certificate", e);
        }
    }

    /**
     * Get Issuing Distribution Point extension from the given certificate.
     *
     * @param crl X509 CRL to extract issuing distribution point from. Never null.
     * @return Issuing Distribution Point or null the if not present in the certificate.
     * @throws IOException if parsing fails.
     */
    public static IssuingDistributionPoint getIssuingDistributionPoint(X509CRL crl) throws IOException {
        try {
            X509CRLHolder holder = new JcaX509CRLHolder(crl);
            Extension ext = holder.getExtension(Extension.issuingDistributionPoint);
            if (ext == null) {
                return null;
            }
            ASN1Primitive value = ASN1Primitive.fromByteArray(ext.getExtnValue().getOctets());
            return IssuingDistributionPoint.getInstance(value);
        } catch (CRLException e) {
            throw new IOException("Failed to parse certificate", e);
        }
    }

}
