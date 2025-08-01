/*
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
 * $Id: FSNameRegistrationResponse.java,v 1.3 2008/06/25 05:46:45 qcheng Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 * Portions Copyrighted 2023 Wren Security
 */
package com.sun.identity.federation.message;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.common.SAMLVersionMismatchException;
import com.sun.identity.saml.protocol.AbstractResponse;
import com.sun.identity.saml.protocol.Status;
import com.sun.identity.saml.protocol.StatusCode;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.xml.XMLUtils;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class has methods to create <code>NameRegistrationResponse</code>
 * object.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated

public class FSNameRegistrationResponse extends AbstractResponse {
    private String providerId = null;
    private String relayState = "";
    private Status status = null;
    protected String xmlString        = null;
    protected String signatureString        = null;
    protected String id = null;
    protected int minorVersion = 0;

    /**
     * Default Constructor.
     */
    public FSNameRegistrationResponse() {
        try {
            setIssueInstant(newDate());
            StatusCode statusCode = new StatusCode(IFSConstants.SAML_SUCCESS);
            status = new Status(statusCode);
        } catch(Exception e){
            FSUtils.debug.error("FSNameRegistrationResponse.Constructor", e);
        }
    }

    /**
     * Returns the value of <code>RelayState</code> attribute.
     *
     * @return the value of <code>RelayState</code> attribute.
     * @see #setRelayState(String)
     */
    public String getRelayState(){
        return relayState;
    }

    /**
     * Set the value of <code>RelayState</code> attribute.
     *
     * @param relayState the value of <code>RelayState</code> attribute.
     * @see #getRelayState()
     */
    public void setRelayState(String relayState){
        this.relayState = relayState;
    }

    /**
     * Returns the value of <code>id</code> attribute.
     *
     * @return the value of <code>id</code> attribute.
     * @see #setID(String)
     */

    public String getID(){
        return id;
    }

    /**
     * Sets the value of <code>id</code> attribute.
     *
     * @param id the value of <code>id</code> attribute.
     * @see #getID()
     */
    public void setID(String id){
        this.id = id;
    }

    /**
     * Returns the value of <code>ProviderID</code> attribute.
     *
     * @return the value of <code>ProviderID</code> attribute.
     * @see #setProviderId(String).
     */
    public String getProviderId(){
        return providerId;
    }

    /**
     * Sets the value of providerID attribute.
     *
     * @param providerId the value of providerID attribute.
     * @see #getProviderId()
     */
    public void setProviderId(String providerId){
        this.providerId = providerId;
    }

    /**
     * Returns signed <code>XML</code> representation of this
     * object.
     *
     * @return xmlString signed <code>XML</code> representation of this
     *         object.
     */
    public String getSignedXMLString(){
        return xmlString;
    }

    /**
     * Returns the signed <code>NameRegistrationResponse</code> string.
     *
     * @return signatureString the signed <code>NameRegistrationResponse</code>
     *         string.
     */
    public String getSignatureString(){
        return signatureString;
    }

    /**
     * Constructor creates the <code>FSNameRegistrationResponse</code> object.
     *
     * @param responseID the value of <code>ResponseID</code> attribute.
     * @param inResponseTo the value of <code>InResponseTo</code> attribute.
     * @param status the <code>Status</code> object.
     * @param providerId the value of <code>ProviderID</code> attribute.
     * @param relayState the value of <code>RelayState</code> attribute.
     * @throws FSMsgException if there is an error creating this object.
     */
    public FSNameRegistrationResponse(String responseID, String inResponseTo,
            Status status, String providerId,
            String relayState) throws FSMsgException {
        if ((responseID == null) || (responseID.length() == 0)) {
            this.responseID = FSUtils.generateID();
            if (this.responseID == null) {
                throw new FSMsgException("errorGenerateID",null);
            }
        } else {
            this.responseID = responseID;
        }
        if (inResponseTo == null) {
            FSUtils.debug.message("Response: inResponseTo is null.");
            throw new FSMsgException("nullInput",null);
        }
        this.inResponseTo = inResponseTo;
        if (status == null) {
            FSUtils.debug.message("Response: missing <Status>.");
            throw new FSMsgException("missingElement",null);
        }
        this.status = status;
        this.providerId = providerId;
        this.relayState = relayState;
        setIssueInstant(newDate());
    }

    /**
     * Constructor creates the <code>FSNameRegistrationResponse</code> object
     * from Document Element.
     *
     * @param root the Document Element objec.t
     * @throws FSMsgException if there is an error creating this object.
     * @throws SAMLException if there is an error creating this object.
     */
    public FSNameRegistrationResponse(Element root) throws
            FSMsgException, SAMLException {
        if (root == null) {
            FSUtils.debug.message("FSNameRegistrationResponse.parseXML:" +
                    " null input.");
            throw new FSMsgException("nullInput",null);
        }
        String tag = null;
        if (((tag = root.getLocalName()) == null) ||
                (!tag.equals("RegisterNameIdentifierResponse"))) {
            FSUtils.debug.error("FSNameRegistrationResponse.parseXML: wrong"+
                    " input.");
            throw new FSMsgException("wrongInput",null);
        }

        id = root.getAttribute("id");

        // Attribute ResponseID
        responseID = root.getAttribute("ResponseID");
        if ((responseID == null) || (responseID.length() == 0)) {
            FSUtils.debug.error("FSNameRegistrationResponse.parseXML: "
                    + "Reponse doesn't have ResponseID.");
            String[] args = { IFSConstants.RESPONSE_ID };
            throw new FSMsgException("missingAttribute",args);
        }

        parseMajorVersion(root.getAttribute("MajorVersion"));
        parseMinorVersion(root.getAttribute("MinorVersion"));

        // Attribute InResponseTo
        inResponseTo = root.getAttribute("InResponseTo");
        if (inResponseTo == null) {
            FSUtils.debug.error("FSNameRegistrationResponse.parseXML: "
                    + "Response doesn't have InResponseTo.");
            String[] args = { IFSConstants.IN_RESPONSE_TO };
            throw new FSMsgException("missingAttribute",args);
        }
        // Attribute IssueInstant
        String instantString = root.getAttribute("IssueInstant");
        if ((instantString == null) || (instantString.length() == 0)) {
            FSUtils.debug.error("FSNameRegistrationResponse(Element): " +
                    "missing IssueInstant");
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new FSMsgException("missingAttribute",args);
        } else {
            try {
                issueInstant = DateUtils.stringToDate(instantString);
            } catch (ParseException e) {
                FSUtils.debug.error(
                        "FSNameRegistrationResponse(Element):" +
                        " could not parse IssueInstant:" , e);
                throw new FSMsgException("wrongInput",null);
            }
        }

        NodeList nl = root.getChildNodes();
        Node child;
        String childName;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if ((childName = child.getLocalName()) != null) {
                if (childName.equals("Status")) {
                    if (status != null) {
                        FSUtils.debug.error("FSNameRegistrationResponse: " +
                                "included more than one <Status>");
                        throw new FSMsgException("moreElement",null);
                    }
                    status = new Status((Element) child);
                } else if (childName.equals(IFSConstants.SIGNATURE)) {
                } else if (childName.equals("ProviderID")) {
                    if (providerId != null) {
                        FSUtils.debug.error("FSNameRegistrationResponse:" +
                                " included more than one providerId");
                        throw new FSMsgException("moreElement",null);
                    }
                    providerId = XMLUtils.getElementValue((Element) child);
                } else if (childName.equals("RelayState")) {
                    relayState = XMLUtils.getElementValue((Element) child);
                }else {
                    FSUtils.debug.error("FSNameRegistrationResponse: " +
                            "included wrong element:" + childName);
                    throw new FSMsgException("wrongInput",null);
                }
            } // end if childName != null
        } // end for loop

        if (status == null) {
            FSUtils.debug.message(
                    "FSNameRegistrationResponse: missing element <Status>.");
            throw new FSMsgException("oneElement",null);
        }

        if (providerId == null) {
            FSUtils.debug.message(
                    "FSNameRegistrationResponse: missing element providerId.");
            throw new FSMsgException("oneElement",null);
        }

        //check for signature
        List signs = XMLUtils.getElementsByTagNameNS1(root,
                SAMLConstants.XMLSIG_NAMESPACE_URI,
                SAMLConstants.XMLSIG_ELEMENT_NAME);
        int signsSize = signs.size();
        if (signsSize == 1) {
            Element elem = (Element)signs.get(0);
            setSignature(elem);
            xmlString = XMLUtils.print(root);
            signed = true;
        } else if (signsSize != 0) {
            FSUtils.debug.error("FSNameRegistrationResponse(Element): " +
                    "included more than one Signature element.");
            throw new FSMsgException("moreElement",null);
        }
    }

    /**
     * Returns the <code>MinorVersion</code>.
     *
     * @return the <code>MinorVersion</code>.
     * @see #setMinorVersion(int)
     */
    public int getMinorVersion() {
        return minorVersion;
    }

    /**
     * Sets the <code>MinorVersion</code>.
     *
     * @param version the <code>MinorVersion</code>.
     * @see #getMinorVersion()
     */

    public void setMinorVersion(int version) {
        minorVersion = version;
    }

    /**
     * Returns the Response <code>Status</code>.
     *
     * @return the Response <code>Status</code>.
     * @see #setStatus(Status)
     */
    public Status getStatus() {
        return status;
    }


    /**
     * Sets the Response <code>Status</code>.
     *
     * @param status the Response <code>Status</code object.
     * @see #getStatus
     */
    public void setStatus(Status status) {
        this.status=status;
    }

    /**
     * Sets the <code>MajorVersion</code> by parsing the version string.
     *
     * @param majorVer a String representing the <code>MajorVersion</code> to
     *        be set.
     * @throws FSMsgException on error.
     * @throws SAMLException when the version mismatchs.
     */
    private void parseMajorVersion(
            String majorVer
            ) throws FSMsgException, SAMLException {
        try {
            majorVersion = Integer.parseInt(majorVer);
        } catch (NumberFormatException e) {
            FSUtils.debug.error("Response(Element): invalid MajorVersion", e);
            throw new FSMsgException("wrongInput",null);
        }

        if (majorVersion != SAMLConstants.PROTOCOL_MAJOR_VERSION) {
            if (majorVersion > SAMLConstants.PROTOCOL_MAJOR_VERSION) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Response(Element):MajorVersion of"
                            + " the Response is too high.");
                }
                throw new SAMLVersionMismatchException(FSUtils.BUNDLE_NAME,
                        "responseVersionTooHigh",null);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Response(Element):MajorVersion of"
                            + " the Response is too low.");
                }
                throw new SAMLVersionMismatchException(FSUtils.BUNDLE_NAME,
                        "responseVersionTooLow",null);
            }
        }
    }

    /**
     * Sets the <code>MinorVersion</code> by parsing the version string.
     *
     * @param minorVer a String representing the <code>MinorVersion</code> to
     *        be set.
     * @throws SAMLException when the version mismatchs.
     */
    private void parseMinorVersion(String minorVer) throws FSMsgException {
        try {
            minorVersion = Integer.parseInt(minorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                        "FSRegisResp(Element): "
                        + "invalid MinorVersion", e);
            }
            throw new FSMsgException("wrongInput",null);
        }

        if (minorVersion > IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
            FSUtils.debug.error("FSRegisResp(Element):MinorVersion of"
                    + " the Response is too high.");
            throw new FSMsgException("responseVersionTooHigh",null);
        } else if (minorVersion < IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
            FSUtils.debug.error("FSRegisResp(Element):MinorVersion of"
                    + " the Response is too low.");
            throw new FSMsgException("responseVersionTooLow",null);
        }

    }

    /**
     * Returns the <code>FSNameRegistrationResponse</code> object.
     *
     * @param xml the XML string to be parsed.
     * @return <code>FSNameRegistrationResponsee</code> object created from
     *         the XML string.
     * @throws FSMsgException if there is error creating the object.
     */
    public static FSNameRegistrationResponse parseXML(String xml)
    throws FSMsgException {
        try{
            Document doc = XMLUtils.toDOMDocument(xml, FSUtils.debug);
            Element root = doc.getDocumentElement();
            return new FSNameRegistrationResponse(root);
        }catch(SAMLException ex){
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSNameRegistrationResponse.parseXML: "
                        + "Error while parsing input xml string");
            }
            throw new FSMsgException("parseError", null, ex);
        }

    }

    /**
     * Returns the string representation of this object.
     * This method translates the response to an XML string.
     *
     * @return An XML String representing the Response.
     * @throws FSMsgException on error.
     */
    public String toXMLString()  throws FSMsgException {
        return this.toXMLString(true, true);
    }

    /**
     * Returns a String representation of the Logout Response.
     *
     * @param includeNS : Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS : Determines whether or not the namespace is declared
     *        within the Element.
     * @return a string containing the valid XML for this element
     * @throws FSMsgException if there is an error converting
     *         this object to a string.
     */
    public String toXMLString(boolean includeNS, boolean declareNS)
    throws FSMsgException {
        return toXMLString(includeNS, declareNS, false);
    }

    /**
     * Returns a String representation of the Logout Response.
     *
     * @param includeNS Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @param includeHeader Determines whether the output include the xml
     *        declaration header.
     * @return a string containing the valid XML for this element
     * @throws FSMsgException if there is an error converting
     *         this object to a string.
     */
    public String toXMLString(boolean includeNS, boolean declareNS,
            boolean includeHeader) throws FSMsgException {
        StringBuffer xml = new StringBuffer(300);
        if (includeHeader) {
            xml.append("<?xml version=\"1.0\" encoding=\"").
                    append(SAMLConstants.DEFAULT_ENCODING).append("\" ?>\n");
        }
        String prefixLIB = "";
        String uriLIB = "";
        if (includeNS) {
            prefixLIB = IFSConstants.LIB_PREFIX;
        }

        if (declareNS) {
            if (minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                uriLIB = IFSConstants.LIB_12_NAMESPACE_STRING;
            } else {
                uriLIB = IFSConstants.LIB_NAMESPACE_STRING;
            }
        }

        String instantString = DateUtils.toUTCDateFormat(issueInstant);

        if ((providerId == null) || (providerId.length() == 0)){
            FSUtils.debug.error("FSNameRegistrationResponse.toXMLString: "
                    + "providerId is null in the response with responseId:"
                    + responseID);
            throw new FSMsgException("nullProviderID",null);
        }

        if ((responseID != null) && (inResponseTo != null)){
            xml.append("<").append(prefixLIB).
                    append("RegisterNameIdentifierResponse").append(uriLIB);
            if (minorVersion == IFSConstants.FF_11_PROTOCOL_MINOR_VERSION &&
                    id != null && !(id.length() == 0)) {
                xml.append(" id=\"").append(id).append("\" ");
            }
            xml.append(" ResponseID=\"").append(responseID).append("\" ").
                    append(" InResponseTo=\"").append(XMLUtils.escapeSpecialCharacters(inResponseTo)).
                    append("\" ").
                    append(" MajorVersion=\"").append(majorVersion).
                    append("\" ").
                    append(" MinorVersion=\"").append(minorVersion).
                    append("\" ").
                    append(" IssueInstant=\"").append(instantString).
                    append("\" ").
                    append(">");
        }

        if (signed) {
            if (signatureString != null) {
                xml.append(signatureString);
            } else if (signature != null) {
                signatureString = XMLUtils.print(signature);
                xml.append(signatureString);
            }
        }

        if (providerId != null) {
            xml.append("<").append(prefixLIB).append("ProviderID").append(">").
                    append(providerId).
                    append("</").append(prefixLIB).append("ProviderID").
                    append(">");
        }

        if (status != null) {
            xml.append(status.toString(includeNS, true));
        }

        if (relayState != null) {
            xml.append("<").append(prefixLIB).append("RelayState").
                    append(">").append(relayState).
                    append("</").append(prefixLIB).
                    append("RelayState").append(">");
        }

        xml.append("</").append(prefixLIB).
                append("RegisterNameIdentifierResponse>");
        return xml.toString();
    }

    /**
     * Returns <code>FSNameRegistrationResponse</code> object. The object
     * is created by parsing an Base64 encode Name Registration Response
     * string.
     *
     * @param encodedRes the encoded response string
     * @throws FSMsgException if there is an error
     *         creating this object.
     * @throws SAMLException if there is an error
     *         creating this object.
     */
    public static FSNameRegistrationResponse parseBASE64EncodedString(
            String encodedRes) throws FSMsgException, SAMLException {
        if (encodedRes != null){
            String decodedNameRegRes = new String(Base64.decode(encodedRes));
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSNameRegistrationResponse."
                        + "parseBASE64EncodedString: decoded input string: "
                        + decodedNameRegRes);
            }
            return parseXML(decodedNameRegRes);
        } else{
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                        "FSNameRegistrationResponse.parseBASE64EncodedString"
                        + ": null String passed in as argument.");
            }
            throw new FSMsgException("nullInput",null);
        }
    }

    /**
     * Returns a Base64 Encoded String.
     *
     * @return a Base64 Encoded String.
     * @throws FSMsgException if there is an error encoding
     *         the string.
     */
    public String toBASE64EncodedString() throws FSMsgException {
        if ((responseID == null) || (responseID.length() == 0)){
            responseID = FSUtils.generateID();
            if (responseID == null) {
                FSUtils.debug.error(
                        "FSNameRegistrationResponse.toBASE64EncodedString: "
                        + "couldn't generate ResponseID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
        return Base64.encode(this.toXMLString().getBytes());
    }

    /**
     * Signs the Name Registration Response.
     *
     * @param certAlias the Certificate Alias.
     * @throws SAMLException if this object cannot be signed.
     */
    public void signXML(String certAlias) throws SAMLException {
        FSUtils.debug.message("FSNameRegistrationResponse.signXML: Called");
        if (signed) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSNameRegistrationResponse.signXML: "
                        + "the assertion is already signed.");
            }
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                    "alreadySigned",null);
        }
        if (certAlias == null || certAlias.length() == 0) {
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                    "cannotFindCertAlias",null);
        }
        try{
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            if (minorVersion == IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
                signatureString = manager.signXML(this.toXMLString(true, true),
                        certAlias, IFSConstants.DEF_SIG_ALGO, IFSConstants.ID,
                        this.id, false);
            } else if (minorVersion ==
                    IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                signatureString = manager.signXML(this.toXMLString(true, true),
                        certAlias, IFSConstants.DEF_SIG_ALGO,
                        IFSConstants.RESPONSE_ID,
                        this.getResponseID(), false);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("invalid minor version.");
                }
            }
            signature =
                    XMLUtils.toDOMDocument(signatureString, FSUtils.debug)
                    .getDocumentElement();

            signed = true;
            xmlString = this.toXMLString(true, true);
        }catch(Exception e){
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                                             "signError",null);
        }
    }

    /**
     * Unsupported operation.
     */
    public void signXML() throws SAMLException {
        throw new SAMLException(FSUtils.BUNDLE_NAME,
                               "unsupportedOperation",null);
    }

    /**
     * Sets the Signature.
     *
     * @param elem the Document Element.
     * @return true if success otherwise false.
     */
    public boolean setSignature(Element elem) {
        signatureString = XMLUtils.print(elem);
        return super.setSignature(elem);
    }

    /**
     * Returns an URL Encoded String.
     *
     * @return a url encoded query string.
     * @throws FSMsgException if there is an error.
     */
    public String toURLEncodedQueryString() throws FSMsgException {
        if ((providerId == null) || (providerId.length() == 0)){
            FSUtils.debug.error("FSNameRegistrationResponse."
                    + "toURLEncodedQueryString: providerId is null in "
                    + "the response ");
            throw new FSMsgException("nullProviderIdInRequest",null);
        }
        if ((responseID == null) || (responseID.length() == 0)){
            responseID = FSUtils.generateID();
            if (responseID == null) {
                FSUtils.debug.error("FSNameRegistrationRequest."
                        + "toURLEncodedQueryString: couldn't generate "
                        + "responseID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
        StringBuffer urlEncodedAuthnReq = new StringBuffer(300);
        urlEncodedAuthnReq.append("ResponseID=").
                append(URLEncDec.encode(responseID)).
                append(IFSConstants.AMPERSAND);
        urlEncodedAuthnReq.append("MajorVersion=").
                append(majorVersion).append(IFSConstants.AMPERSAND);
        urlEncodedAuthnReq.append("MinorVersion=").
                append(minorVersion).append(IFSConstants.AMPERSAND);
        urlEncodedAuthnReq.append("InResponseTo=").
                append(URLEncDec.encode(inResponseTo)).
                append(IFSConstants.AMPERSAND);

        if (issueInstant != null){
            urlEncodedAuthnReq.append("IssueInstant=")
            .append(URLEncDec.encode(
                    DateUtils.toUTCDateFormat(issueInstant)))
                    .append(IFSConstants.AMPERSAND);
        } else {
            FSUtils.debug.error("FSNameRegistrationRequest."
                    + "toURLEncodedQueryString: issueInstant missing");
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new FSMsgException("missingAttribute",args);
        }
        if (providerId != null && !providerId.equals("")) {
            urlEncodedAuthnReq.append("ProviderID=").
                    append(URLEncDec.encode(providerId)).
                    append(IFSConstants.AMPERSAND);
        }

        if (relayState != null && relayState.length() > 0) {
            urlEncodedAuthnReq.append("RelayState=").
                    append(URLEncDec.encode(relayState)).
                    append(IFSConstants.AMPERSAND);
        }

        if (status != null) {
            urlEncodedAuthnReq.append("Value=");
            urlEncodedAuthnReq.append(
                    URLEncDec.encode(status.getStatusCode().getValue())).
                    append(IFSConstants.AMPERSAND);
        }

        return urlEncodedAuthnReq.toString();
    }


    /**
     * Returns <code>FSNameRegistrationLogoutResponse</code> object. The
     * object is creating by parsing the <code>HttpServletRequest</code>
     * object.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @throws FSMsgException if there is an error
     *         creating this object.
     */
    public static FSNameRegistrationResponse parseURLEncodedRequest(
            HttpServletRequest request) throws FSMsgException, SAMLException {
        FSNameRegistrationResponse retNameRegistrationResponse =
                new FSNameRegistrationResponse();
        try {
            FSUtils.debug.message("checking minor version");
            retNameRegistrationResponse.majorVersion =
                    Integer.parseInt(request.getParameter("MajorVersion"));
            retNameRegistrationResponse.minorVersion =
                    Integer.parseInt(request.getParameter("MinorVersion"));
        } catch(NumberFormatException ex){
            FSUtils.debug.error("FSNameRegistrationResponse.parseURL" +
                    "EncodedRequest: version parsing error:" + ex);
            throw new FSMsgException("invalidNumber",null);
        }

        if (request.getParameter("ResponseID")!= null) {
            retNameRegistrationResponse.responseID =
                    request.getParameter("ResponseID");
        } else {
            FSUtils.debug.error("FSNameRegistrationResponse.parseURL" +
                    "EncodedRequest: Response ID is null" );
            String[] args = { IFSConstants.RESPONSE_ID };
            throw new FSMsgException("missingAttribute",args);
        }

        String instantString = request.getParameter("IssueInstant");
        if (instantString == null || instantString.length() == 0) {
            FSUtils.debug.error("FSNameRegistrationResponse.parseURL" +
                    "EncodedRequest: Issue Instant is null" );
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new FSMsgException("missingAttribute",args);
        }
        try{
            retNameRegistrationResponse.issueInstant =
                    DateUtils.stringToDate(instantString);
        } catch (ParseException e){
            FSUtils.debug.error("FSNameRegistrationResponse.parseURL" +
                    "EncodedRequest: Can not parse Issue Instant", e);
            throw new FSMsgException("parseError",null);
        }
        if (request.getParameter("ProviderID")!= null){
            retNameRegistrationResponse.providerId =
                    request.getParameter("ProviderID");
        } else {
            FSUtils.debug.error("FSNameRegistrationResponse.parseURL" +
                    "EncodedRequest: Provider ID is null " );
            throw new FSMsgException("missingElement",null);
        }

        if (request.getParameter("RelayState")!= null){
            retNameRegistrationResponse.relayState =
                    request.getParameter("RelayState");
        }
        if (request.getParameter("InResponseTo")!= null){
            retNameRegistrationResponse.inResponseTo =
                    request.getParameter("InResponseTo");
        }

        if (request.getParameter("Value") != null){
            FSUtils.debug.message("Status : " + request.getParameter("Value"));
            StatusCode statusCode =
                    new StatusCode(request.getParameter("Value"));
            retNameRegistrationResponse.status = new Status(statusCode);
        } else {
            FSUtils.debug.error("FSNameRegistrationResponse.parseURL" +
                    "EncodedRequest: Status Value is  null " );
            throw new FSMsgException("missingElement",null);
        }

        FSUtils.debug.message("Returning registration response Object");
        return retNameRegistrationResponse;
    }
}
