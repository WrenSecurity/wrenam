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
 * $Id: OfflineResolver.java,v 1.2 2008/06/25 05:47:38 qcheng Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 * Portions Copyrighted 2023 Wren Security
 */

package com.sun.identity.saml.xmlsig;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.w3c.dom.Attr;
import org.apache.xml.security.utils.resolver.ResourceResolverContext;
import org.apache.xml.security.utils.resolver.ResourceResolverException;
import org.apache.xml.security.signature.XMLSignatureInput;
import org.apache.xml.security.utils.resolver.ResourceResolverSpi;

/**
 * This class helps us home users to resolve http URIs without a network
 * connection
 *
 * @author $Author: qcheng $
 */
public class OfflineResolver extends ResourceResolverSpi {

    @Override
    public XMLSignatureInput engineResolveURI(ResourceResolverContext context) throws ResourceResolverException {
        return engineResolve(context.attr, context.baseUri);
    }

    @Override
    public boolean engineCanResolveURI(ResourceResolverContext context) {
        return engineCanResolve(context.attr, context.baseUri);
    }

   /**
    * Method engineResolve
    *
    * @param uri
    * @param BaseURI
    * @throws ResourceResolverException
    */
   public XMLSignatureInput engineResolve(Attr uri, String BaseURI) throws ResourceResolverException {

      try {
         String URI = uri.getNodeValue();

         String newURI = (String) this._uriMap.get(URI);
         if (newURI != null) {

            InputStream is = new FileInputStream(newURI);


            XMLSignatureInput result = new XMLSignatureInput(is);

            // XMLSignatureInput result = new XMLSignatureInput(inputStream);
            result.setSourceURI(URI);
            result.setMIMEType((String) this._mimeMap.get(URI));

            return result;
         } else {
            Object exArgs[] = {
               "The URI " + URI + " is not configured for offline work" };

            throw new ResourceResolverException("generic.EmptyMessage", exArgs, uri.getNodeValue(), BaseURI);
         }
      } catch (IOException ex) {
         throw new ResourceResolverException(ex, uri.getNodeValue(), BaseURI, "generic.EmptyMessage");
      }
   }

   /**
    * We resolve http URIs <I>without</I> fragment.
    *
    * @param uri
    * @param BaseURI
    */
   public boolean engineCanResolve(Attr uri, String BaseURI) {

      String uriNodeValue = uri.getNodeValue();

      if (uriNodeValue.length() == 0 || uriNodeValue.startsWith("#")) {
         return false;
      }

      try {
         URI uriNew = getNewURI(uri.getNodeValue(), BaseURI);
         if (uriNew.getScheme().equals("http")) {
            return true;
         }
      } catch (URISyntaxException ex) {}

      return false;
   }

   /** Field _uriMap */
   static Map _uriMap = null;

   /** Field _mimeMap */
   static Map _mimeMap = null;

   /**
    * Method register
    *
    * @param URI
    * @param filename
    * @param MIME
    */
   private static void register(String URI, String filename, String MIME) {
      OfflineResolver._uriMap.put(URI, filename);
      OfflineResolver._mimeMap.put(URI, MIME);
   }

   private static URI getNewURI(String uri, String baseURI) throws URISyntaxException {
        URI newUri = null;
        if (baseURI == null || "".equals(baseURI)) {
            newUri = new URI(uri);
        } else {
            newUri = new URI(baseURI).resolve(uri);
        }

        // if the URI contains a fragment, ignore it
        if (newUri.getFragment() != null) {
            URI uriNewNoFrag =
                new URI(newUri.getScheme(), newUri.getSchemeSpecificPart(), null);
            return uriNewNoFrag;
        }
        return newUri;
   }

   static {
      org.apache.xml.security.Init.init();

      OfflineResolver._uriMap = new HashMap<String, String>();
      OfflineResolver._mimeMap = new HashMap<String, String>();

      OfflineResolver.register("http://www.w3.org/TR/xml-stylesheet",
                               "data/org/w3c/www/TR/xml-stylesheet.html",
                               "text/html");
      OfflineResolver.register("http://www.w3.org/TR/2000/REC-xml-20001006",
                               "data/org/w3c/www/TR/2000/REC-xml-20001006",
                               "text/xml");
      OfflineResolver.register("http://www.nue.et-inf.uni-siegen.de/index.html",
                               "data/org/apache/xml/security/temp/nuehomepage",
                               "text/html");
      OfflineResolver.register(
         "http://www.nue.et-inf.uni-siegen.de/~geuer-pollmann/id2.xml",
         "data/org/apache/xml/security/temp/id2.xml", "text/xml");
      OfflineResolver.register(
         "http://xmldsig.pothole.com/xml-stylesheet.txt",
         "data/com/pothole/xmldsig/xml-stylesheet.txt", "text/xml");
   }

}
