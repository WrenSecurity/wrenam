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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // Pattern to match serialized version with two capture groups (numeric version and build date)
    private static final Pattern VERSION_FORMAT_PATTERN = Pattern.compile("^(?:.*?(\\d+\\.\\d+\\.?\\d*).*)?\\((.*)\\)");
    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder().
            appendOptional(DateTimeFormatter.ISO_DATE_TIME).
            appendOptional(DateTimeFormatter.ofPattern("yyyy-MMMM-dd HH:mm")). // Legacy build date format
            toFormatter(Locale.US).
            withZone(ZoneOffset.UTC);;

    private static volatile boolean evaluatedUpgradeVersion = false;
    private static boolean isVersionNewer = false;


    /**
     * @see #isVersionNewer(String, String).
     */
    public static boolean isVersionNewer() {
        if (!evaluatedUpgradeVersion) {
            // Cache result to avoid repeated evaluations
            isVersionNewer = isVersionNewer(getCurrentVersion(), getWarFileVersion());
            evaluatedUpgradeVersion = true;
        }
        return isVersionNewer;
    }

    /**
     * Check whether version of the war file is newer than the one currently deployed.
     * @param currentValue Serialized version of the currently deployed instance. Can be null.
     * @param warValue Serialized version of WAR file. Can be null.
     * @return true when version is newer, false otherwise.
     */
    public static boolean isVersionNewer(String currentValue, String warValue) {
        // Ignore version check when upgrade is globally disabled
        if (SystemProperties.get("org.forgerock.donotupgrade") != null) {
            return false;
        }
        // Parsed serialized values
        Entry<Integer, ZonedDateTime> current = parseVersion(currentValue);
        Entry<Integer, ZonedDateTime> war = parseVersion(warValue);
        if (current == null || war == null) {
            return false;
        }
        // Compare numeric versions when different
        if (!current.getKey().equals(war.getKey())) {
            return current.getKey() < war.getKey();
        }
        // Compare build dates
        return current.getValue() != null && war.getValue() != null && war.getValue().isAfter(current.getValue());
    }

    public static String getCurrentVersion() {
        return SystemProperties.get(Constants.AM_VERSION);
    }

    public static String getWarFileVersion() {
        return ServerConfiguration.getWarFileVersion();
    }

    /**
     * Check if the currently deployed Wren:AM has the same version number as the expected version number.
     * @param expectedVersion Expected version to check. Never null.
     * @return <code>false</code> if the version number cannot be detected or if the local version does not match,
     * <code>true</code> otherwise.
     */
    public static boolean isCurrentVersionEqualTo(Integer expectedVersion) {
        Entry<Integer, ZonedDateTime> current = parseVersion(getCurrentVersion());
        if (current == null) {
            return false;
        }
        return expectedVersion.equals(current.getKey());
    }

    /**
     * Check if the currently deployed Wren:AM version is less than the specified version.
     * @param version Version to check.
     * @param notParsed The value to return if the current version cannot be parsed.
     */
    public static boolean isCurrentVersionLessThan(int version, boolean notParsed) {
        Entry<Integer, ZonedDateTime> current = parseVersion(getCurrentVersion());
        if (current == null) {
            return notParsed;
        }
        return current.getKey() < version;
    }

    /**
     * Extract version represented as integer value (e.g. '15.0.0' -> 1500 ) and build date from the specified serialized value.
     */
    private static Entry<Integer, ZonedDateTime> parseVersion(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = VERSION_FORMAT_PATTERN.matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        Integer version = matcher.group(1) != null ? Integer.valueOf(matcher.group(1).replace(".", "")) : -1;
        ZonedDateTime buildDate = null;
        try {
            buildDate = ZonedDateTime.parse(matcher.group(2), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            DEBUG.error("Failed to parse build date: '" + value + "' during version check.", e);
        }
        return new SimpleEntry<>(version, buildDate);
    }

}
