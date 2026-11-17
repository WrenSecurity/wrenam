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

import java.util.Objects;

/**
 * A remote RADIUS server to which authentication is delegated, with the settings it is used with: the shared secret,
 * how its responses are verified and how it is checked while it is offline.
 *
 * <p>Servers with the same address and settings are equal and share their offline state. A server with changed
 * settings, e.g. after a configuration change, is a different server that is online until it fails itself.
 */
public final class RadiusServer {

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

    private final String host;

    private final int port;

    private final String secret;

    private final int timeout;

    private final boolean messageAuthenticatorRequired;

    private final HealthCheckType healthCheckType;

    private final int healthCheckInterval;

    /**
     * Creates a server.
     *
     * @param host the host name or address of the server.
     * @param port the UDP port of the server.
     * @param secret the secret shared with the server.
     * @param timeout the timeout of a response in seconds.
     * @param messageAuthenticatorRequired whether responses without a Message-Authenticator attribute are discarded.
     * @param healthCheckType how the server is checked while it is offline.
     * @param healthCheckInterval the interval between the checks of the server while it is offline, in minutes.
     */
    public RadiusServer(String host, int port, String secret, int timeout, boolean messageAuthenticatorRequired,
            HealthCheckType healthCheckType, int healthCheckInterval) {
        this.host = host;
        this.port = port;
        this.secret = secret;
        this.timeout = timeout;
        this.messageAuthenticatorRequired = messageAuthenticatorRequired;
        this.healthCheckType = healthCheckType;
        this.healthCheckInterval = healthCheckInterval;
    }

    /**
     * Returns the host name or address of the server.
     *
     * @return the host.
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the UDP port of the server.
     *
     * @return the port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the secret shared with the server.
     *
     * @return the shared secret.
     */
    String getSecret() {
        return secret;
    }

    /**
     * Returns the timeout of a response.
     *
     * @return the timeout in milliseconds.
     */
    int getTimeoutMillis() {
        return timeout * 1000;
    }

    /**
     * Returns whether responses without a Message-Authenticator attribute are discarded.
     *
     * @return true if the Message-Authenticator attribute is required.
     */
    boolean isMessageAuthenticatorRequired() {
        return messageAuthenticatorRequired;
    }

    /**
     * Returns how the server is checked while it is offline.
     *
     * @return the health check type.
     */
    HealthCheckType getHealthCheckType() {
        return healthCheckType;
    }

    /**
     * Returns the time between the checks of the server while it is offline.
     *
     * @return the health check interval in milliseconds, at least one minute.
     */
    long getHealthCheckPeriod() {
        return Math.max(healthCheckInterval, 1) * 60000L;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RadiusServer)) {
            return false;
        }
        RadiusServer other = (RadiusServer) obj;
        return Objects.equals(host, other.host)
                && port == other.port
                && Objects.equals(secret, other.secret)
                && timeout == other.timeout
                && messageAuthenticatorRequired == other.messageAuthenticatorRequired
                && healthCheckType == other.healthCheckType
                && healthCheckInterval == other.healthCheckInterval;
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, secret, timeout, messageAuthenticatorRequired, healthCheckType,
                healthCheckInterval);
    }

    /**
     * Describes the server by its address, without the shared secret.
     *
     * @return the description.
     */
    @Override
    public String toString() {
        return "RADIUS server: " + host + ":" + port;
    }

}
