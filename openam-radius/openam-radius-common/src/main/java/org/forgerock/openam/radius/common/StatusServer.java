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

/**
 * A Status-Server packet as specified in RFC 5997, which asks a server whether it is alive without performing an
 * authentication. Sent to the authentication port, it is answered with an Access-Accept. It must carry a
 * Message-Authenticator attribute.
 */
public class StatusServer extends Packet {

    /**
     * Constructs a Status-Server packet without an identifier and authenticator, used when parsing packets.
     */
    public StatusServer() {
        super(PacketType.STATUS_SERVER);
    }

    /**
     * Constructs a Status-Server packet.
     *
     * @param id
     *            the packet identifier.
     * @param auth
     *            the request authenticator.
     */
    public StatusServer(short id, Authenticator auth) {
        super(PacketType.STATUS_SERVER, id, auth);
    }
}
