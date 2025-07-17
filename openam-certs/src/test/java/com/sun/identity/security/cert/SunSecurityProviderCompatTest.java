package com.sun.identity.security.cert;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.util.List;
import org.testng.annotations.Test;
import sun.security.x509.CRLDistributionPointsExtension;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.DistributionPoint;
import sun.security.x509.DistributionPointName;
import sun.security.x509.GeneralNames;
import sun.security.x509.IssuingDistributionPointExtension;
import sun.security.x509.SubjectAlternativeNameExtension;
import sun.security.x509.X509CRLImpl;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

/**
 * {@link SunSecurityProviderCompat} test cases.
 */
@SuppressWarnings("restriction")
public class SunSecurityProviderCompatTest {

    @Test
    public void testSubjectAlternativeNameExtraction() throws Exception {
        X509CertImpl cert = readCertificate("/compat/user.cert.pem");
        assertNotNull(cert);

        X509CertInfo certInfo = new X509CertInfo(cert.getTBSCertificate());
        assertNotNull(certInfo);

        CertificateExtensions extensions = SunSecurityProviderCompat.getExtensions(certInfo);
        assertNotNull(extensions);

        SubjectAlternativeNameExtension sanExtension = SunSecurityProviderCompat.getSanExtension(extensions);
        assertNotNull(sanExtension);

        GeneralNames sanValues = SunSecurityProviderCompat.getSubjectAlternativeNames(sanExtension);
        assertNotNull(sanValues);
        assertFalse(sanValues.names().isEmpty());
    }

    @Test
    public void testDistributionPointExtraction() throws Exception {
        X509CertImpl cert = readCertificate("/compat/user.cert.pem");
        assertNotNull(cert);

        CRLDistributionPointsExtension crlDistributionPointsExtension = cert.getCRLDistributionPointsExtension();
        assertNotNull(crlDistributionPointsExtension);

        List<DistributionPoint> distributionPoints = SunSecurityProviderCompat
                .getDistributionPoints(crlDistributionPointsExtension);
        assertFalse(distributionPoints.isEmpty());
    }

    @Test
    public void testRevocationListIssuerExtraction() throws Exception {
        X509CRLImpl crl = readCertificateRevocationList("/compat/sample.crl.pem");
        assertNotNull(crl);

        IssuingDistributionPointExtension issuingDistributionPointExtension = crl.getIssuingDistributionPointExtension();
        assertNotNull(issuingDistributionPointExtension);

        DistributionPointName distributionPoint = SunSecurityProviderCompat.getDistributionPoint(issuingDistributionPointExtension);
        assertNotNull(distributionPoint);
    }

    private X509CertImpl readCertificate(String path) throws Exception {
        try (InputStream input = SunSecurityProviderCompatTest.class.getResourceAsStream(path)) {
            return new X509CertImpl(input);
        }
    }

    private X509CRLImpl readCertificateRevocationList(String path) throws Exception {
        try (InputStream input = SunSecurityProviderCompatTest.class.getResourceAsStream(path)) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            return (X509CRLImpl) certificateFactory.generateCRL(input);
        }
    }

}
