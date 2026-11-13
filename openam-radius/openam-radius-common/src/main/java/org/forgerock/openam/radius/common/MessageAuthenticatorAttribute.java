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
 * Copyright 2026 Wren Security. All rights reserved.
 */
package org.forgerock.openam.radius.common;

import java.nio.ByteBuffer;

/**
 * Class representing the structure of the Message-Authenticator attribute as specified in section 3.2 of RFC 3579.
 * Its value is an HMAC-MD5 over the whole packet keyed with the shared secret. {@link PacketIntegrity} computes and
 * verifies the value.
 */
public class MessageAuthenticatorAttribute extends Attribute {

    /**
     * The length of the value of the attribute, the length of an HMAC-MD5.
     */
    public static final int VALUE_LENGTH = 16;

    /**
     * The length of the on-the-wire format of the attribute including the type and length octets.
     */
    public static final int LENGTH = VALUE_LENGTH + 2;

    /**
     * Creates an attribute with a zero value, the placeholder used while the HMAC is computed.
     */
    public MessageAuthenticatorAttribute() {
        super(placeholderOctets());
    }

    /**
     * Creates an attribute from the on-the-wire octets.
     *
     * @param octets
     *            the on-the-wire bytes.
     */
    public MessageAuthenticatorAttribute(byte[] octets) {
        super(octets);
    }

    /**
     * Sets the value of the attribute.
     *
     * @param value
     *            the 16 octet HMAC-MD5.
     */
    void setValue(byte[] value) {
        if (value.length != VALUE_LENGTH) {
            throw new IllegalArgumentException("Message-Authenticator value must have " + VALUE_LENGTH + " octets");
        }
        final byte[] octets = new byte[LENGTH];
        octets[0] = (byte) AttributeType.MESSAGE_AUTHENTICATOR.getTypeCode();
        octets[1] = (byte) LENGTH;
        System.arraycopy(value, 0, octets, 2, VALUE_LENGTH);
        setOctets(octets);
    }

    @Override
    public String toStringImpl() {
        return Utils.toSpacedHex(ByteBuffer.wrap(getOctets(), 2, getOctets().length - 2));
    }

    private static byte[] placeholderOctets() {
        final byte[] octets = new byte[LENGTH];
        octets[0] = (byte) AttributeType.MESSAGE_AUTHENTICATOR.getTypeCode();
        octets[1] = (byte) LENGTH;
        return octets;
    }
}
