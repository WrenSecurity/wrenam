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

import java.lang.reflect.Method;
import java.util.List;
import sun.security.x509.CRLDistributionPointsExtension;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.DistributionPoint;
import sun.security.x509.GeneralNames;
import sun.security.x509.SubjectAlternativeNameExtension;
import sun.security.x509.X509CertInfo;

/**
 * Utility methods for maintaining compatibility with older supported JDK versions.
 */
public final class JdkProviderUtils {

    private static final int JAVA_VERSION = Runtime.version().feature();
    
    public static List<DistributionPoint> getDistributionPoints(CRLDistributionPointsExtension extension) throws Exception {
        if (JAVA_VERSION < 20) {
            Method getter = CRLDistributionPointsExtension.class.getMethod("get", String.class);
            return (List<DistributionPoint>) getter.invoke(extension, "points");
        }
        return extension.getDistributionPoints();
    }

    public static CertificateExtensions getExtensions(X509CertInfo certInfo) throws Exception {
        if (JAVA_VERSION < 20) {
            Method getter = X509CertInfo.class.getMethod("get", String.class);
            return (CertificateExtensions) getter.invoke(certInfo, X509CertInfo.EXTENSIONS);
        }
        return certInfo.getExtensions();
    }

    public static SubjectAlternativeNameExtension getSanExtension(CertificateExtensions extensions) throws Exception {
        if (JAVA_VERSION < 20) {
            Method getter = CertificateExtensions.class.getMethod("get", String.class);
            return (SubjectAlternativeNameExtension) getter.invoke(extensions, SubjectAlternativeNameExtension.NAME);
        }
        return (SubjectAlternativeNameExtension) extensions.getExtension(SubjectAlternativeNameExtension.NAME);
    }

    public static GeneralNames getSubjectNames(SubjectAlternativeNameExtension extension) throws Exception {
        if (JAVA_VERSION < 20) {
            Method getter = SubjectAlternativeNameExtension.class.getMethod("get", String.class);
            return (GeneralNames) getter.invoke(extension, "subject_name");
        }
        return extension.getNames();
    }

}
