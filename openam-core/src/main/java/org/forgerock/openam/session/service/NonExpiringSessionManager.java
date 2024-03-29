/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 * Portions Copyright 2023 Wren Security.
 */

package org.forgerock.openam.session.service;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.sun.identity.shared.debug.Debug;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;

/**
 * This class tracks sessions created by this server which are not set to expire and updates it to keep idle time
 * bellow allowed limit. If the server goes offline, the sessions created for it will eventually be removed.
 */
class NonExpiringSessionManager {

    private static final Debug DEBUG = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    /**
     * Internal (in minutes) to periodically refresh non-expiring session validity
     * This has to be less than {@link InternalSession#NON_EXPIRING_SESSION_MAX_IDLE_TIME}.
     */
    private static final long SESSION_REFRESH_INTERVAL = 5;

    private final Set<SessionID> nonExpiringSessions = new CopyOnWriteArraySet<>();

    private final SessionAccessManager sessionAccessManager;

    /**
     * Create a new manager for non expiring sessions. Should only be used by the SessionAccessManager.
     * @param sessionAccessManager The session access manager this uses to recover sessions.
     * @param threadMonitor The thread monitor used to restart the underlying thread on failure.
     */
    NonExpiringSessionManager(SessionAccessManager sessionAccessManager,
                              ScheduledExecutorService scheduler,
                              ThreadMonitor threadMonitor) {
        this.sessionAccessManager = sessionAccessManager;

        NonExpiringSessionUpdater sessionUpdater = new NonExpiringSessionUpdater();
        threadMonitor.watchScheduledThread(scheduler, sessionUpdater, 0, SESSION_REFRESH_INTERVAL, TimeUnit.MINUTES);
    }

    /**
     * Adds a session to be managed. This operation can not be undone.
     * @param session The non expiring session to manage.
     */
    void addNonExpiringSession(InternalSession session) {
        if (session.willExpire()) {
            throw new IllegalStateException("Tried to add session which would expire to NonExpiringSessionManager");
        }
        updateSession(session);
        DEBUG.message("Registering non-expiring session for '{}'", session.getUUID());
        nonExpiringSessions.add(session.getID());
    }

    private void updateSession(InternalSession session) {
        if (session == null) {
            return;
        }
        session.setLatestAccessTime();
    }

    private class NonExpiringSessionUpdater implements Runnable {

        @Override
        public void run() {
            for (SessionID sessionID : nonExpiringSessions) {
                updateSession(sessionAccessManager.getInternalSession(sessionID));
            }
        }
    }
}
