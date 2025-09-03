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
 * $Id: AMCertPath.java,v 1.5 2009/07/16 00:02:24 beomsuk Exp $
 *
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 * Portions Copyrighted 2025 Wren Security
 */
package com.sun.identity.security.cert;

import com.sun.identity.security.SecurityDebug;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import org.forgerock.openam.utils.StringUtils;

/**
 * Class AMCertPath is special cased Certpath validation class.
 * It does cert path validation together with CRL check and ocsp checking
 * if they are properly configured.
 */
public class AMCertPath {

    private static CertificateFactory cf = null;
    private static CertPathValidator cpv = null;
    private CertStore store = null; //GuardedBy("AMCertPath.class")
    private static Debug debug = SecurityDebug.debug;
    private static final String OCSP_ENABLE = "ocsp.enable";
    private static final String OCSP_RESPONDER_URL = "ocsp.responderURL";
    private static final String TRUE = "true";
    private static final String FALSE = "false";

    static {
    	try {
    	    cf = SecurityProviderCompat.getCertificateFactory();
            cpv = SecurityProviderCompat.getPathValidator();
    	} catch (Exception e) {
    		debug.error("AMCertPath.Static:",e);
    	}
    }

    public AMCertPath(List<X509CRL> crls)
         throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {

        if (crls != null && !crls.isEmpty()) {
            if (debug.messageEnabled()) {
                X509CRL crl = crls.get(0);
                debug.message("AMCertPath:AMCertPath: crl =" + crl.toString());
            }

            CollectionCertStoreParameters collection = new CollectionCertStoreParameters(crls);
            synchronized(AMCertPath.class) {
                store = CertStore.getInstance("Collection", collection);
            }
        } else {
            if (debug.messageEnabled()) {
                debug.message("AMCertPath:AMCertPath: no crl");
            }
        }
    }

    /**
     * It does cert path validation together with CRL check and ocsp checking if they are properly configured.
     **/
    public boolean verify(X509Certificate[] certs, boolean crlEnabled, boolean ocspEnabled) {
        /*
        The entire contents of this method must be synchronized for the following reasons:
        1. The CertPathValidator#validate method is not thread-safe
        2. even if a non-static CertPathValidator instance were obtained in this method, each instance references
        the ocsp-related properties in the Security class. Thus the state set in Security.setProperty("ocsp.enable", true/false)
        will affect all CertPathValidator instances.
        Note that despite the synchronized block, the fact that static Security properties are being set and referenced
        exposes the code below to data races in the context of these Security properties. Currently, Security.setProperties
        is not being called from anywhere in the OpenAM code base. If this were to change, and the "ocsp.enable" property
        were manipulated, the OCSP-based checking below would be susceptible to data races. There does not seem to
        be an alternative however: the section on PKIXParameters here:
        http://docs.oracle.com/javase/6/docs/technotes/guides/security/certpath/CertPathProgGuide.html#Introduction
        mentions setting PKIXCertPathChecker implementations to do CRL or OCSP based checking, but there is no remove
        method, and the state returned from getCertPathCheckers is immutable.
         */
        synchronized(AMCertPath.class) {
            if (debug.messageEnabled()) {
                debug.message("AMCertPath.verify: invoked !");
            }
            try {
                final List<X509Certificate> certList = Arrays.asList(certs);
                final CertPath cp = cf.generateCertPath(certList);

                // init PKIXParameters
                Class<?> trustMgrClass = Class.forName("com.sun.identity.security.keystore.AMX509TrustManager");
                Object trustMgr = trustMgrClass.getDeclaredConstructor().newInstance();
                Method method = trustMgrClass.getMethod("getKeyStore");
                KeyStore keystore = (KeyStore) method.invoke(trustMgr);
                PKIXParameters pkixparams = new PKIXParameters(keystore);
                if (debug.messageEnabled()) {
                    debug.message("AMCertPath.verify: crlEnabled ---> " + crlEnabled);
                    debug.message("AMCertPath.verify: ocspEnabled ---> " + ocspEnabled);
                }

                pkixparams.setRevocationEnabled(crlEnabled || ocspEnabled);
                if (ocspEnabled) {
                    final String responderURLString = getResponderURLString();
                    if (!StringUtils.isBlank(responderURLString)) {
                        Security.setProperty(OCSP_ENABLE, TRUE);
                        Security.setProperty(OCSP_RESPONDER_URL, responderURLString);
                        if (debug.messageEnabled()) {
                            debug.message("AMCertPath.verify: pkixparams.setRevocationEnabled "
                                    + "set to true, and ocsp.enabled set to true with a OCSP responder url of " + responderURLString);
                        }
                    } else {
                        //OCSP revocation checking not configured properly. Disable the check if crl-based checking not enabled
                        pkixparams.setRevocationEnabled(crlEnabled);
                        Security.setProperty(OCSP_ENABLE, FALSE);
                        debug.error("AMCertPath.verify: OCSP is enabled, but the " +
                                "com.sun.identity.authentication.ocsp.responder.url property does not specify a OCSP " +
                                "responder. OCSP checking will NOT be performed.");
                    }
                } else {
                    //the Security properties are static - if we are doing crl validation, insure that the property
                    //is not present which will toggle OCSP checking.
                    Security.setProperty(OCSP_ENABLE, FALSE);
                    if (debug.messageEnabled()) {
                        debug.message("AMCertPath.verify: pkixparams Security property ocsp.enabled set to false.");
                    }
                }

                if (store != null) {
                    pkixparams.addCertStore(store);
                }
                if (debug.messageEnabled()) {
                    StringBuilder sb = new StringBuilder("The policy-related state in the PKIXParameters passed to the PKIX CertPathValidator: \n");
                    sb.append("\tgetInitialPolicies: ").append(pkixparams.getInitialPolicies()).append('\n');
                    sb.append("\tisExplicitPolicyRequired: ").append(pkixparams.isExplicitPolicyRequired()).append('\n');
                    sb.append("\tisPolicyMappingInhibited: ").append(pkixparams.isPolicyMappingInhibited()).append('\n');
                    debug.message(sb.toString());
                }
                // validate
                CertPathValidatorResult cpvResult = cpv.validate(cp, pkixparams);

                if (debug.messageEnabled()) {
                    debug.message("AMCertPath.verify: PASS " + cpvResult.toString());
                }
            } catch (java.security.cert.CertPathValidatorException e) {
                debug.error("AMCertPath.verify: FAILED - " + e.getMessage());
                if (debug.messageEnabled()) {
                    debug.message("AMCertPath.verify: FAILED", e);
                }
                return false;
            } catch (Throwable t) {
                debug.error("AMCertPath.verify: FAILED", t);
                return false;
            }
            return true;
        }
    }

    /*
     * returns <code>null</code> if no or invalid value is specified for
     * <code>com.sun.identity.authentication.ocsp.responder.url</code>
     */
    private String getResponderURLString() {
        final String responderURLString = SystemPropertiesManager.get(
                "com.sun.identity.authentication.ocsp.responder.url");
        if (responderURLString != null) {
            try {
                new URL(responderURLString);
            } catch (MalformedURLException urlEx) {
                debug.error("AMCertPath.getResponderURLString: Invalid ocsp responder url configured", urlEx);
                return null;
            }
        } else {
            if (debug.warningEnabled()) {
                debug.warning("AMCertPath.getResponderURLString: No ocsp responder url configured");
            }
        }
        return responderURLString;
    }


}
