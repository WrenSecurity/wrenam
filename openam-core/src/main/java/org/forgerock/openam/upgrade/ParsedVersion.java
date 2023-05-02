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
 * Copyright 2023 Wren Security.
 */
package org.forgerock.openam.upgrade;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Parsed version information value class.
 *
 * <p>
 * This class holds information parsed from version string in the form
 * <code>${PRODUCT} ${VERSION} Build ${REVISION} (${DATESTAMP})</code> (see
 * openam-server-only/pom.xml file) and implements {@link Comparable} to allow
 * comparing different product versions to assist upgrade process execution.
 * </p>
 *
 * <p>
 * Version parsing logic is deliberately lenient to allow processing of missing version
 * and/or legacy version formats.
 * </p>
 *
 * @since 15.0.0
 */
public final class ParsedVersion implements Comparable<ParsedVersion> {

    /**
     * Pattern to match serialized version with two capture groups (numeric version and build date).
     *
     * <p>
     * Expected format is <code>${PRODUCT} ${VERSION} Build ${REVISION} (${DATESTAMP})</code>.
     * </p>
     */
    private static final Pattern VERSION_FORMAT_PATTERN = Pattern.compile("^(?:.*?(\\d+\\.\\d+\\.?\\d*))?[^\\(]*(?:\\((.*)\\))?");

    /**
     * Build date time formatter that supports new ISO format and legacy UK format.
     */
    private static final DateTimeFormatter BUILD_DATE_FORMATTER = new DateTimeFormatterBuilder()
            // Add normalized ISO date format
            .appendOptional(DateTimeFormatter.ISO_DATE_TIME)
            // Add legacy build date format
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MMMM-dd HH:mm"))
            // Define locale for legacy date format that uses month names
            .toFormatter(Locale.UK)
            .withZone(ZoneOffset.UTC);


    private final int[] versionNumber;
    private final Instant buildDate;

    private ParsedVersion(int[] versionNumber, Instant buildDate) {
        this.versionNumber = versionNumber;
        this.buildDate = buildDate;
    }

    /**
     * Get build date if defined.
     *
     * @return Build date or null if not defined.
     */
    public Instant getBuildDate() {
        return buildDate;
    }

    /**
     * Get version number (should be semantic version in <code>major.minor.patch</code> format).
     *
     * @return Version number or -1 if not defined.
     */
    public String getVersionNumber() {
        return IntStream.of(versionNumber).mapToObj(String::valueOf).collect(Collectors.joining("."));
    }

    /**
     * Parse version string in the form <code>${PRODUCT} ${VERSION} Build ${REVISION} (${DATESTAMP})</code>.
     *
     * <p>
     * This method is deliberately lenient to allow processing of missing version
     * and/or legacy version format. Callers have to handle <code>null</code> results.
     * </p>
     * @param value Full version string. Can be null.
     * @return Version number and build date. Can be null if unable to parse.
     */
    public static ParsedVersion parse(String value) {
        // This can happen if the version is not defined (ideally should be handled by the caller)
        if (value == null) {
            return null;
        }
        Matcher matcher = VERSION_FORMAT_PATTERN.matcher(value);
        // We want to ignore invalid version string
        if (!matcher.matches() || matcher.group(1) == null && matcher.group(2) == null) {
            return null; // Missing both version number and build date
        }
        // Parse version number
        int[] versionNumber = matcher.group(1) != null
                ? Stream.of(matcher.group(1).split("\\.")).mapToInt(Integer::parseInt).toArray()
                : new int[] { -1 };
        // Parse build date
        Instant buildDate = null;
        try {
            if (matcher.group(2) != null) {
                buildDate = Instant.from(BUILD_DATE_FORMATTER.parse(matcher.group(2)));
            }
        } catch (DateTimeParseException e) { /* ignore invalid date */ }
        return new ParsedVersion(versionNumber, buildDate);
    }

    @Override
    public int compareTo(ParsedVersion o) {
        int versionCompare = Arrays.compare(versionNumber, o.versionNumber);
        return versionCompare == 0 && buildDate != null && o.buildDate != null ?
                buildDate.compareTo(o.buildDate) : versionCompare;
    }

}
