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
 * $Id: UpgradeUtils.java,v 1.18 2009/09/30 17:35:24 goodearth Exp $
 *
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 * Portions Copyright 2023 Wren Security.
 */
package org.forgerock.openam.upgrade;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

/**
 * Utility class that deals with determining and comparing versions of OpenAM.
 *
 * @since 13.0.0
 */
public class VersionUtils {

    private static final Debug DEBUG = Debug.getInstance("amUpgrade");

    /**
     * Cached result of version evaluation.
     */
    private static Boolean isVersionNewer;

    /**
     * @see #isVersionNewer(String, String)
     */
    public static boolean isVersionNewer() {
        if (isVersionNewer == null) {
            // Cache result to avoid repeated evaluations
            isVersionNewer = isVersionNewer(getCurrentVersion(), getWarFileVersion());
        }
        return isVersionNewer;
    }

    /**
     * Check whether version of the WAR file is newer than the one currently installed.
     *
     * @param currentValue Serialized version of the currently installed instance. Can be null.
     * @param warValue Serialized version of the WAR file. Can be null.
     * @return true when the WAR file version is newer than the currently installed version, false otherwise.
     */
    public static boolean isVersionNewer(String currentValue, String warValue) {
        // Ignore version check when the upgrade is globally disabled
        if (SystemProperties.get("org.forgerock.donotupgrade") != null) {
            return false;
        }
        // Parse serialized version values
        ParsedVersion currentVersion = ParsedVersion.parse(currentValue);
        ParsedVersion warVersion = ParsedVersion.parse(warValue);
        if (currentVersion == null || warVersion == null) {
            DEBUG.warning("Unable to determine versions for upgrade; current: {}, war: {}",
                    currentValue, warValue);
            return false;
        }
        // Compare version numbers
        return currentVersion.compareTo(warVersion) < 0;
    }

    /**
     * Get version string for the current installation.
     *
     * @return Version string. Can be null if unable to retrieve.
     */
    public static String getCurrentVersion() {
        return SystemProperties.get(Constants.AM_VERSION);
    }

    /**
     * Get version from the deployed WAR file.
     *
     * @return Version string. Can be null in unable to retrieve.
     */
    public static String getWarFileVersion() {
        return ServerConfiguration.getWarFileVersion();
    }

    /**
     * Check if the currently installed version has the same version as the expected version number.
     *
     * @param version Version number to check. Never null.
     * @return True if the current version is the same as the installed one, false otherwise.
     */
    public static boolean isCurrentVersionEqualTo(String version) {
        ParsedVersion currentVersion = ParsedVersion.parse(getCurrentVersion());
        if (currentVersion == null) {
            return false;
        }
        ParsedVersion compareVersion = ParsedVersion.parse(version);
        if (compareVersion == null) {
            return false;
        }
        return currentVersion.compareTo(compareVersion) == 0;
    }

    /**
     * Check if the currently installed version is less than the specified version.
     *
     * @param version Version number to check. Never null.
     * @param fallback Fallback value to return if the current version cannot be determined.
     * @return True if the current version is less than the given one, false otherwise.
     */
    public static boolean isCurrentVersionLessThan(String version, boolean fallback) {
        ParsedVersion currentVersion = ParsedVersion.parse(getCurrentVersion());
        if (currentVersion == null) {
            return fallback;
        }
        ParsedVersion compareVersion = ParsedVersion.parse(version);
        if (compareVersion == null) {
            return fallback;
        }
        return currentVersion.compareTo(compareVersion) < 0;
    }

}
