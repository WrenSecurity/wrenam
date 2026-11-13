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
package org.forgerock.openam.radius.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.forgerock.openam.radius.common.AccessAccept;
import org.forgerock.openam.radius.common.AttributeType;
import org.forgerock.openam.radius.common.PacketIntegrity;
import org.forgerock.openam.radius.common.PacketIntegrity.MessageAuthenticatorStatus;
import org.forgerock.openam.radius.common.PacketType;
import org.forgerock.openam.radius.common.ReplyMessageAttribute;
import org.forgerock.openam.radius.common.RequestAuthenticator;
import org.forgerock.openam.radius.server.config.ClientConfig;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

/**
 * Tests for {@link RadiusRequestContext}.
 */
public class RadiusRequestContextTest {

    private static final String SECRET = "s3cret";

    private static final byte[] REQUEST_AUTHENTICATOR = {
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16
    };

    @Test
    public void shouldSignResponseWithMessageAuthenticatorFirst() throws Exception {
        final DatagramChannel channel = mock(DatagramChannel.class);
        final InetSocketAddress source = new InetSocketAddress("127.0.0.1", 40000);
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.setName("test");
        clientConfig.setSecret(SECRET);
        final RadiusRequestContext context = new RadiusRequestContext(clientConfig, channel, source);
        context.setRequestId((short) 42);
        context.setRequestAuthenticator(new RequestAuthenticator(REQUEST_AUTHENTICATOR));

        final AccessAccept accept = new AccessAccept();
        accept.addAttribute(new ReplyMessageAttribute("welcome"));
        context.send(accept);

        final ArgumentCaptor<ByteBuffer> sent = ArgumentCaptor.forClass(ByteBuffer.class);
        verify(channel).send(sent.capture(), eq(source));
        final ByteBuffer buffer = sent.getValue();
        final byte[] received = new byte[buffer.remaining()];
        buffer.get(received);
        final byte[] octets = PacketIntegrity.extractPacket(received, received.length);

        assertThat(octets).isNotNull();
        assertThat(octets[1] & 0xFF).isEqualTo(42);
        assertThat(octets[20] & 0xFF).isEqualTo(AttributeType.MESSAGE_AUTHENTICATOR.getTypeCode());
        assertThat(PacketIntegrity.verifyResponseAuthenticator(octets, REQUEST_AUTHENTICATOR, SECRET)).isTrue();
        assertThat(PacketIntegrity.checkMessageAuthenticator(octets, REQUEST_AUTHENTICATOR, SECRET))
                .isEqualTo(MessageAuthenticatorStatus.VALID);
    }

    @Test
    public void shouldNotSendWithoutRequestAuthenticator() throws Exception {
        final DatagramChannel channel = mock(DatagramChannel.class);
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.setName("test");
        clientConfig.setSecret(SECRET);
        final RadiusRequestContext context = new RadiusRequestContext(clientConfig, channel,
                new InetSocketAddress("127.0.0.1", 40000));

        assertThatThrownBy(() -> context.send(new AccessAccept()))
                .isInstanceOfSatisfying(RadiusProcessingException.class, e -> assertThat(e.getNature())
                        .isEqualTo(RadiusProcessingExceptionNature.INVALID_RESPONSE));

        verify(channel, never()).send(any(ByteBuffer.class), any());
    }

    @Test
    public void shouldSendAccessRejectInsteadOfNullResponse() throws Exception {
        final DatagramChannel channel = mock(DatagramChannel.class);
        final InetSocketAddress source = new InetSocketAddress("127.0.0.1", 40000);
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.setName("test");
        clientConfig.setSecret(SECRET);
        final RadiusRequestContext context = new RadiusRequestContext(clientConfig, channel, source);
        context.setRequestId((short) 42);
        context.setRequestAuthenticator(new RequestAuthenticator(REQUEST_AUTHENTICATOR));

        context.send(null);

        final ArgumentCaptor<ByteBuffer> sent = ArgumentCaptor.forClass(ByteBuffer.class);
        verify(channel).send(sent.capture(), eq(source));
        final ByteBuffer buffer = sent.getValue();
        final byte[] received = new byte[buffer.remaining()];
        buffer.get(received);
        final byte[] octets = PacketIntegrity.extractPacket(received, received.length);

        assertThat(octets).isNotNull();
        assertThat(PacketType.getPacketType(octets[0])).isEqualTo(PacketType.ACCESS_REJECT);
        assertThat(PacketIntegrity.verifyResponseAuthenticator(octets, REQUEST_AUTHENTICATOR, SECRET)).isTrue();
    }
}
