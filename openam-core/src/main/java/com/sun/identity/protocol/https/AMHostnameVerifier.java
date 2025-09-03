/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
 * Portions copyright 2025 Wren Security
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
 * $Id: AMHostnameVerifier.java,v 1.2 2008/06/25 05:43:54 qcheng Exp $
 *
 */

package com.sun.identity.protocol.https;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.HashSet;
import java.util.StringTokenizer;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.debug.Debug;


public class AMHostnameVerifier implements HostnameVerifier {

    private static final Integer NAME_DNS = 2;

    public static boolean trustAllServerCerts = false;

    public static boolean checkSubjectAltName = false;

    public static boolean resolveIPAddress = false;

    public static HashSet<String> sslTrustHosts = new HashSet<>();

    static private Debug debug = Debug.getInstance("amJSSE");

    static {
        String tmp = SystemProperties.get("com.iplanet.am.jssproxy.trustAllServerCerts");
        trustAllServerCerts = (tmp != null && tmp.equalsIgnoreCase("true"));

        tmp = SystemProperties.get("com.iplanet.am.jssproxy.checkSubjectAltName");
        checkSubjectAltName = (tmp != null && tmp.equalsIgnoreCase("true"));

        tmp = SystemProperties.get("com.iplanet.am.jssproxy.resolveIPAddress");
        resolveIPAddress = (tmp != null && tmp.equalsIgnoreCase("true"));

        tmp = SystemProperties.get("com.iplanet.am.jssproxy.SSLTrustHostList", null);
        if (tmp != null) {
            getSSLTrustHosts(tmp);
        }

        if (debug.messageEnabled()) {
            debug.message("AMHostnameVerifier trustAllServerCerts = " + trustAllServerCerts);
            debug.message("AMHostnameVerifier checkSubjectAltName = " + checkSubjectAltName);
            debug.message("AMHostnameVerifier  resolveIPAddress = " + resolveIPAddress);
            debug.message("AMHostnameVerifier  SSLTrustHostList = " + sslTrustHosts.toString());
        }
    }

    @Override
    public boolean verify(String hostname, SSLSession session) {
    	if (trustAllServerCerts) {
            return true;
        }

        boolean approve = true;
        X509Certificate peercert =  null;
    	String cn = null;

    	try {
            Certificate[] peercerts = session.getPeerCertificates();
    	    peercert = (X509Certificate) peercerts[0];
            LdapName subjectDN = new LdapName(peercert.getSubjectDN().getName());
            Rdn subjectName = subjectDN.getRdn(subjectDN.size() - 1);
            if (subjectName.getType().equalsIgnoreCase("cn")) {
                cn = (String) subjectName.getValue();
            }
        } catch (Exception ex) {
            debug.error("AMHostnameVerifier:"+ex.toString());
        }

        if (cn == null) {
            return false;
        }

        if (!sslTrustHosts.isEmpty()) {
            if (sslTrustHosts.contains(cn.toLowerCase())) {
                return true;
            }
        }

        if (resolveIPAddress) {
            try {
                approve = InetAddress.getByName(cn).getHostAddress().equals(
                             InetAddress.getByName(hostname).getHostAddress());
            }
            catch (UnknownHostException ex) {
                if (debug.messageEnabled()) {
                    debug.message("AMHostnameVerifier:", ex);
                }
                approve = false;
            }
        } else {
            approve = false;
        }

        if (checkSubjectAltName && !approve) {
            try {
                for (List<?> name : peercert.getSubjectAlternativeNames()) {
                    if (NAME_DNS.equals(name.get(0)) && compareHosts((String) name.get(1), hostname)) {
                        approve = true;
                        break;
                    }
                }
            } catch (Exception ex) {
                return false;
            }
        }

        return approve;
    }

    private boolean compareHosts(String name, String hostname) {
        try {
	        name = name.substring(name.indexOf(':')+1).trim();
            return InetAddress.getByName(name).equals(
                    InetAddress.getByName(hostname));
        } catch (UnknownHostException e) {
    	    if (debug.messageEnabled()) {
    	        debug.message(e.toString());
    	    }
        }

        return false;
    }

    static private void getSSLTrustHosts(String hostlist) {
        if (debug.messageEnabled()) {
            debug.message("AMHostnameVerifier  SSLTrustHostList = " +
                                   hostlist);
        }
        StringTokenizer st = new StringTokenizer(hostlist, ",");
        sslTrustHosts.clear();
        while (st.hasMoreTokens()) {
            sslTrustHosts.add(st.nextToken().trim().toLowerCase());
        }
    }
}

