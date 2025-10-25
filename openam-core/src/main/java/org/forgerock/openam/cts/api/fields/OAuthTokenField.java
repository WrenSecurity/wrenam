/*
 * Copyright 2013-2016 ForgeRock AS.
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Portions copyright 2025 Wren Security
 */
package org.forgerock.openam.cts.api.fields;

import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.tokens.CoreTokenField;

/**
 * The OAuthTokenField provides a mapping between known OAuth fields and the LDAP Attributes
 * that they map to.
 * <p>
 * This class, like all other Token Field classes references the CoreTokenField fields. It goes
 * one stage further by mapping OAuth attribute names to the corresponding CoreTokenField attributes.
 * <p>
 * This class should <b>only</b> list mappings for token fields that should be queryable in the CTS.
 * Other token fields will be stored automatically in the CTS blob.
 */
public enum OAuthTokenField {
    /**
     * Fields used for the OAuth Bearer Token.
     */
    EXPIRY_TIME(OAuth2Constants.CoreTokenParams.EXPIRE_TIME, CoreTokenField.EXPIRY_DATE),
    SCOPE(OAuth2Constants.CoreTokenParams.SCOPE, CoreTokenField.STRING_ONE),
    PARENT(OAuth2Constants.CoreTokenParams.PARENT, CoreTokenField.STRING_TWO),
    USER_NAME(OAuth2Constants.CoreTokenParams.USERNAME, CoreTokenField.STRING_THREE),
    REDIRECT_URI(OAuth2Constants.CoreTokenParams.REDIRECT_URI, CoreTokenField.STRING_FOUR),
    REFRESH_TOKEN(OAuth2Constants.CoreTokenParams.REFRESH_TOKEN, CoreTokenField.STRING_FIVE),
    ISSUED(OAuth2Constants.CoreTokenParams.ISSUED, CoreTokenField.STRING_SIX),
    TYPE(OAuth2Constants.CoreTokenParams.TOKEN_TYPE, CoreTokenField.STRING_SEVEN),
    REALM(OAuth2Constants.CoreTokenParams.REALM, CoreTokenField.STRING_EIGHT),
    ID(OAuth2Constants.CoreTokenParams.ID, CoreTokenField.TOKEN_ID),
    CLIENT_ID(OAuth2Constants.CoreTokenParams.CLIENT_ID, CoreTokenField.STRING_NINE),
    TOKEN_NAME(OAuth2Constants.CoreTokenParams.TOKEN_NAME, CoreTokenField.STRING_TEN),
    NONCE(OAuth2Constants.Custom.NONCE, CoreTokenField.STRING_ELEVEN),
    GRANT_TYPE(OAuth2Constants.Params.GRANT_TYPE, CoreTokenField.STRING_TWELVE),
    SESSION_ID(OAuth2Constants.Custom.SSO_TOKEN_ID, CoreTokenField.STRING_THIRTEEN),
    DEVICE_USER_CODE(OAuth2Constants.DeviceCode.USER_CODE, CoreTokenField.STRING_FOURTEEN),
    AUTH_GRANT_ID(OAuth2Constants.CoreTokenParams.AUTH_GRANT_ID, CoreTokenField.STRING_FIFTEEN),
    AUTH_TIME(OAuth2Constants.CoreTokenParams.AUTH_TIME, CoreTokenField.DATE_THREE);

    private final String oAuthField;
    private final CoreTokenField coreTokenField;

    /**
     * @param oAuthField The OAuth field name.
     * @param coreTokenField The CoreTokenField to be mapped to.
     */
    private OAuthTokenField(String oAuthField, CoreTokenField coreTokenField) {
        this.oAuthField = oAuthField;
        this.coreTokenField = coreTokenField;
    }

    /**
     * @return The CoreTokenField that this OAuthTokenField is mapped to.
     */
    public CoreTokenField getField() {
        return coreTokenField;
    }

    /**
     * @return The OAuthField name.
     */
    public String getOAuthField() {
        return oAuthField;
    }

    /**
     * Provides a mechanism for getting the OAuthTokenField from a given OAuth field name.
     *
     * @param value The value to use when matching against the OAuthTokenField.
     *
     * @return Returns the OAuthTokenField for the named value.
     * @throws IllegalArgumentException If the string did not match any OAuthTokenField.
     */
    public static OAuthTokenField getField(String value) {
        for (OAuthTokenField field : values()) {
            if (field.getOAuthField().equals(value)) {
                return field;
            }
        }
        throw new IllegalArgumentException("Invalid OAuthTokenField value: " + value);
    }

    /**
     * @return Returns the CoreTokenField field toString value.
     */
    @Override
    public String toString() {
        return getField().toString();
    }
}
