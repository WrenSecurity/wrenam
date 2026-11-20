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

import static org.forgerock.openam.utils.Time.currentTimeMillis;

import com.sun.identity.shared.debug.Debug;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.forgerock.openam.radius.common.AccessRequest;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.PacketFactory;
import org.forgerock.openam.radius.common.PacketIntegrity;
import org.forgerock.openam.radius.common.PacketIntegrity.MessageAuthenticatorStatus;
import org.forgerock.openam.radius.common.PacketType;
import org.forgerock.openam.radius.common.RequestAuthenticator;
import org.forgerock.openam.radius.common.StatusServer;

/**
 * A UDP socket for the requests to one RADIUS server. Requests are signed with the shared secret of the server and
 * only valid responses to them are received, as required by section 3 of RFC 2865.
 */
final class RadiusConnection implements AutoCloseable {

    private static final Debug DEBUG = Debug.getInstance("amAuthRadius");

    /**
     * Source of the request authenticators and the initial identifiers.
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RadiusServer server;

    private final DatagramSocket socket;

    /**
     * The identifier of the next request, unique among the recent requests from this socket.
     */
    private short identifier = (short) RANDOM.nextInt(256);

    /**
     * Opens a socket for the requests to a server.
     *
     * @param server the server to send the requests to.
     * @throws SocketException if the socket can not be opened.
     */
    RadiusConnection(RadiusServer server) throws SocketException {
        this.server = server;
        this.socket = new DatagramSocket();
        try {
            socket.setSoTimeout(server.getTimeoutMillis());
        } catch (SocketException e) {
            socket.close();
            throw e;
        }
    }

    /**
     * Returns the server the requests are sent to.
     *
     * @return the server.
     */
    RadiusServer getServer() {
        return server;
    }

    /**
     * Returns the local port of the socket, the NAS-Port of the requests.
     *
     * @return the local port.
     */
    int getLocalPort() {
        return socket.getLocalPort();
    }

    /**
     * Creates an Access-Request with a new identifier and a request authenticator for the server.
     *
     * @return the request without attributes.
     * @throws NoSuchAlgorithmException if the request authenticator can not be generated.
     */
    AccessRequest createAccessRequest() throws NoSuchAlgorithmException {
        return new AccessRequest(nextIdentifier(), new RequestAuthenticator(RANDOM, server.getSecret()));
    }

    /**
     * Creates a Status-Server request (RFC 5997) with a new identifier and a request authenticator for the server.
     *
     * @return the request.
     * @throws NoSuchAlgorithmException if the request authenticator can not be generated.
     */
    StatusServer createStatusServer() throws NoSuchAlgorithmException {
        return new StatusServer(nextIdentifier(), new RequestAuthenticator(RANDOM, server.getSecret()));
    }

    /**
     * Signs a request with a Message-Authenticator attribute, sends it to the server and waits for a valid response.
     *
     * @param request the request created by this connection.
     * @return the response.
     * @throws IOException if the request can not be sent or no valid response is received before the timeout.
     */
    Packet send(Packet request) throws IOException {
        InetAddress address = InetAddress.getByName(server.getHost());
        PacketIntegrity.signRequest(request, server.getSecret());
        byte[] data = request.getOctets();
        socket.send(new DatagramPacket(data, data.length, address, server.getPort()));
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Sent " + request);
        }
        return receive(request, address);
    }

    /**
     * Closes the socket.
     */
    @Override
    public void close() {
        socket.close();
    }

    private short nextIdentifier() {
        return identifier++;
    }

    /**
     * Waits until a valid response to the request is received from the server. Datagrams that are not a valid
     * response (malformed, from another source, for another request or with a wrong authenticator) are discarded and
     * the wait continues until the timeout elapses.
     */
    private Packet receive(Packet request, InetAddress address) throws IOException {
        final int timeout = socket.getSoTimeout();
        final long deadline = timeout == 0 ? Long.MAX_VALUE : currentTimeMillis() + timeout;
        final byte[] buffer = new byte[PacketIntegrity.MAX_PACKET_LENGTH];
        try {
            while (true) {
                if (timeout != 0) {
                    long remaining = deadline - currentTimeMillis();
                    if (remaining <= 0) {
                        throw new SocketTimeoutException("No valid RADIUS response received from " + address);
                    }
                    socket.setSoTimeout((int) Math.min(remaining, Integer.MAX_VALUE));
                }
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                socket.receive(dp);
                Packet response = toValidResponse(dp, request, address);
                if (response != null) {
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("Received " + response + " size=" + response.getAttributeSet().size());
                    }
                    return response;
                }
            }
        } finally {
            socket.setSoTimeout(timeout);
        }
    }

    /**
     * Checks that a received datagram is a valid response to the request and parses it.
     *
     * @return the response, or null if the datagram must be discarded.
     */
    private Packet toValidResponse(DatagramPacket dp, Packet request, InetAddress address) {
        if (!address.equals(dp.getAddress()) || server.getPort() != dp.getPort()) {
            DEBUG.warning("Discarding RADIUS packet from unexpected source {}:{}", dp.getAddress(), dp.getPort());
            return null;
        }
        byte[] data = PacketIntegrity.extractPacket(dp.getData(), dp.getLength());
        if (data == null) {
            DEBUG.warning("Discarding malformed RADIUS packet from {}", address);
            return null;
        }
        if ((data[1] & 0xFF) != (request.getIdentifier() & 0xFF)) {
            DEBUG.warning("Discarding RADIUS packet with identifier {} not matching the request identifier {}",
                    data[1] & 0xFF, request.getIdentifier() & 0xFF);
            return null;
        }
        PacketType type = PacketType.getPacketType(data[0]);
        if (type != PacketType.ACCESS_ACCEPT && type != PacketType.ACCESS_REJECT
                && type != PacketType.ACCESS_CHALLENGE) {
            DEBUG.warning("Discarding RADIUS packet with unexpected code {} from {}", data[0] & 0xFF, address);
            return null;
        }
        byte[] requestAuthenticator = request.getAuthenticator().getOctets();
        if (!PacketIntegrity.verifyResponseAuthenticator(data, requestAuthenticator, server.getSecret())) {
            DEBUG.warning("Discarding RADIUS response from {} with invalid Response Authenticator; "
                    + "check the shared secret", address);
            return null;
        }
        MessageAuthenticatorStatus status = PacketIntegrity.checkMessageAuthenticator(data, requestAuthenticator,
                server.getSecret());
        if (status == MessageAuthenticatorStatus.INVALID) {
            DEBUG.warning("Discarding RADIUS response from {} with invalid Message-Authenticator", address);
            return null;
        }
        if (status == MessageAuthenticatorStatus.ABSENT && server.isMessageAuthenticatorRequired()) {
            DEBUG.error("Discarding RADIUS response from {} without Message-Authenticator, which the module "
                    + "configuration requires", address);
            return null;
        }
        return PacketFactory.toPacket(data);
    }

}
