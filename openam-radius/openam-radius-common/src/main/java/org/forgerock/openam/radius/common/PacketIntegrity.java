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

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Computes and verifies the integrity protection of RADIUS packets: the Response Authenticator of section 3 of RFC 2865
 * and the Message-Authenticator attribute of section 3.2 of RFC 3579.
 * <p>
 * Verification works on the received octets rather than on parsed packets, so that a packet can be checked before it is
 * parsed and so that the octets covered by the check are exactly those that were received.
 */
public final class PacketIntegrity {

    /**
     * The length of the code, identifier, length and authenticator fields that precede the attributes.
     */
    public static final int HEADER_LENGTH = 20;

    /**
     * The maximum length of a RADIUS packet as specified in section 3 of RFC 2865.
     */
    public static final int MAX_PACKET_LENGTH = 4096;

    private static final int AUTHENTICATOR_OFFSET = 4;

    private static final int AUTHENTICATOR_LENGTH = 16;

    /**
     * The result of checking the Message-Authenticator attribute of a packet.
     */
    public enum MessageAuthenticatorStatus {
        /** The packet does not contain a Message-Authenticator attribute. */
        ABSENT,
        /** The packet contains a single Message-Authenticator attribute with a correct value. */
        VALID,
        /** The Message-Authenticator attribute is wrong, malformed or repeated, or the packet is malformed. */
        INVALID
    }

    private PacketIntegrity() {
    }

    /**
     * Extracts the octets of a packet from a received datagram, checking the length field as described in section 3 of
     * RFC 2865: octets beyond the length field are padding and are dropped, a packet shorter than its length field is
     * invalid.
     *
     * @param data
     *            the buffer holding the received datagram.
     * @param receivedLength
     *            the number of octets received.
     * @return the octets of the packet, or {@code null} if the packet is malformed and must be silently discarded.
     */
    public static byte[] extractPacket(byte[] data, int receivedLength) {
        if (receivedLength < HEADER_LENGTH || receivedLength > data.length) {
            return null;
        }
        final int packetLength = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        if (packetLength < HEADER_LENGTH || packetLength > MAX_PACKET_LENGTH || packetLength > receivedLength) {
            return null;
        }
        final byte[] packet = Arrays.copyOf(data, packetLength);
        return hasValidAttributeStructure(packet) ? packet : null;
    }

    /**
     * Adds a Message-Authenticator attribute as the first attribute of a request and computes its value. Any
     * Message-Authenticator attribute already present is replaced. The request authenticator and all other attributes
     * must be set before calling this method.
     *
     * @param request
     *            the request packet.
     * @param secret
     *            the secret shared between the client and the server.
     */
    public static void signRequest(Packet request, String secret) {
        final MessageAuthenticatorAttribute attribute = insertMessageAuthenticator(request);
        attribute.setValue(hmacMd5(request.getOctets(), secret));
    }

    /**
     * Signs a response to a request. Adds a Message-Authenticator attribute as the first attribute and computes its
     * value as described in section 3.2 of RFC 3579, then computes the Response Authenticator as described in
     * section 3 of RFC 2865, which covers the Message-Authenticator attribute. Any Message-Authenticator attribute
     * already present is replaced. All other attributes and the identifier must be set before calling this method.
     *
     * @param response
     *            the response packet.
     * @param requestAuthenticator
     *            the authenticator of the request being answered.
     * @param secret
     *            the secret shared between the client and the server.
     */
    public static void signResponse(Packet response, Authenticator requestAuthenticator, String secret) {
        response.setAuthenticator(new RequestAuthenticator(requestAuthenticator.getOctets().clone()));
        final MessageAuthenticatorAttribute attribute = insertMessageAuthenticator(response);
        attribute.setValue(hmacMd5(response.getOctets(), secret));
        final byte[] responseAuthenticator = md5(response.getOctets(), secret);
        response.setAuthenticator(new ResponseAuthenticator(responseAuthenticator));
    }

    /**
     * Verifies the Response Authenticator of a received response as described in section 3 of RFC 2865.
     *
     * @param packet
     *            the octets of the response, as returned by {@link #extractPacket(byte[], int)}.
     * @param requestAuthenticator
     *            the authenticator of the request that the response answers.
     * @param secret
     *            the secret shared between the client and the server.
     * @return {@code true} if the Response Authenticator is correct.
     */
    public static boolean verifyResponseAuthenticator(byte[] packet, byte[] requestAuthenticator, String secret) {
        if (packet.length < HEADER_LENGTH || isEmpty(secret)) {
            return false;
        }
        final byte[] received = Arrays.copyOfRange(packet, AUTHENTICATOR_OFFSET,
                AUTHENTICATOR_OFFSET + AUTHENTICATOR_LENGTH);
        final byte[] copy = packet.clone();
        System.arraycopy(requestAuthenticator, 0, copy, AUTHENTICATOR_OFFSET, AUTHENTICATOR_LENGTH);
        return MessageDigest.isEqual(received, md5(copy, secret));
    }

    /**
     * Checks the Message-Authenticator attribute of a received packet as described in section 3.2 of RFC 3579.
     *
     * @param packet
     *            the octets of the packet, as returned by {@link #extractPacket(byte[], int)}.
     * @param requestAuthenticator
     *            for a response, the authenticator of the request that the response answers; for a request,
     *            {@code null}.
     * @param secret
     *            the secret shared between the client and the server.
     * @return whether the packet has a correct Message-Authenticator attribute, an incorrect one or none.
     */
    public static MessageAuthenticatorStatus checkMessageAuthenticator(byte[] packet, byte[] requestAuthenticator,
            String secret) {
        if (packet.length < HEADER_LENGTH || !hasValidAttributeStructure(packet)) {
            return MessageAuthenticatorStatus.INVALID;
        }
        int valueOffset = -1;
        int offset = HEADER_LENGTH;
        while (offset < packet.length) {
            final int type = packet[offset] & 0xFF;
            final int length = packet[offset + 1] & 0xFF;
            if (type == AttributeType.MESSAGE_AUTHENTICATOR.getTypeCode()) {
                if (valueOffset != -1 || length != MessageAuthenticatorAttribute.LENGTH) {
                    return MessageAuthenticatorStatus.INVALID;
                }
                valueOffset = offset + 2;
            }
            offset += length;
        }
        if (valueOffset == -1) {
            return MessageAuthenticatorStatus.ABSENT;
        }
        if (isEmpty(secret)) {
            return MessageAuthenticatorStatus.INVALID;
        }
        final byte[] received = Arrays.copyOfRange(packet, valueOffset,
                valueOffset + MessageAuthenticatorAttribute.VALUE_LENGTH);
        final byte[] copy = packet.clone();
        Arrays.fill(copy, valueOffset, valueOffset + MessageAuthenticatorAttribute.VALUE_LENGTH, (byte) 0);
        if (requestAuthenticator != null) {
            System.arraycopy(requestAuthenticator, 0, copy, AUTHENTICATOR_OFFSET, AUTHENTICATOR_LENGTH);
        }
        return MessageDigest.isEqual(received, hmacMd5(copy, secret))
                ? MessageAuthenticatorStatus.VALID
                : MessageAuthenticatorStatus.INVALID;
    }

    /**
     * Checks that the attributes of a packet fill it exactly, each with a length of at least two octets.
     */
    private static boolean hasValidAttributeStructure(byte[] packet) {
        int offset = HEADER_LENGTH;
        while (offset < packet.length) {
            if (offset + 2 > packet.length) {
                return false;
            }
            final int length = packet[offset + 1] & 0xFF;
            if (length < 2 || offset + length > packet.length) {
                return false;
            }
            offset += length;
        }
        return true;
    }

    private static MessageAuthenticatorAttribute insertMessageAuthenticator(Packet packet) {
        final MessageAuthenticatorAttribute attribute = new MessageAuthenticatorAttribute();
        packet.getAttributeSet().removeAttributesByType(AttributeType.MESSAGE_AUTHENTICATOR);
        packet.getAttributeSet().insertAttributeAt(attribute, 0);
        return attribute;
    }

    private static byte[] hmacMd5(byte[] data, String secret) {
        requireSecret(secret);
        try {
            final Mac mac = Mac.getInstance("HmacMD5");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacMD5"));
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacMD5 is not available", e);
        }
    }

    private static byte[] md5(byte[] data, String secret) {
        requireSecret(secret);
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(data);
            md5.update(secret.getBytes(StandardCharsets.UTF_8));
            return md5.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is not available", e);
        }
    }

    private static void requireSecret(String secret) {
        if (isEmpty(secret)) {
            throw new IllegalArgumentException("The RADIUS shared secret must not be empty");
        }
    }

    private static boolean isEmpty(String secret) {
        return secret == null || secret.isEmpty();
    }
}
