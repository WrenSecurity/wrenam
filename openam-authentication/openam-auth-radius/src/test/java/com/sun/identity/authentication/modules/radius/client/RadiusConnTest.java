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
package com.sun.identity.authentication.modules.radius.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.forgerock.openam.radius.common.AccessAccept;
import org.forgerock.openam.radius.common.AccessChallenge;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.AttributeType;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.PacketFactory;
import org.forgerock.openam.radius.common.PacketType;
import org.forgerock.openam.radius.common.PacketIntegrity;
import org.forgerock.openam.radius.common.PacketIntegrity.MessageAuthenticatorStatus;
import org.forgerock.openam.radius.common.RequestAuthenticator;
import org.forgerock.openam.radius.common.ResponseAuthenticator;
import org.forgerock.openam.radius.common.StateAttribute;
import org.forgerock.openam.radius.common.UserNameAttribute;
import org.forgerock.openam.radius.common.UserPasswordAttribute;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.authentication.modules.radius.RADIUSServer;
import com.sun.identity.authentication.modules.radius.client.RadiusConn.HealthCheckType;

/**
 * Tests that {@link RadiusConn} accepts only authentic responses from the server it sent the request to.
 */
public class RadiusConnTest {

    private static final String SECRET = "s3cret";

    private static final int TIMEOUT_SECONDS = 1;

    private static final int HEALTH_CHECK_INTERVAL_MINUTES = 5;

    private DatagramSocket server;

    private DatagramSocket attacker;

    private Thread serverThread;

    private final AtomicReference<byte[]> receivedRequest = new AtomicReference<>();

    @BeforeMethod
    public void setUp() throws Exception {
        server = new DatagramSocket(0, InetAddress.getLoopbackAddress());
        attacker = new DatagramSocket(0, InetAddress.getLoopbackAddress());
    }

    @AfterMethod
    public void tearDown() throws Exception {
        server.close();
        attacker.close();
        if (serverThread != null) {
            serverThread.join(5000);
        }
    }

    @Test
    public void shouldAcceptValidAccessAccept() throws Exception {
        respond((request, client) -> send(server, signed(new AccessAccept(), request), client));

        connection(false).authenticate("user", "password");

        assertThat(PacketIntegrity.checkMessageAuthenticator(receivedRequest.get(), null, SECRET))
                .isEqualTo(MessageAuthenticatorStatus.VALID);
    }

    @Test
    public void shouldThrowRejectExceptionOnValidAccessReject() throws Exception {
        respond((request, client) -> send(server, signed(new AccessReject(), request), client));

        assertThatThrownBy(() -> connection(false).authenticate("user", "password"))
                .isInstanceOf(RejectException.class);
    }

    @Test
    public void shouldThrowChallengeExceptionOnValidAccessChallenge() throws Exception {
        respond((request, client) -> {
            AccessChallenge challenge = new AccessChallenge();
            challenge.addAttribute(new StateAttribute("state"));
            send(server, signed(challenge, request), client);
        });

        assertThatThrownBy(() -> connection(false).authenticate("user", "password"))
                .isInstanceOf(ChallengeException.class);
    }

    @Test
    public void shouldDiscardAccessAcceptWithWrongSecret() throws Exception {
        respond((request, client) -> send(server, signed(new AccessAccept(), request, "wrong"), client));

        assertThatThrownBy(() -> connection(false).authenticate("user", "password"))
                .isInstanceOf(IOException.class);
    }

    @Test
    public void shouldDiscardUnsignedAccessAccept() throws Exception {
        respond((request, client) -> {
            AccessAccept accept = new AccessAccept();
            accept.setIdentifier((short) (request[1] & 0xFF));
            accept.setAuthenticator(new ResponseAuthenticator(new byte[16]));
            send(server, accept.getOctets(), client);
        });

        assertThatThrownBy(() -> connection(false).authenticate("user", "password"))
                .isInstanceOf(IOException.class);
    }

    @Test
    public void shouldDiscardAccessAcceptFromOtherSource() throws Exception {
        respond((request, client) -> {
            send(attacker, signed(new AccessAccept(), request), client);
            send(server, signed(new AccessReject(), request), client);
        });

        assertThatThrownBy(() -> connection(false).authenticate("user", "password"))
                .isInstanceOf(RejectException.class);
    }

    @Test
    public void shouldDiscardAccessAcceptForOtherRequest() throws Exception {
        respond((request, client) -> {
            byte[] otherRequest = request.clone();
            otherRequest[1]++;
            send(server, signed(new AccessAccept(), otherRequest), client);
            send(server, signed(new AccessReject(), request), client);
        });

        assertThatThrownBy(() -> connection(false).authenticate("user", "password"))
                .isInstanceOf(RejectException.class);
    }

    @Test
    public void shouldDiscardResponseOfUnexpectedType() throws Exception {
        respond((request, client) -> {
            byte[] response = signed(new AccessAccept(), request);
            response[0] = 5; // Accounting-Response
            send(server, resign(response, request, SECRET), client);
        });

        assertThatThrownBy(() -> connection(false).authenticate("user", "password"))
                .isInstanceOf(IOException.class);
    }

    @Test
    public void shouldAcceptResponseWithoutMessageAuthenticatorUnlessRequired() throws Exception {
        respond((request, client) -> send(server, signedWithoutMessageAuthenticator(new AccessAccept(), request),
                client));

        connection(false).authenticate("user", "password");
    }

    @Test
    public void shouldDiscardResponseWithoutMessageAuthenticatorWhenRequired() throws Exception {
        respond((request, client) -> send(server, signedWithoutMessageAuthenticator(new AccessAccept(), request),
                client));

        assertThatThrownBy(() -> connection(true).authenticate("user", "password"))
                .isInstanceOf(IOException.class);
    }

    @Test
    public void shouldDiscardResponseWithWrongMessageAuthenticator() throws Exception {
        respond((request, client) -> {
            byte[] response = signed(new AccessAccept(), request);
            response[22] ^= 1; // first octet of the Message-Authenticator value
            send(server, resign(response, request, SECRET), client);
        });

        assertThatThrownBy(() -> connection(false).authenticate("user", "password"))
                .isInstanceOf(IOException.class);
    }

    @Test
    public void shouldNotSendTooLongPasswordNorMarkServerOffline() throws Exception {
        RadiusConn connection = connection(false);
        String password = "x".repeat(UserPasswordAttribute.MAX_PASSWORD_LENGTH + 1);

        assertThatThrownBy(() -> connection.authenticate("user", password))
                .isInstanceOf(IllegalArgumentException.class);

        // the server is still in use and receives the next request
        respond((request, client) -> send(server, signed(new AccessAccept(), request), client));
        connection.authenticate("user", "password");
        assertThat(receivedRequest.get()).isNotNull();
    }

    @Test
    public void shouldFindServerAvailableOnValidStatusServerResponse() throws Exception {
        respond((request, client) -> send(server, signed(new AccessAccept(), request), client));
        RadiusConn connection = connection(false);
        connection.setHealthCheckType(HealthCheckType.STATUS_SERVER);

        assertThat(connection.isAvailable(radiusServer(), System.currentTimeMillis())).isTrue();
        assertThat(PacketType.getPacketType(receivedRequest.get()[0])).isEqualTo(PacketType.STATUS_SERVER);
        assertThat(PacketIntegrity.checkMessageAuthenticator(receivedRequest.get(), null, SECRET))
                .isEqualTo(MessageAuthenticatorStatus.VALID);
    }

    @Test
    public void shouldFindServerAvailableOnValidAccessReject() throws Exception {
        respond((request, client) -> send(server, signed(new AccessReject(), request), client));
        RadiusConn connection = connection(false);
        connection.setHealthCheckType(HealthCheckType.ACCESS_REQUEST);

        assertThat(connection.isAvailable(radiusServer(), System.currentTimeMillis())).isTrue();
        UserNameAttribute user = (UserNameAttribute) PacketFactory.toPacket(receivedRequest.get())
                .getAttributeSet().getAttributeByType(AttributeType.USER_NAME);
        assertThat(user.getName()).isEqualTo("nonexistent");
    }

    @Test
    public void shouldNotFindServerAvailableOnResponseWithWrongSecret() throws Exception {
        respond((request, client) -> send(server, signed(new AccessAccept(), request, "wrong"), client));
        RadiusConn connection = connection(false);
        connection.setHealthCheckType(HealthCheckType.STATUS_SERVER);

        assertThat(connection.isAvailable(radiusServer(), System.currentTimeMillis())).isFalse();
    }

    @Test
    public void shouldNotFindServerAvailableOnResponseFromOtherSource() throws Exception {
        respond((request, client) -> send(attacker, signed(new AccessAccept(), request), client));
        RadiusConn connection = connection(false);
        connection.setHealthCheckType(HealthCheckType.ACCESS_REQUEST);

        assertThat(connection.isAvailable(radiusServer(), System.currentTimeMillis())).isFalse();
    }

    @Test
    public void shouldNotFindServerAvailableWithoutResponse() throws Exception {
        respond((request, client) -> { });
        RadiusConn connection = connection(false);
        connection.setHealthCheckType(HealthCheckType.STATUS_SERVER);

        assertThat(connection.isAvailable(radiusServer(), System.currentTimeMillis())).isFalse();
    }

    @Test
    public void shouldFindServerAvailableWithoutProbeOnceIntervalElapsed() throws Exception {
        RadiusConn connection = connection(false);
        connection.setHealthCheckType(HealthCheckType.NONE);
        long now = System.currentTimeMillis();

        assertThat(connection.isAvailable(radiusServer(), now)).isFalse();
        assertThat(connection.isAvailable(radiusServer(), now - HEALTH_CHECK_INTERVAL_MINUTES * 60000L)).isTrue();
    }

    private RADIUSServer radiusServer() {
        return new RADIUSServer(InetAddress.getLoopbackAddress().getHostAddress(), server.getLocalPort());
    }

    private RadiusConn connection(boolean requireMessageAuthenticator) throws Exception {
        Set<RADIUSServer> servers = Collections.singleton(radiusServer());
        return new RadiusConn(servers, servers, SECRET, TIMEOUT_SECONDS, null, HEALTH_CHECK_INTERVAL_MINUTES,
                requireMessageAuthenticator);
    }

    /**
     * Handles a single request in a background thread.
     */
    private void respond(Responder responder) {
        serverThread = new Thread(() -> {
            try {
                DatagramPacket dp = new DatagramPacket(new byte[4096], 4096);
                server.receive(dp);
                byte[] request = Arrays.copyOf(dp.getData(), dp.getLength());
                receivedRequest.set(request);
                responder.respond(request, dp.getSocketAddress());
            } catch (Exception e) {
                // the socket is closed at the end of the test
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private static void send(DatagramSocket from, byte[] data, SocketAddress to) throws IOException {
        from.send(new DatagramPacket(data, data.length, to));
    }

    private static byte[] signed(Packet response, byte[] request) {
        return signed(response, request, SECRET);
    }

    private static byte[] signed(Packet response, byte[] request, String secret) {
        response.setIdentifier((short) (request[1] & 0xFF));
        PacketIntegrity.signResponse(response, new RequestAuthenticator(Arrays.copyOfRange(request, 4, 20)), secret);
        return response.getOctets();
    }

    private static byte[] signedWithoutMessageAuthenticator(Packet response, byte[] request) throws Exception {
        response.setIdentifier((short) (request[1] & 0xFF));
        response.setAuthenticator(new ResponseAuthenticator(new byte[16]));
        return resign(response.getOctets(), request, SECRET);
    }

    /**
     * Recomputes the Response Authenticator of a response as described in section 3 of RFC 2865.
     */
    private static byte[] resign(byte[] response, byte[] request, String secret) throws Exception {
        byte[] result = response.clone();
        System.arraycopy(request, 4, result, 4, 16);
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(result);
        md5.update(secret.getBytes(StandardCharsets.UTF_8));
        System.arraycopy(md5.digest(), 0, result, 4, 16);
        return result;
    }

    /**
     * Sends the responses of the fake server to a request.
     */
    private interface Responder {
        void respond(byte[] request, SocketAddress client) throws Exception;
    }
}
