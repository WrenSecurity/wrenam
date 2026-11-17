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

import static org.forgerock.openam.utils.Time.currentTimeMillis;

import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.shared.debug.Debug;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.forgerock.openam.radius.common.AccessAccept;
import org.forgerock.openam.radius.common.AccessChallenge;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.AccessRequest;
import org.forgerock.openam.radius.common.AttributeType;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.StateAttribute;
import org.forgerock.openam.radius.common.UserNameAttribute;
import org.forgerock.openam.radius.common.UserPasswordAttribute;
import org.forgerock.openam.radius.common.packet.NASIPAddressAttribute;
import org.forgerock.openam.radius.common.packet.NASPortAttribute;

/**
 * A RADIUS client (RFC 2865) authenticating users against primary and secondary servers. A server that does not
 * respond is offline until its health check finds it available again; meanwhile the other servers are used.
 */
public class RadiusClient {

    /**
     * The user name of the Access-Request sent to check an offline server.
     */
    private static final String HEALTH_CHECK_USERNAME = "nonexistent";

    /**
     * The password of the Access-Request sent to check an offline server.
     */
    private static final String HEALTH_CHECK_PASSWORD = "invalidpass";

    /**
     * The offline servers, each with the scheduled check that brings it back online.
     */
    private static final Map<RadiusServer, HealthCheck> OFFLINE_SERVERS = new HashMap<>();

    private static final Debug DEBUG = Debug.getInstance("amAuthRadius");

    private final Set<RadiusServer> primaries;

    private final Set<RadiusServer> secondaries;

    /**
     * The server that received the last request, to which a challenge is answered, or null.
     */
    private RadiusServer selectedServer;

    /**
     * Creates a client.
     *
     * @param primaries the servers to use.
     * @param secondaries the servers to use when all the primary servers are offline.
     */
    public RadiusClient(Set<RadiusServer> primaries, Set<RadiusServer> secondaries) {
        this.primaries = primaries;
        this.secondaries = secondaries;
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Primary RADIUS servers: " + primaries);
            DEBUG.message("Secondary RADIUS servers: " + secondaries);
        }
    }

    /**
     * Authenticates the user name and password against an online server.
     *
     * @param name the user name.
     * @param password the password.
     * @throws IOException if no server responds or the response is unexpected.
     * @throws NoSuchAlgorithmException if the request authenticator can not be generated.
     * @throws RejectException if the server rejects the user.
     * @throws ChallengeException if the server challenges the user, see {@link #replyChallenge}.
     */
    public void authenticate(String name, String password)
            throws IOException, NoSuchAlgorithmException, RejectException, ChallengeException {
        while (true) {
            RadiusServer server = getOnlineServer();
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Using " + server + " for contact RADIUS");
            }
            if (server == null) {
                throw new IOException("No RADIUS server is online.");
            }
            selectedServer = server;
            Packet response;
            try (RadiusConnection connection = new RadiusConnection(server)) {
                response = connection.send(createAccessRequest(connection, null, name, password));
            } catch (ConnectException | SocketTimeoutException e) {
                markOffline(server);
                continue;
            }
            handleResponse(response);
            return;
        }
    }

    /**
     * Answers a challenge received by the last call of {@link #authenticate} or of this method. The answer is sent
     * to the server that sent the challenge, without failing over to another server, which would not know the state
     * of the challenge.
     *
     * @param name the user name, or null.
     * @param password the answer to the challenge.
     * @param ce the challenge.
     * @throws IOException if the server does not respond, the challenge has no state or the response is unexpected.
     * @throws NoSuchAlgorithmException if the request authenticator can not be generated.
     * @throws RejectException if the server rejects the user.
     * @throws ChallengeException if the server challenges the user again.
     */
    public void replyChallenge(String name, String password, ChallengeException ce)
            throws IOException, NoSuchAlgorithmException, RejectException, ChallengeException {
        StateAttribute state = (StateAttribute) ce.getAttributeSet().getAttributeByType(AttributeType.STATE);
        if (state == null) {
            throw new IOException("State not found in challenge");
        }
        if (selectedServer == null) {
            throw new IllegalStateException("No challenge received");
        }
        Packet response;
        try (RadiusConnection connection = new RadiusConnection(selectedServer)) {
            response = connection.send(createAccessRequest(connection, state, name, password));
        } catch (ConnectException | SocketTimeoutException e) {
            markOffline(selectedServer);
            throw e;
        }
        handleResponse(response);
    }

    /**
     * Creates an Access-Request with the credentials of a user for the server of a connection.
     *
     * @param connection the connection the request is sent through.
     * @param state the state of the challenge answered by the request, or null.
     * @param name the user name, or null.
     * @param password the password or the answer to the challenge.
     * @return the request.
     */
    private static AccessRequest createAccessRequest(RadiusConnection connection, StateAttribute state, String name,
            String password) throws IOException, NoSuchAlgorithmException {
        AccessRequest request = connection.createAccessRequest();
        if (state != null) {
            request.addAttribute(state);
        }
        if (name != null) {
            request.addAttribute(new UserNameAttribute(name));
        }
        request.addAttribute(new UserPasswordAttribute(request.getAuthenticator(), connection.getServer().getSecret(),
                password));
        request.addAttribute(new NASIPAddressAttribute(InetAddress.getLocalHost()));
        request.addAttribute(new NASPortAttribute(connection.getLocalPort()));
        return request;
    }

    /**
     * Turns a response other than Access-Accept into an exception.
     *
     * @param response the response.
     */
    private static void handleResponse(Packet response) throws IOException, RejectException, ChallengeException {
        if (response instanceof AccessReject) {
            throw new RejectException((AccessReject) response);
        } else if (response instanceof AccessChallenge) {
            throw new ChallengeException((AccessChallenge) response);
        } else if (!(response instanceof AccessAccept)) {
            throw new IOException("Unexpected RADIUS response " + response);
        }
    }

    /**
     * Returns the first primary server that is online, or the first online secondary server if there is none.
     *
     * @return the server, or null if all servers are offline.
     */
    private RadiusServer getOnlineServer() {
        synchronized (OFFLINE_SERVERS) {
            for (RadiusServer server : primaries) {
                if (!OFFLINE_SERVERS.containsKey(server)) {
                    return server;
                }
            }
            for (RadiusServer server : secondaries) {
                if (!OFFLINE_SERVERS.containsKey(server)) {
                    return server;
                }
            }
        }
        return null;
    }

    /**
     * Marks a server offline and schedules its health check, unless it is offline already.
     *
     * @param server the server that did not respond.
     */
    private static void markOffline(RadiusServer server) {
        HealthCheck check = new HealthCheck(server);
        synchronized (OFFLINE_SERVERS) {
            if (OFFLINE_SERVERS.putIfAbsent(server, check) != null) {
                return;
            }
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Moving server to offline state - " + server);
        }
        SystemTimer.getTimer().schedule(check, new Date(currentTimeMillis() + check.getRunPeriod()));
    }

    /**
     * Checks whether an offline server is available again, as configured by its health check type. A probe counts
     * only if the server answers it with a valid response, verified like the responses to authentication requests.
     * Called once the health check interval has elapsed since the server was found offline or last checked.
     *
     * @param server the offline server.
     * @return true if the server may be used again.
     */
    static boolean isAvailable(RadiusServer server) {
        if (server.getHealthCheckType() == RadiusServer.HealthCheckType.NONE) {
            return true;
        }
        try (RadiusConnection probe = new RadiusConnection(server)) {
            probe.send(createHealthCheckRequest(probe));
            return true;
        } catch (IOException | NoSuchAlgorithmException | RuntimeException e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message(server + " is still unavailable: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Creates the request that checks whether a server is available.
     *
     * @param probe the connection to the server.
     * @return a Status-Server or Access-Request packet, as configured.
     * @throws IOException if the local address can not be determined.
     * @throws NoSuchAlgorithmException if the request authenticator can not be generated.
     */
    private static Packet createHealthCheckRequest(RadiusConnection probe)
            throws IOException, NoSuchAlgorithmException {
        if (probe.getServer().getHealthCheckType() == RadiusServer.HealthCheckType.STATUS_SERVER) {
            return probe.createStatusServer();
        }
        AccessRequest req = probe.createAccessRequest();
        req.addAttribute(new UserNameAttribute(HEALTH_CHECK_USERNAME));
        req.addAttribute(new UserPasswordAttribute(req.getAuthenticator(), probe.getServer().getSecret(),
                HEALTH_CHECK_PASSWORD));
        req.addAttribute(new NASIPAddressAttribute(InetAddress.getLocalHost()));
        req.addAttribute(new NASPortAttribute(probe.getLocalPort()));
        return req;
    }

    /**
     * The scheduled health check of an offline server. It runs once the health check interval of the server has
     * elapsed and then again after each interval until the server is available, which brings it back online.
     */
    static final class HealthCheck extends GeneralTaskRunnable {

        private final RadiusServer server;

        /**
         * Whether the server is available again and the check is not to run any more.
         */
        private volatile boolean done = false;

        /**
         * Creates the health check of a server.
         *
         * @param server the offline server.
         */
        HealthCheck(RadiusServer server) {
            this.server = server;
        }

        /**
         * Not implemented in this task.
         *
         * @param key unused.
         * @return always false.
         */
        @Override
        public boolean addElement(Object key) {
            return false;
        }

        /**
         * Not implemented in this task.
         *
         * @param key unused.
         * @return always false.
         */
        @Override
        public boolean removeElement(Object key) {
            return false;
        }

        /**
         * Not implemented in this task.
         *
         * @return always true.
         */
        @Override
        public boolean isEmpty() {
            return true;
        }

        /**
         * Returns the time until the next check.
         *
         * @return the health check interval of the server in milliseconds, or -1 once the server is available.
         */
        @Override
        public long getRunPeriod() {
            return done ? -1 : server.getHealthCheckPeriod();
        }

        /**
         * Checks the server and brings it back online if it is available.
         */
        @Override
        public void run() {
            if (!isAvailable(server)) {
                return;
            }
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Moving server to online state - " + server);
            }
            synchronized (OFFLINE_SERVERS) {
                OFFLINE_SERVERS.remove(server, this);
            }
            done = true;
        }

    }

}
