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
import sun.security.x509.DistributionPointName;
import sun.security.x509.GeneralNames;
import sun.security.x509.IssuingDistributionPointExtension;
import sun.security.x509.SubjectAlternativeNameExtension;
import sun.security.x509.X509CertInfo;

/**
 * Utility methods for maintaining compatibility with older supported JDK versions.
 *
 * <p>
 * This class exists only because we are using JDK internals that are not guaranteed being stable across releases.
 * Ideally the whole X.509 code that relies on <code>sun.security.x509</code> should be replaced. Using reflection
 * gets rid of build time type checking and makes the codebase less future-proof.
 *
 * <p>
 * Issues solved by this class:
 * <ul>
 * <li> binary incompatibility introduced by https://bugs.openjdk.org/browse/JDK-8296072
 */
@SuppressWarnings({ "restriction", "unchecked" })
public final class SunSecurityProviderCompat {

    public static List<DistributionPoint> getDistributionPoints(CRLDistributionPointsExtension extension) throws Exception {
        try {
            Method typesafeGetter = CRLDistributionPointsExtension.class.getMethod("getDistributionPoints");
            return (List<DistributionPoint>) typesafeGetter.invoke(extension);
        } catch (NoSuchMethodException e) { /* fall through */ }
        try {
            Method legacyGetter = CRLDistributionPointsExtension.class.getMethod("get", String.class);
            return (List<DistributionPoint>) legacyGetter.invoke(extension, "points");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unsupported SUN provider implementation", e);
        }
    }

    public static DistributionPointName getDistributionPoint(IssuingDistributionPointExtension extension) throws Exception {
        try {
            Method typesafeGetter = IssuingDistributionPointExtension.class.getMethod("getDistributionPoint");
            return (DistributionPointName) typesafeGetter.invoke(extension);
        } catch (NoSuchMethodException e) { /* fall through */ }
        try {
            Method legacyGetter = IssuingDistributionPointExtension.class.getMethod("get", String.class);
            return (DistributionPointName) legacyGetter.invoke(extension, "point");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unsupported SUN provider implementation", e);
        }
    }

    public static CertificateExtensions getExtensions(X509CertInfo certInfo) throws Exception {
        try {
            Method typesafeGetter = X509CertInfo.class.getMethod("getExtensions");
            return (CertificateExtensions) typesafeGetter.invoke(certInfo);
        } catch (NoSuchMethodException e) { /* fall through */ }
        try {
            Method legacyGetter = X509CertInfo.class.getMethod("get", String.class);
            return (CertificateExtensions) legacyGetter.invoke(certInfo, X509CertInfo.EXTENSIONS);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unsupported SUN provider implementation", e);
        }
    }

    public static SubjectAlternativeNameExtension getSanExtension(CertificateExtensions extensions) throws Exception {
        try {
            Method typesafeGetter = CertificateExtensions.class.getMethod("getExtension", String.class);
            return (SubjectAlternativeNameExtension) typesafeGetter.invoke(extensions, SubjectAlternativeNameExtension.NAME);
        } catch (NoSuchMethodException e) { /* fall through */ }
        try {
            Method getter = CertificateExtensions.class.getMethod("get", String.class);
            return (SubjectAlternativeNameExtension) getter.invoke(extensions, SubjectAlternativeNameExtension.NAME);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unsupported SUN provider implementation", e);
        }
    }

    public static GeneralNames getSubjectAlternativeNames(SubjectAlternativeNameExtension extension) throws Exception {
        try {
            Method typesafeGetter = SubjectAlternativeNameExtension.class.getMethod("getNames");
            return (GeneralNames) typesafeGetter.invoke(extension);
        } catch (NoSuchMethodException e) { /* fall through */ }
        try {
            Method legacyGetter = SubjectAlternativeNameExtension.class.getMethod("get", String.class);
            return (GeneralNames) legacyGetter.invoke(extension, "subject_name");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unsupported SUN provider implementation", e);
        }
    }

}
