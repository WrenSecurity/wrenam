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
 * $Id: AMCRLStore.java,v 1.7 2009/01/28 05:35:12 ww203982 Exp $
 *
 * Portions Copyrighted 2013-2016 ForgeRock AS.
 * Portions Copyrighted 2025 Wren Security
 */
package com.sun.identity.security.cert;

import static org.forgerock.openam.utils.Time.newDate;

import com.forgerock.opendj.ldap.controls.TransactionIdControl;
import com.iplanet.security.x509.CertUtils;
import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.shared.encode.URLEncDec;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Hashtable;
import java.util.StringTokenizer;
import javax.security.auth.x500.X500Principal;
import org.apache.commons.lang.ArrayUtils;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.IssuingDistributionPoint;
import org.forgerock.i18n.LocalizedIllegalArgumentException;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LDAPUrl;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.ModificationType;
import org.forgerock.opendj.ldap.SSLContextBuilder;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.forgerock.util.Options;

/**
 * The class is used to manage crl store in LDAP server
 * This class does get crl and update crl with CRLDistribution
 * PointsExtension in client certificate or IssuingDistribution
 * PointExtension in CRL. This class should be used
 * in order to manage CRL store in LDAP
 * <pre>
 * id-ce-cRLDistributionPoints OBJECT IDENTIFIER ::=  { id-ce 31 }
 *
 * RLDistributionPoints ::= SEQUENCE SIZE (1..MAX) OF DistributionPoint
 *
 * DistributionPoint ::= SEQUENCE {
 *        distributionPoint       [0]     DistributionPointName OPTIONAL,
 *        reasons                 [1]     ReasonFlags OPTIONAL,
 *        cRLIssuer               [2]     GeneralNames OPTIONAL }
 *
 * DistributionPointName ::= CHOICE {
 *        fullName                [0]     GeneralNames,
 *        nameRelativeToCRLIssuer [1]     RelativeDistinguishedName }
 *
 * ReasonFlags ::= BIT STRING {
 *        unused                  (0),
 *        keyCompromise           (1),
 *        cACompromise            (2),
 *        affiliationChanged      (3),
 *        superseded              (4),
 *        cessationOfOperation    (5),
 *        certificateHold         (6),
 *        privilegeWithdrawn      (7),
 *        aACompromise            (8) }
 * </pre>
 */

public class AMCRLStore extends AMCertStore {

    public static final String CERTIFICATE_REVOCATION_LIST = "certificaterevocationlist";
    public static final String CERTIFICATE_REVOCATION_LIST_BINARY = CERTIFICATE_REVOCATION_LIST + ";binary";
    // In memory CRL cache
    private static Hashtable<String, X509CRL> cachedcrls = new Hashtable<String, X509CRL>();

    private String mCrlAttrName = null;

    /**
     * Class AMCRLStore is special cased CRL store for LDAP.
     * A AMCRLStore instance has to have all the information for ldap
     * and all the access information for CRLDistributionPointExtension and
     * CRLIssuingDistributionPoint Extension
     *
     * @param param
     */
    public AMCRLStore(AMLDAPCertStoreParameters param) {
        super(param);
    }

    /**
     * Checks certificate and returns corresponding stored CRL in ldap store
     *
     * @param certificate
     */
    public X509CRL getCRL(X509Certificate certificate) throws IOException {
        SearchResultEntry crlEntry = null;
        X509CRL crl = null;

        if (storeParam.isDoCRLCaching()) {
            if (debug.messageEnabled()) {
                debug.message("AMCRLStore.getCRL: Trying to get CRL from cache");
            }
            crl = getCRLFromCache(certificate);
        }

        try (Connection ldc = getConnection()) {
            if (ldc == null) {
                return null;
            }
            if (crl == null) {
                if (debug.messageEnabled()) {
                    debug.message("AMCRLStore.getCRL: crl is null");
                }
                if (mCrlAttrName == null) {
                    crlEntry = getLdapEntry(ldc, CERTIFICATE_REVOCATION_LIST, CERTIFICATE_REVOCATION_LIST_BINARY);
                } else {
                    crlEntry = getLdapEntry(ldc, mCrlAttrName);
                }
                crl = getCRLFromEntry(crlEntry);
            }

            if (storeParam.isDoUpdateCRLs() && needCRLUpdate(crl)) {
                if (debug.messageEnabled()) {
                    debug.message("AMCRLStore.getCRL: need CRL update");
                }

                X509CRL tmpCrl = null;

                IssuingDistributionPoint crlIDPExt = null;
                try {
                    if (crl != null) {
                        crlIDPExt = SecurityProviderCompat.getIssuingDistributionPoint(crl);
                    }
                } catch (Exception e) {
                    debug.message("AMCRLStore.getCRL: crlIDPExt is null");
                }

                CRLDistPoint crlDPExt = null;
                try {
                    crlDPExt = SecurityProviderCompat.getCrlDistributionPoints(certificate);
                } catch (Exception e) {
                    debug.message("AMCRLStore.getCRL: crlDPExt is null");
                }

                if (tmpCrl == null && crlIDPExt != null) {
                    tmpCrl = getUpdateCRLFromCrlIDP(crlIDPExt);
                }

                if (tmpCrl == null && crlDPExt != null) {
                    tmpCrl = getUpdateCRLFromCrlDP(crlDPExt);
                }

                if (tmpCrl != null) {
                    if (crlEntry == null) {
                        crlEntry = getLdapEntry(ldc);
                    }

                    if (debug.messageEnabled()) {
                        debug.message("AMCRLStore.getCRL: new crl = " + tmpCrl);
                    }

                    if (crlEntry != null) {
                        updateCRL(ldc, crlEntry.getName().toString(), tmpCrl.getEncoded());
                    }
                }
                crl = tmpCrl;
            }

            if (storeParam.isDoCRLCaching()) {
                if (debug.messageEnabled()) {
                    debug.message("AMCRLStore.getCRL: Updating CRL cache");
                }
                updateCRLCache(certificate, crl);
            }

        } catch (Exception e) {
            debug.error("AMCRLStore.getCRL: Error in getting CRL : ", e);
        }

        return crl;
    }

    /**
     * Checks certificate and returns corresponding stored CRL in cached CRL store.
     *
     * @param certificate
     * @return Cached CRL information about the certificate.
     */
    public X509CRL getCRLFromCache(X509Certificate certificate) {
        return cachedcrls.get(CertUtils.getIssuerName(certificate));
    }

    /**
     * Checks certificate and update CRL in cached CRL store.
     */
    public void updateCRLCache(X509Certificate certificate, X509CRL crl) {
        String issuer = CertUtils.getIssuerName(certificate);

        if (crl == null) {
            cachedcrls.remove(issuer);
        } else {
            cachedcrls.put(issuer, crl);
        }
    }

    private X509CRL getCRLFromEntry(SearchResultEntry entry) throws Exception {
        if (debug.messageEnabled()) {
            debug.message("AMCRLStore.getCRLFromEntry:");
        }

        if (entry == null) {
            return null;
        }

        Attribute crlAttribute = null;
        X509CRL crl = null;

        try {
            // Retrieve the certificate revocation list if available.
            if (mCrlAttrName == null) {
                crlAttribute = entry.getAttribute(CERTIFICATE_REVOCATION_LIST);
                if (crlAttribute == null) {
                    crlAttribute = entry.getAttribute(CERTIFICATE_REVOCATION_LIST_BINARY);
                    if (crlAttribute == null) {
                        debug.error("No CRL Cache is configured");
                        return null;
                    }
                }
                mCrlAttrName = crlAttribute.getAttributeDescriptionAsString();
            } else {
                crlAttribute = entry.getAttribute(mCrlAttrName);
            }

            if (crlAttribute.size() > 1) {
                debug.error("More than one CRL entries are configured");
                return null;
            }
        } catch (Exception e) {
            debug.error("Error in getting Cached CRL");
            return null;
        }

        try {
            byte[] bytes = crlAttribute.firstValue().toByteArray();
            if (debug.messageEnabled()) {
                debug.message("AMCRLStore.getCRLFromEntry: crl size = " + bytes.length);
            }
            crl = (X509CRL) AMCertStore.cf.generateCRL(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            debug.error("Certificate: CertRevoked = ", e);
        }
        return crl;
    }

    /**
     * It updates CRL under the dn in the directory server.
     * It retrieves CRL distribution points from the parameter
     * CRLDistributionPointsExtension dpExt.
     *
     * @param dpExt
     */
    private synchronized X509CRL getUpdateCRLFromCrlDP(CRLDistPoint dpExt) {
        if (dpExt == null) {
            return null;
        }
        DistributionPoint[] dps = dpExt.getDistributionPoints();
        if (dps == null || dps.length == 0) {
            return null;
        }
        for (DistributionPoint dp : dps) {
            GeneralNames gName = (GeneralNames) dp.getDistributionPoint().getName();
            if (debug.messageEnabled()) {
                debug.message("AMCRLStore.getUpdateCRLFromCrlDP: DP = " + gName);
            }
            byte[] crls = getCRLsFromGeneralNamesBC(gName);
            if (crls != null && crls.length > 0) {
                try {
                    return (X509CRL) cf.generateCRL(new ByteArrayInputStream(crls));
                } catch (Exception ex) {
                    if (debug.warningEnabled()) {
                        debug.warning("AMCRLStore.getUpdateCRLFromCrlDP: Error in generating X509CRL", ex);
                    }
                }
            }
        }
        return null;
    }

    /**
     * It updates CRL under the dn in the directory server. It retrieves CRL distribution points from the parameter
     * CRLDistributionPointsExtension dpExt.
     */
    private synchronized X509CRL getUpdateCRLFromCrlIDP(IssuingDistributionPoint idpExt) {
        GeneralNames generalNames = null;
        try {
            if (idpExt != null && idpExt.getDistributionPoint() != null) {
                generalNames = (GeneralNames) idpExt.getDistributionPoint().getName();
            }
        } catch (Exception e) {
            debug.error("Error getting distribution point name", e);
            return null;
        }
        if (generalNames == null) {
            return null;
        }
        if (debug.messageEnabled()) {
            debug.message("AMCRLStore.getUpdateCRLFromCrlIDP: gName = " + generalNames);
        }
        byte[] Crls = getCRLsFromGeneralNamesBC(generalNames);
        X509CRL crl = null;
        if (Crls != null) {
            try {
                crl = (X509CRL) cf.generateCRL(new ByteArrayInputStream(Crls));
            } catch (Exception e) {
                debug.error("Error in generating X509CRL" + e.toString());
            }
        }
        return crl;
    }

    // Bouncy Castle version of getCRLsFromGeneralNames
    private byte[] getCRLsFromGeneralNamesBC(GeneralNames generalNames) {
        byte[] crls = null;
        if (debug.messageEnabled()) {
            debug.message("AMCRLStore.getCRLsFromGeneralNamesBC: gNames.size = " + generalNames.getNames().length);
        }
        int idx = 0;
        GeneralName[] names = generalNames.getNames();
        do {
            String uri = names[idx++].getName().toString().trim();
            String protocol = uri.toLowerCase();
            int proto_pos;
            if ((proto_pos = protocol.indexOf("http")) == -1) {
                if ((proto_pos = protocol.indexOf("https")) == -1) {
                    if ((proto_pos = protocol.indexOf("ldap")) == -1) {
                        if ((proto_pos = protocol.indexOf("ldaps")) == -1) {
                            continue;
                        }
                    }
                }
            }
            uri = uri.substring(proto_pos);
            if (debug.messageEnabled()) {
                debug.message("DP Name : " + uri);
            }
            crls = getCRLByURI(uri);
        } while ((crls != null) && (idx < names.length));
        return crls;
    }

    /**
     * It replaces attribute value under the DN.
     * It is used to replace old CRL with new one.
     *
     * @param ldc
     * @param dn
     * @param crls
     */
    private void updateCRL(Connection ldc, String dn, byte[] crls) {
        try {
            ModifyRequest modifyRequest = LDAPRequests.newModifyRequest(dn)
                    .addModification(ModificationType.REPLACE, mCrlAttrName, crls);
            ldc.modify(modifyRequest);
        } catch (LdapException e) {
            debug.error("Error updating CRL Cache : ", e);
        }
    }

    /**
     * It is checking uri's protocol.
     * Protocol has to be http(s) or ldap.
     * Based on checked protocol, it gets new CRL by invking
     * getCRLByLdapURI() or getCRLByHttpURI()
     *
     * @param uri
     */
    private byte[] getCRLByURI(String uri) {
        if (debug.messageEnabled()) {
            debug.message("AMCRLStore.getCRLByURI : uri = " + uri);
        }
        if (uri == null) {
            return null;
        }

        String protocol = uri.trim().toLowerCase();
        if (protocol.startsWith("http") || protocol.startsWith("https")) {
            return getCRLByHttpURI(uri);
        } else if (protocol.startsWith("ldap") || protocol.startsWith("ldaps")) {
            return getCRLByLdapURI(uri);
        }
        return null;
    }

    /**
     * It gets the new CRL from ldap server.
     * If it is ldap URI, the URI has to be a dn that can be accessed
     * with ldap anonymous bind.
     * (example : ldap://server:port/uid=ca,o=company.com)
     * This dn entry has to have CRL in attribute certificaterevocationlist
     * or certificaterevocationlist;binary.
     *
     * @param uri
     */
    private byte[] getCRLByLdapURI(String uri) {
        if (debug.messageEnabled()) {
            debug.message("AMCRLStore.getCRLByLdapURI: uri = " + uri);
        }

        LDAPUrl url;
        byte[] crl = null;
        LDAPConnectionFactory factory = null;
        try {
            url = LDAPUrl.valueOf(uri);
        } catch (LocalizedIllegalArgumentException e) {
            debug.error("AMCRLStore.getCRLByLdapURI(): Could not parse uri: {}", uri, e);
            return null;
        }
        debug.message("AMCRLStore.getCRLByLdapURI: url.dn = {}", url.getName());

        // Check ldap over SSL
        try {
            if (url.isSecure()) {
                factory = new LDAPConnectionFactory(url.getHost(), url.getPort(), Options.defaultOptions()
                        .set(LDAPConnectionFactory.SSL_CONTEXT, new SSLContextBuilder().getSSLContext()));
            } else { // non-ssl
                factory = new LDAPConnectionFactory(url.getHost(), url.getPort());
            }

            try (Connection ldc = factory.getConnection()) {
                ConnectionEntryReader results = ldc.search(url.asSearchRequest()
                        .addControl(TransactionIdControl.newControl(
                                AuditRequestContext.createSubTransactionIdValue())));

                if (!results.hasNext()) {
                    debug.error("verifyCertificate - No CRL distribution Point configured");
                    return null;
                }

                if (results.isReference()) {
                    debug.warning("Getting CRL but got LDAP reference: {}", results.readReference());
                    return null;
                }

                SearchResultEntry entry = results.readEntry();

                /*
                 * Retrieve the certificate revocation list if available.
                 */
                Attribute crlAttribute = entry.getAttribute(CERTIFICATE_REVOCATION_LIST);
                if (crlAttribute == null) {
                    crlAttribute = entry.getAttribute(CERTIFICATE_REVOCATION_LIST_BINARY);
                    if (crlAttribute == null) {
                        debug.error("verifyCertificate - No CRL distribution Point configured");
                        return null;
                    }
                }

                crl = crlAttribute.firstValue().toByteArray();
            }
        } catch (Exception e) {
            debug.error("getCRLByLdapURI : Error in getting CRL", e);
        }
        return crl;
    }

    private byte[] getCRLByHttpURI(String url) {
        StringBuffer params = null;
        HttpURLConnection con = null;
        byte[] crl = null;

        String uriParamsCRL = storeParam.getURIParams();

        try {

            if (uriParamsCRL != null) {
                params = new StringBuffer();
                StringTokenizer st1 = new StringTokenizer(uriParamsCRL, ",");
                while (st1.hasMoreTokens()) {
                    String token = st1.nextToken();
                    StringTokenizer st2 = new StringTokenizer(token, "=");
                    if (st2.countTokens() == 2) {
                        String param = st2.nextToken();
                        String value = st2.nextToken();
                        params.append(URLEncDec.encode(param) + "=" + URLEncDec.encode(value));
                    } else {
                        continue;
                    }

                    if (st1.hasMoreTokens()) {
                        params.append("&");
                    }
                }
            }

            URL uri = new URL(url);
            con = HttpURLConnectionManager.getConnection(uri);

            // Prepare for both input and output
            con.setDoInput(true);

            // Turn off Caching
            con.setUseCaches(false);

            if (params != null) {
                byte[] paramsBytes = params.toString().trim().getBytes("UTF-8");
                if (paramsBytes.length > 0) {
                    con.setDoOutput(true);
                    con.setRequestProperty("Content-Length",
                            Integer.toString(paramsBytes.length));

                    // Write the arguments as post data
                    BufferedOutputStream out =
                            new BufferedOutputStream(con.getOutputStream());
                    out.write(paramsBytes, 0, paramsBytes.length);
                    out.flush();
                    out.close();
                }
            }
            // Input ...
            InputStream in = con.getInputStream();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            int len;
            byte[] buf = new byte[1024];
            while ((len = in.read(buf, 0, buf.length)) != -1) {
                bos.write(buf, 0, len);
            }
            crl = bos.toByteArray();

            if (debug.messageEnabled()) {
                debug.message("AMCRLStore.getCRLByHttpURI: crl.length = " +
                        crl.length);
            }
        } catch (Exception e) {
            debug.error("getCRLByHttpURI : Error in getting CRL", e);
        }

        return crl;
    }

    // It returns NextCRLUpdate for current cached CRL
    // It gets CRL from crlAttribue member variable
    private boolean needCRLUpdate(X509CRL crl) {
        Date nextCRLUpdate = null;
        if (crl == null) {
            return true;
        }
        // Check CRLNextUpdate in CRL
        nextCRLUpdate = crl.getNextUpdate();
        if (debug.messageEnabled()) {
            debug.message("AMCRLStore.needCRLUpdate: nextCrlUpdate = " +
                    nextCRLUpdate);
        }

        return ((nextCRLUpdate != null) && nextCRLUpdate.before(newDate()));
    }

    /**
     * It gets the new CRL from ldap server.
     * If it is ldap URI, the URI has to be a dn that can be accessed
     * with ldap anonymous bind.
     * (example : ldap://server:port/uid=ca,o=company.com)
     * This dn entry has to have CRL in attribute certificaterevocationlist
     * or certificaterevocationlist;binary.
     * <p/>
     * if attrNames does only contain one value the ldap search filter will be
     * (attrName=Value_of_the_corresponding_Attribute_from_SubjectDN)
     * e.g. SubjectDN of issuer cert 'C=BE, CN=Citizen CA, serialNumber=201007'
     * attrNames is 'CN', search filter used will be (CN=Citizen CA)
     * <p/>
     * if attrNames does contain serveral values the ldap search filter value will be
     * a comma separated list of name attribute values, the search attribute will be 'cn'
     * (cn="attrNames[0]=Value_of_the_corresponding_Attribute_from_SubjectDN,
     * attrNames[1]=Value_of_the_corresponding_Attribute_from_SubjectDN")
     * <p/>
     * e.g. SubjectDN of issuer cert 'C=BE, CN=Citizen CA, serialNumber=201007'
     * attrNames is {"CN","serialNumber"}, search filter used will be
     * (cn=CN=Citizen CA,serialNumber=201007)
     * <p/>
     * The order of the values of attrNames matter as they must match the value of the
     * 'cn' attribute of a crlDistributionPoint entry in the directory server
     *
     * @param ldapParam
     * @param cert
     * @param attrNames, attributes names from the subjectDN of the issuer cert
     */
    public static X509CRL getCRL(AMLDAPCertStoreParameters ldapParam, X509Certificate cert, String... attrNames) {
        X509CRL crl = null;

        try {
            if (!ArrayUtils.isEmpty(attrNames)) {
                X500Principal issuerPrincipal = cert.getIssuerX500Principal();
                String searchFilter;


                if (attrNames.length < 2) {
                    /*
                     * Get the CN of the input certificate
                     */
                    String attrValue = null;

                    // Retrieve attribute value of the attribute name
                    attrValue = CertUtils.getAttributeValue(issuerPrincipal, attrNames[0]);

                    if (null == attrValue) {
                        return crl;
                    }

                    searchFilter = setSearchFilter(attrNames[0], attrValue);


                } else {
                    String searchFilterValue = buildSearchFilterValue(attrNames, issuerPrincipal);

                    if (searchFilterValue.isEmpty()) {
                        return crl;
                    }
                    searchFilter = setSearchFilter("cn", searchFilterValue);
                }

                if (debug.messageEnabled()) {
                    debug.message("AMCRLStore:getCRL using searchFilter " + searchFilter);
                }

                /*
                 * Lookup the certificate in the LDAP certificate directory
                 */

                ldapParam.setSearchFilter(searchFilter);

                AMCRLStore store = new AMCRLStore(ldapParam);
                crl = store.getCRL(cert);
            }

        } catch (Exception e) {
            debug.error("AMCRLStore:getCRL ", e);
        }

        return crl;
    }

    private static String buildSearchFilterValue(String[] attrNames, X500Principal principal) {
        StringBuilder searchFilterBuilder = new StringBuilder();
        for (int i = 0; i < attrNames.length; i++) {
            String attrName = attrNames[i];
            String attrValue = CertUtils.getAttributeValue(principal, attrName);

            if (null != attrValue) {
                searchFilterBuilder.append(attrName);
                searchFilterBuilder.append("=");
                searchFilterBuilder.append(attrValue);
                if (i < attrNames.length - 1) {
                    searchFilterBuilder.append(",");
                }
            }
        }
        return searchFilterBuilder.toString();
    }

}
