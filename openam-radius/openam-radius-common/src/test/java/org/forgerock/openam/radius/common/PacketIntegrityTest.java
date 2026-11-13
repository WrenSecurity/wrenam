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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.forgerock.openam.radius.common.PacketIntegrity.MessageAuthenticatorStatus;
import org.testng.annotations.Test;

/**
 * Tests for {@link PacketIntegrity}.
 */
public class PacketIntegrityTest {

    private static final String SECRET = Rfc2865Examples.secret;

    /** The Access-Request of example 7.1 of RFC 2865. */
    private static final byte[] REQUEST = Utils.toByteArray(Rfc2865Examples.Ex71.spacedHex);

    private static final byte[] REQUEST_AUTHENTICATOR = Arrays.copyOfRange(REQUEST, 4, 20);

    /** The Access-Accept of example 7.1 of RFC 2865, answering {@link #REQUEST}. */
    private static final byte[] ACCEPT = Utils.toByteArray("02 00 00 26 86 fe 22 0e 76 24 ba 2a 10 05 f6 bf "
            + "9b 55 e0 b2 06 06 00 00 00 01 0f 06 00 00 00 00 0e 06 c0 a8 01 03");

    /** {@link #REQUEST} with a Message-Authenticator appended, computed independently of this code. */
    private static final byte[] SIGNED_REQUEST = Utils.toByteArray("01 00 00 4a 0f 40 3f 94 73 97 80 57 bd 83 d5 cb "
            + "98 f4 22 7a 01 06 6e 65 6d 6f 02 12 0d be 70 8d 93 d4 13 ce 31 96 e4 3f 78 2a 0a ee 04 06 c0 a8 01 10 "
            + "05 06 00 00 00 03 50 12 63 b7 8a 6b 9d 2f 14 99 89 fb f5 7e a2 1d 19 4c");

    @Test
    public void shouldVerifyResponseAuthenticatorOfRfcExample() {
        assertThat(PacketIntegrity.verifyResponseAuthenticator(ACCEPT, REQUEST_AUTHENTICATOR, SECRET)).isTrue();
    }

    @Test
    public void shouldRejectResponseAuthenticatorWithWrongSecretOrModifiedPacket() {
        assertThat(PacketIntegrity.verifyResponseAuthenticator(ACCEPT, REQUEST_AUTHENTICATOR, "other")).isFalse();
        assertThat(PacketIntegrity.verifyResponseAuthenticator(ACCEPT, REQUEST_AUTHENTICATOR, "")).isFalse();
        assertThat(PacketIntegrity.verifyResponseAuthenticator(ACCEPT, REQUEST_AUTHENTICATOR, null)).isFalse();

        final byte[] modified = ACCEPT.clone();
        modified[0] = 3; // Access-Accept turned into Access-Reject
        assertThat(PacketIntegrity.verifyResponseAuthenticator(modified, REQUEST_AUTHENTICATOR, SECRET)).isFalse();

        final byte[] otherRequest = REQUEST_AUTHENTICATOR.clone();
        otherRequest[0] ^= 1;
        assertThat(PacketIntegrity.verifyResponseAuthenticator(ACCEPT, otherRequest, SECRET)).isFalse();
    }

    @Test
    public void shouldVerifyIndependentlyComputedMessageAuthenticator() {
        assertThat(PacketIntegrity.checkMessageAuthenticator(SIGNED_REQUEST, null, SECRET))
                .isEqualTo(MessageAuthenticatorStatus.VALID);
        assertThat(PacketIntegrity.checkMessageAuthenticator(SIGNED_REQUEST, null, "other"))
                .isEqualTo(MessageAuthenticatorStatus.INVALID);
        assertThat(PacketIntegrity.checkMessageAuthenticator(SIGNED_REQUEST, null, null))
                .isEqualTo(MessageAuthenticatorStatus.INVALID);
    }

    @Test
    public void shouldDetectModifiedOrMissingMessageAuthenticator() {
        final byte[] modified = SIGNED_REQUEST.clone();
        modified[22] ^= 1; // a byte of the User-Name
        assertThat(PacketIntegrity.checkMessageAuthenticator(modified, null, SECRET))
                .isEqualTo(MessageAuthenticatorStatus.INVALID);
        assertThat(PacketIntegrity.checkMessageAuthenticator(REQUEST, null, SECRET))
                .isEqualTo(MessageAuthenticatorStatus.ABSENT);
        assertThat(PacketIntegrity.checkMessageAuthenticator(REQUEST, null, null))
                .isEqualTo(MessageAuthenticatorStatus.ABSENT);
    }

    @Test
    public void shouldRejectRepeatedOrMalformedMessageAuthenticator() {
        final byte[] repeated = append(SIGNED_REQUEST, Arrays.copyOfRange(SIGNED_REQUEST, 56, 74));
        assertThat(PacketIntegrity.checkMessageAuthenticator(repeated, null, SECRET))
                .isEqualTo(MessageAuthenticatorStatus.INVALID);

        final byte[] shortAttribute = append(REQUEST, new byte[] {80, 4, 0, 0});
        assertThat(PacketIntegrity.checkMessageAuthenticator(shortAttribute, null, SECRET))
                .isEqualTo(MessageAuthenticatorStatus.INVALID);
    }

    @Test
    public void shouldSignRequestWithMessageAuthenticatorFirst() {
        final AccessRequest request = (AccessRequest) PacketFactory.toPacket(REQUEST);

        PacketIntegrity.signRequest(request, SECRET);
        PacketIntegrity.signRequest(request, SECRET); // signing again replaces the attribute

        final byte[] octets = request.getOctets();
        assertThat(request.getAttributeAt(0)).isInstanceOf(MessageAuthenticatorAttribute.class);
        assertThat(octets).hasSize(REQUEST.length + MessageAuthenticatorAttribute.LENGTH);
        assertThat(Arrays.copyOfRange(octets, 4, 20)).isEqualTo(REQUEST_AUTHENTICATOR);
        assertThat(PacketIntegrity.checkMessageAuthenticator(octets, null, SECRET))
                .isEqualTo(MessageAuthenticatorStatus.VALID);
    }

    @Test
    public void shouldSignResponse() {
        final AccessAccept accept = new AccessAccept();
        accept.setIdentifier((short) 7);
        accept.addAttribute(new ReplyMessageAttribute("welcome"));

        PacketIntegrity.signResponse(accept, new RequestAuthenticator(REQUEST_AUTHENTICATOR), SECRET);

        final byte[] octets = accept.getOctets();
        assertThat(accept.getAttributeAt(0)).isInstanceOf(MessageAuthenticatorAttribute.class);
        assertThat(accept.getAttributeAt(1)).isInstanceOf(ReplyMessageAttribute.class);
        assertThat(PacketIntegrity.verifyResponseAuthenticator(octets, REQUEST_AUTHENTICATOR, SECRET)).isTrue();
        assertThat(PacketIntegrity.checkMessageAuthenticator(octets, REQUEST_AUTHENTICATOR, SECRET))
                .isEqualTo(MessageAuthenticatorStatus.VALID);
        assertThat(PacketIntegrity.checkMessageAuthenticator(octets, null, SECRET))
                .isEqualTo(MessageAuthenticatorStatus.INVALID);
    }

    @Test
    public void shouldRefuseToSignWithoutSecret() {
        final AccessRequest request = (AccessRequest) PacketFactory.toPacket(REQUEST);
        assertThatThrownBy(() -> PacketIntegrity.signRequest(request, ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldExtractPacketAndDropPadding() {
        final byte[] padded = Arrays.copyOf(ACCEPT, ACCEPT.length + 3); // octets beyond the length field
        assertThat(PacketIntegrity.extractPacket(padded, padded.length)).isEqualTo(ACCEPT);
        assertThat(PacketIntegrity.extractPacket(new byte[4096], ACCEPT.length)).isNull();
    }

    @Test
    public void shouldRejectMalformedPackets() {
        assertThat(PacketIntegrity.extractPacket(ACCEPT, 19)).isNull();
        assertThat(PacketIntegrity.extractPacket(ACCEPT, ACCEPT.length - 1)).isNull();
        assertThat(PacketIntegrity.extractPacket(ACCEPT, ACCEPT.length + 1)).isNull();

        final byte[] zeroLengthAttribute = append(REQUEST, new byte[] {1, 0});
        assertThat(PacketIntegrity.extractPacket(zeroLengthAttribute, zeroLengthAttribute.length)).isNull();

        final byte[] overlongAttribute = append(REQUEST, new byte[] {1, 9, 0});
        assertThat(PacketIntegrity.extractPacket(overlongAttribute, overlongAttribute.length)).isNull();
    }

    @Test
    public void shouldNotLoopOnZeroLengthAttribute() {
        final byte[] zeroLengthAttribute = append(REQUEST, new byte[] {1, 0});
        assertThatThrownBy(() -> PacketFactory.toPacket(ByteBuffer.wrap(zeroLengthAttribute)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Appends octets to a packet and updates its length field.
     */
    private static byte[] append(byte[] packet, byte[] octets) {
        final byte[] result = Arrays.copyOf(packet, packet.length + octets.length);
        System.arraycopy(octets, 0, result, packet.length, octets.length);
        result[2] = (byte) (result.length >> 8);
        result[3] = (byte) result.length;
        return result;
    }
}
