/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AccessAccept.java,v 1.2 2008/06/25 05:42:00 qcheng Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 * Portions Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 * Portions Copyright 2026 Wren Security.
 */
package com.sun.identity.authentication.modules.radius.client;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.authentication.modules.radius.RADIUSServer;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.shared.debug.Debug;

import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.radius.common.AccessAccept;
import org.forgerock.openam.radius.common.AccessChallenge;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.AccessRequest;
import org.forgerock.openam.radius.common.AttributeType;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.PacketFactory;
import org.forgerock.openam.radius.common.PacketIntegrity;
import org.forgerock.openam.radius.common.PacketIntegrity.MessageAuthenticatorStatus;
import org.forgerock.openam.radius.common.PacketType;
import org.forgerock.openam.radius.common.RequestAuthenticator;
import org.forgerock.openam.radius.common.StateAttribute;
import org.forgerock.openam.radius.common.StatusServer;
import org.forgerock.openam.radius.common.UserNameAttribute;
import org.forgerock.openam.radius.common.UserPasswordAttribute;
import org.forgerock.openam.radius.common.packet.NASIPAddressAttribute;
import org.forgerock.openam.radius.common.packet.NASPortAttribute;

/**
 * This class implements RFC2865 - Remote Authentication Dial In User Service (RADIUS), June 2000.
 */
public class RadiusConn {
    /**
     * The default timeout.
     */
    public static final int DEFAULT_TIMEOUT = 5;
    /**
     * The user name of the Access-Request sent to check an offline server.
     */
    private static final String HEALTH_CHECK_USERNAME = "nonexistent";
    /**
     * The password of the Access-Request sent to check an offline server.
     */
    private static final String HEALTH_CHECK_PASSWORD = "invalidpass";

    /**
     * How the health check finds out that an offline server is available again.
     */
    public enum HealthCheckType {
        /** Sends an Access-Request for a nonexistent user; any valid response will do. */
        ACCESS_REQUEST,
        /** Sends a Status-Server request (RFC 5997), which does not authenticate anyone. */
        STATUS_SERVER,
        /** Sends nothing; the server is used again once the health check interval has elapsed. */
        NONE
    }

    /**
     * The client secret.
     */
    private String secret = null;
    /**
     * The data gram socket.
     */
    private DatagramSocket socket = null;
    /**
     * The source of id generation for access request packets.
     */
    private short id = (short) currentTimeMillis();
    /**
     * Source of random bytes.
     */
    private SecureRandom random = null;
    /**
     * The set of primary servers to connect to.
     */
    private Set<RADIUSServer> primaries;
    /**
     * The secondary set of servers to connect to if the primaries are offline.
     */
    private Set<RADIUSServer> secondaries;
    /**
     * The health check interval.
     */
    private int healthCheckInterval = 5;
    /**
     * Whether responses without a Message-Authenticator attribute are discarded.
     */
    private boolean requireMessageAuthenticator = false;
    /**
     * The timeout of a response in milliseconds.
     */
    private final int timeoutMillis;
    /**
     * How offline servers are checked.
     */
    private HealthCheckType healthCheckType = HealthCheckType.ACCESS_REQUEST;
    /**
     * The offline servers, each with the connection that found it offline and is used to check it. Servers that are not
     * in the map are online.
     */
    private static final Map<RADIUSServer, OfflineServer> OFFLINE_SERVERS = new HashMap<>();
    /**
     * The Debug logger instance for this class.
     */
    private static Debug debug = Debug.getInstance("amAuthRadius");
    /**
     * The runnable task monitor that verifies connectivity to servers.
     */
    private static RADIUSMonitor serverMonitor;
    /**
     * An object used as an access lock.
     */
    private static final Object SERVER_MONITOR_LOCK = new Object();

    /**
     * Construct a connection object with a set of primary and seconary servers.
     *
     * @param primaries           the primary servers to connect to.
     * @param secondaries         the secondary servers to connect to.
     * @param secret              the secret shared between this client and the remote servers.
     * @param timeout             the timeout value.
     * @param healthCheckInterval the health check interval.
     * @throws SocketException thrown if unable to create a DatagramSocket.
     */
    public RadiusConn(Set<RADIUSServer> primaries, Set<RADIUSServer> secondaries,
                      String secret, int timeout, int healthCheckInterval) throws SocketException {
        this(primaries, secondaries, secret, timeout, null, healthCheckInterval);
    }

    /**
     * Constructs a connection object with only a set of primary servers.
     *
     * @param primaries           the primary servers to connect to.
     * @param secret              the secret shared between this client and the remote servers.
     * @param seed                the seed value to be used to create a {@link java.security.SecureRandom} instance.
     * @param healthCheckInterval the health check interval.
     * @throws SocketException thrown if unable to create a DatagramSocket.
     */
    public RadiusConn(Set<RADIUSServer> primaries, String secret, byte[] seed,
                      int healthCheckInterval) throws SocketException {
        this(primaries, primaries, secret, DEFAULT_TIMEOUT, seed, healthCheckInterval);
    }

    /**
     * Construct a connection object primary and secondary servers and seed for generating a {@link
     * java.security.SecureRandom}.
     *
     * @param primaries           the primary servers to connect to.
     * @param secondaries         the secondary servers to connect to.
     * @param secret              the secret shared between this client and the remote servers.
     * @param timeout             the timeout value.
     * @param seed                the seed value to be used to create a {@link java.security.SecureRandom} instance.
     * @param healthCheckInterval the health check interval.
     * @throws SocketException if a socket exception occurs.
     */
    public RadiusConn(Set<RADIUSServer> primaries, Set<RADIUSServer> secondaries,
                      String secret, int timeout, byte[] seed, int healthCheckInterval)
            throws SocketException {
        this(primaries, secondaries, secret, timeout, seed, healthCheckInterval, false);
    }

    /**
     * Construct a connection object primary and secondary servers, seed for generating a {@link
     * java.security.SecureRandom} and the Message-Authenticator policy.
     *
     * @param primaries                   the primary servers to connect to.
     * @param secondaries                 the secondary servers to connect to.
     * @param secret                      the secret shared between this client and the remote servers.
     * @param timeout                     the timeout value.
     * @param seed                        the seed value to be used to create a {@link java.security.SecureRandom}
     *                                    instance, or null.
     * @param healthCheckInterval         the health check interval.
     * @param requireMessageAuthenticator whether responses without a Message-Authenticator attribute (RFC 3579) are
     *                                    discarded.
     * @throws SocketException if a socket exception occurs.
     */
    public RadiusConn(Set<RADIUSServer> primaries, Set<RADIUSServer> secondaries,
                      String secret, int timeout, byte[] seed, int healthCheckInterval,
                      boolean requireMessageAuthenticator)
            throws SocketException {
        this.requireMessageAuthenticator = requireMessageAuthenticator;
        this.secret = secret;
        this.primaries = primaries;
        this.secondaries = secondaries;
        this.healthCheckInterval = healthCheckInterval;
        if (debug.messageEnabled()) {
            debug.message("Primary RADIUS servers: " + primaries);
            debug.message("Secondary RADIUS servers: " + secondaries);
        }
        timeoutMillis = timeout * 1000;
        socket = new DatagramSocket();
        socket.setSoTimeout(timeoutMillis);
        if (seed == null) {
            random = new SecureRandom();
        } else {
            random = new SecureRandom(seed);
        }
    }

    /**
     * Sets how this connection checks the servers it finds offline.
     *
     * @param type the kind of check.
     */
    public void setHealthCheckType(HealthCheckType type) {
        this.healthCheckType = type;
    }

    /**
     * Disconnects the underlying datagram socket.
     *
     * @throws IOException if an io exception occurs.
     */
    public void disconnect() throws IOException {
        socket.disconnect();
    }

    /**
     * Authenticates the username and password against the remote servers.
     *
     * @param name     the username.
     * @param password the password.
     * @throws IOException              if there is a problem.
     * @throws NoSuchAlgorithmException if there is a problem.
     * @throws RejectException          if there is a problem.
     * @throws ChallengeException       if there is a problem.
     */
    public void authenticate(String name, String password)
            throws IOException, NoSuchAlgorithmException,
            RejectException, ChallengeException {
        AccessRequest req = createAccessRequest();
        req.addAttribute(new UserNameAttribute(name));
        req.addAttribute(new UserPasswordAttribute(req.getAuthenticator(),
                secret, password));
        req.addAttribute(new NASIPAddressAttribute(InetAddress.getLocalHost()));
        req.addAttribute(new NASPortAttribute(socket.getLocalPort()));
        sendPacket(req);
    }

    /**
     * Sends an access-request to the server in response to a challenge request.
     *
     * @param name     the username.
     * @param password the password.
     * @param ce       the challenge exception providing access to the original challenge response.
     * @throws IOException              if there is a problem.
     * @throws NoSuchAlgorithmException if there is a problem.
     * @throws RejectException          if there is a problem.
     * @throws ChallengeException       if there is a problem.
     */
    public void replyChallenge(String name, String password,
                               ChallengeException ce) throws IOException, NoSuchAlgorithmException,
            RejectException, ChallengeException {
        StateAttribute state = (StateAttribute)
                ce.getAttributeSet().getAttributeByType(AttributeType.STATE);
        if (state == null) {
            throw new IOException("State not found in challenge");
        }
        AccessRequest req = createAccessRequest();
        req.addAttribute(state); // needed in challenge
        if (name != null) {
            req.addAttribute(new UserNameAttribute(name));
        }
        req.addAttribute(new UserPasswordAttribute(req.getAuthenticator(),
                secret, password));
        req.addAttribute(new NASIPAddressAttribute(InetAddress.getLocalHost()));
        req.addAttribute(new NASPortAttribute(socket.getLocalPort()));

        sendPacket(req);
    }

    /**
     * Finds an available server and then sends a packet to that servers.
     *
     * @param packet the packet.
     * @throws IOException        if there is a problem.
     * @throws RejectException    if there is a problem.
     * @throws ChallengeException if there is a problem.
     */
    private void sendPacket(Packet packet) throws IOException,
            RejectException, ChallengeException {
        Packet res = null;
        RADIUSServer server = null;
        while (res == null) {
            server = getOnlineServer();
            if (debug.messageEnabled()) {
                debug.message("Using " + server + " for contact RADIUS");
            }
            try {
                if (server == null) {
                    throw new IOException("No RADIUS server is online.");
                }
                InetAddress address = InetAddress.getByName(server.getHost());
                send(socket, packet, address, server.getPort());
                res = receive(socket, packet, address, server.getPort());
                if (res instanceof AccessReject) {
                    throw new RejectException((AccessReject) res);
                } else if (res instanceof AccessChallenge) {
                    throw new ChallengeException((AccessChallenge) res);
                } else if (!(res instanceof AccessAccept)) {
                    throw new IOException("Unexpected RADIUS response " + res);
                }
            } catch (IOException ioe) {
                if (ioe instanceof ConnectException || ioe instanceof SocketTimeoutException) {
                    markOffline(server);
                } else {
                    throw ioe;
                }
            }
        }
    }

    /**
     * Returns a server that has successfully been contacted.
     *
     * @return the server to connect to.
     */
    private RADIUSServer getOnlineServer() {
        synchronized (OFFLINE_SERVERS) {
            for (RADIUSServer server : primaries) {
                if (!OFFLINE_SERVERS.containsKey(server)) {
                    return server;
                }
            }
            for (RADIUSServer server : secondaries) {
                if (!OFFLINE_SERVERS.containsKey(server)) {
                    return server;
                }
            }
        }
        return null;
    }

    /**
     * Marks a server offline, to be checked with the settings of this connection, and starts the health check
     * monitor if it is not running.
     *
     * @param server the server that did not respond.
     */
    private void markOffline(RADIUSServer server) {
        if (debug.messageEnabled()) {
            debug.message("Moving server to offline state - " + server);
        }
        synchronized (SERVER_MONITOR_LOCK) {
            synchronized (OFFLINE_SERVERS) {
                OFFLINE_SERVERS.put(server, new OfflineServer(this, currentTimeMillis()));
            }
            if (serverMonitor == null || serverMonitor.cancelled || serverMonitor.scheduledExecutionTime() == -1) {
                serverMonitor = new RADIUSMonitor(healthCheckInterval);
                SystemTimer.getTimer().schedule(serverMonitor, new Date(((currentTimeMillis()) / 1000) * 1000));
            }
        }
    }

    /**
     * Checks whether an offline server is available again, as configured by {@link #setHealthCheckType}. A probe counts
     * only if the server answers it with a valid response, verified like the responses to authentication requests.
     *
     * @param server       the offline server.
     * @param offlineSince when the server was found offline.
     * @return true if the server may be used again.
     */
    boolean isAvailable(RADIUSServer server, long offlineSince) {
        if (healthCheckType == HealthCheckType.NONE) {
            return currentTimeMillis() - offlineSince >= healthCheckInterval * 60000L;
        }
        try (DatagramSocket probeSocket = new DatagramSocket()) {
            probeSocket.setSoTimeout(timeoutMillis);
            Packet request = createHealthCheckRequest(probeSocket);
            InetAddress address = InetAddress.getByName(server.getHost());
            send(probeSocket, request, address, server.getPort());
            receive(probeSocket, request, address, server.getPort());
            return true;
        } catch (IOException | NoSuchAlgorithmException | RuntimeException e) {
            if (debug.messageEnabled()) {
                debug.message("RADIUS server " + server + " is still unavailable: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Creates the request that checks whether a server is available.
     *
     * @param probeSocket the socket the request is sent from.
     * @return a Status-Server or Access-Request packet, as configured.
     * @throws IOException              if the local address can not be determined.
     * @throws NoSuchAlgorithmException if the request authenticator can not be generated.
     */
    private Packet createHealthCheckRequest(DatagramSocket probeSocket) throws IOException, NoSuchAlgorithmException {
        if (healthCheckType == HealthCheckType.STATUS_SERVER) {
            return new StatusServer(getIdentifier(), new RequestAuthenticator(random, secret));
        }
        AccessRequest req = createAccessRequest();
        req.addAttribute(new UserNameAttribute(HEALTH_CHECK_USERNAME));
        req.addAttribute(new UserPasswordAttribute(req.getAuthenticator(), secret, HEALTH_CHECK_PASSWORD));
        req.addAttribute(new NASIPAddressAttribute(InetAddress.getLocalHost()));
        req.addAttribute(new NASPortAttribute(probeSocket.getLocalPort()));
        return req;
    }

    /**
     * Generates a new access request packet identifier.
     *
     * @return the new identifier.
     */
    private short getIdentifier() {
        return id++;
    }

    /**
     * Signs the packet with a Message-Authenticator attribute and sends it.
     *
     * @param udpSocket the socket to send the packet from.
     * @param packet  the packet.
     * @param address the address of the target server.
     * @param port    the port of the target server.
     * @throws IOException if the packet can not be sent.
     */
    private void send(DatagramSocket udpSocket, Packet packet, InetAddress address, int port)
            throws IOException {
        PacketIntegrity.signRequest(packet, secret);
        byte[] data = packet.getOctets();
        udpSocket.send(new DatagramPacket(data, data.length, address, port));
        if (debug.messageEnabled()) {
            debug.message("Sent " + packet);
        }
    }

    /**
     * Blocking call that waits until a valid response to the request is received from the server. Datagrams that are
     * not a valid response (malformed, from another source, for another request or with a wrong authenticator) are
     * discarded as required by section 3 of RFC 2865 and the wait continues until the socket timeout elapses.
     *
     * @param udpSocket the socket the request was sent from.
     * @param request the request packet.
     * @param address the address of the server the request was sent to.
     * @param port    the port of the server the request was sent to.
     * @return the received response packet.
     * @throws IOException if no valid response is received before the timeout or the socket fails.
     */
    private Packet receive(DatagramSocket udpSocket, Packet request, InetAddress address, int port)
            throws IOException {
        final int timeout = udpSocket.getSoTimeout();
        final long deadline = timeout == 0 ? Long.MAX_VALUE : currentTimeMillis() + timeout;
        final byte[] buffer = new byte[PacketIntegrity.MAX_PACKET_LENGTH];
        try {
            while (true) {
                if (timeout != 0) {
                    long remaining = deadline - currentTimeMillis();
                    if (remaining <= 0) {
                        throw new SocketTimeoutException("No valid RADIUS response received from " + address);
                    }
                    udpSocket.setSoTimeout((int) Math.min(remaining, Integer.MAX_VALUE));
                }
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(dp);
                Packet response = toValidResponse(dp, request, address, port);
                if (response != null) {
                    if (debug.messageEnabled()) {
                        debug.message("Received " + response + " size=" + response.getAttributeSet().size());
                    }
                    return response;
                }
            }
        } finally {
            udpSocket.setSoTimeout(timeout);
        }
    }

    /**
     * Checks that a received datagram is a valid response to the request and parses it.
     *
     * @param dp      the received datagram.
     * @param request the request packet.
     * @param address the address of the server the request was sent to.
     * @param port    the port of the server the request was sent to.
     * @return the response packet, or null if the datagram must be discarded.
     */
    private Packet toValidResponse(DatagramPacket dp, Packet request, InetAddress address, int port) {
        if (!address.equals(dp.getAddress()) || port != dp.getPort()) {
            debug.warning("Discarding RADIUS packet from unexpected source {}:{}", dp.getAddress(), dp.getPort());
            return null;
        }
        byte[] data = PacketIntegrity.extractPacket(dp.getData(), dp.getLength());
        if (data == null) {
            debug.warning("Discarding malformed RADIUS packet from {}", address);
            return null;
        }
        if ((data[1] & 0xFF) != (request.getIdentifier() & 0xFF)) {
            debug.warning("Discarding RADIUS packet with identifier {} not matching the request identifier {}",
                    data[1] & 0xFF, request.getIdentifier() & 0xFF);
            return null;
        }
        PacketType type = PacketType.getPacketType(data[0]);
        if (type != PacketType.ACCESS_ACCEPT && type != PacketType.ACCESS_REJECT
                && type != PacketType.ACCESS_CHALLENGE) {
            debug.warning("Discarding RADIUS packet with unexpected code {} from {}", data[0] & 0xFF, address);
            return null;
        }
        byte[] requestAuthenticator = request.getAuthenticator().getOctets();
        if (!PacketIntegrity.verifyResponseAuthenticator(data, requestAuthenticator, secret)) {
            debug.warning("Discarding RADIUS response from {} with invalid Response Authenticator; "
                    + "check the shared secret", address);
            return null;
        }
        MessageAuthenticatorStatus status = PacketIntegrity.checkMessageAuthenticator(data, requestAuthenticator,
                secret);
        if (status == MessageAuthenticatorStatus.INVALID) {
            debug.warning("Discarding RADIUS response from {} with invalid Message-Authenticator", address);
            return null;
        }
        if (status == MessageAuthenticatorStatus.ABSENT && requireMessageAuthenticator) {
            debug.error("Discarding RADIUS response from {} without Message-Authenticator, which the module "
                    + "configuration requires", address);
            return null;
        }
        return PacketFactory.toPacket(data);
    }

    /**
     * Generates an access request packet.
     *
     * @return the access request packet.
     * @throws NoSuchAlgorithmException
     */
    private AccessRequest createAccessRequest() throws
            NoSuchAlgorithmException {
        RequestAuthenticator ra = new RequestAuthenticator(random, secret);
        AccessRequest req = new AccessRequest(getIdentifier(), ra);
        return req;
    }

    /**
     * An offline server and the connection that found it offline, whose settings are used to check it.
     */
    private static final class OfflineServer {
        private final RadiusConn connection;
        private final long since;

        private OfflineServer(RadiusConn connection, long since) {
            this.connection = connection;
            this.since = since;
        }
    }

    /**
     * A monitor that periodically checks the offline servers and marks those that are available again online.
     */
    private static final class RADIUSMonitor extends GeneralTaskRunnable {
        /**
         * Indicates if monitoring is cancelled, written under {@code SERVER_MONITOR_LOCK}.
         */
        private volatile boolean cancelled = false;
        /**
         * The interval between the checks in minutes.
         */
        private final int interval;

        private RADIUSMonitor(int interval) {
            this.interval = interval;
        }

        /**
         * Not implemented in this monitor. Always returns false.
         *
         * @param key unused parameter.
         * @return always returns false.
         */
        public boolean addElement(Object key) {
            return false;
        }

        /**
         * Not implemented in this monitor. Always returns false.
         *
         * @param key unused parameter.
         * @return always returns false.
         */
        public boolean removeElement(Object key) {
            return false;
        }

        /**
         * Always returns true.
         *
         * @return always returns true.
         */
        public boolean isEmpty() {
            return true;
        }

        /**
         * Returns the run period of this TaskRunnable.
         *
         * @return A long value to indicate the run period.
         */
        public long getRunPeriod() {
            return cancelled ? 0 : interval * 60000L;
        }

        /**
         * Checks the offline servers and stops the monitor once all of them are online.
         */
        public void run() {
            if (debug.messageEnabled()) {
                debug.message("Checking server statuses");
            }
            Map<RADIUSServer, OfflineServer> offline;
            synchronized (OFFLINE_SERVERS) {
                offline = new LinkedHashMap<>(OFFLINE_SERVERS);
            }
            for (Map.Entry<RADIUSServer, OfflineServer> entry : offline.entrySet()) {
                RADIUSServer server = entry.getKey();
                OfflineServer status = entry.getValue();
                if (status.connection.isAvailable(server, status.since)) {
                    if (debug.messageEnabled()) {
                        debug.message("Moving server to online state - " + server);
                    }
                    synchronized (OFFLINE_SERVERS) {
                        OFFLINE_SERVERS.remove(server, status);
                    }
                }
            }
            synchronized (SERVER_MONITOR_LOCK) {
                synchronized (OFFLINE_SERVERS) {
                    if (OFFLINE_SERVERS.isEmpty()) {
                        cancelled = true;
                    }
                }
            }
        }
    }
}
