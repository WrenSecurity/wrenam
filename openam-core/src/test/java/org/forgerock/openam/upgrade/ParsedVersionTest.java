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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.time.Instant;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * {@link ParsedVersion} test case.
 */
public class ParsedVersionTest {

    @DataProvider(name = "parse")
    public Object[] getParseData() {
        return new Object[][] {
            { "invalid", null, null },
            { "Wren:AM 15.0.0", "15.0.0", null },
            { "Wren:AM 15.0.0-SNAPSHOT", "15.0.0", null },
            { "Wren:AM 15.0.0 (2023-01-01T00:00:00Z)", "15.0.0", Instant.parse("2023-01-01T00:00:00Z") },
            { "Wren:AM 15.0.0 Build foobar (2023-01-01T00:00:00Z)", "15.0.0", Instant.parse("2023-01-01T00:00:00Z") },
            { "OpenAM 13.0.0 Build foobar (2023-January-01 00:00)", "13.0.0", Instant.parse("2023-01-01T00:00:00Z") },
        };
    }

    @Test(dataProvider =  "parse")
    public void testParse(String value, String versionNumber, Instant buildDate) {
        ParsedVersion version = ParsedVersion.parse(value);
        if (version == null) {
            assertNull(versionNumber);
        } else {
            assertEquals(version.getVersionNumber(), versionNumber);
            assertEquals(version.getBuildDate(), buildDate);
        }
    }

    @DataProvider(name = "compare")
    public Object[][] getCompareData() {
        return new Object[][] {
            // Different version, with version specifier, missing revision number, with legacy date format
            { "OpenAM 10.0.0 (2012-April-13 10:24)", "OpenAM 9.5.5-RC1 (2012-March-05 03:13)", 1 },
            // Different version, invalid version specifier, with revision number, with legacy date format
            { "9.5.3_RC1 Build 753 (2011-May-06 10:55)", "OpenAM 10.0.0 (2012-April-13 10:24)", -1 },
            // Same version, with version specifier, missing revision number, with legacy date format
            { "OpenAM 10.0.0-EA (2012-February-07 00:14)", "OpenAM 10.0.0 (2012-April-13 10:24)", -1 },
            // Different version numbers
            { "Wren:AM 15.0.0 (2023-04-13T10:24:21Z)", "Wren:AM 15.0.1-SNAPSHOT (2023-06-17T10:24:44Z)", -1 },
            // Same version, different dates
            { "Wren:AM 15.0.0 (2023-04-13T10:24:21Z)", "Wren:AM 15.0.0 (2023-06-17T10:24:44Z)", -1 },
            // Different product names
            { "OpenAM 12.0.0-SNAPSHOT Build 2000 (2013-March-13 08:43)", "Wren:AM 15.0.0-M2 Build 5dba72a2f9 (2023-02-22T13:35:15Z)", -1},
            // Invalid version string
            { "Wren:AM 15.0.0 (2023-04-13T10:24:21Z)", "", null },
            // Invalid build date
            { "Wren:AM 15.0.1 (Foo)", "Wren:AM 15.0.0 (Bar)", 1 },
            { "Wren:AM 15.0.0 (Foo)", "Wren:AM 15.0.0 (Bar)", 0 },
            // Equal versions
            { "Wren:AM 15.0.0-M2 Build 5dba72a2f9 (2023-02-22T13:35:15Z)", "Wren:AM 15.0.0-M2 Build 5dba72a2f9 (2023-02-22T13:35:15Z)", 0 },
            { "Wren:AM 15.0.0", "Wren:AM 15.0.0", 0 },
        };
    }

    @Test(dataProvider = "compare")
    public void testVersionCompare(String first, String second, Integer result) {
        ParsedVersion firstVersion = ParsedVersion.parse(first);
        ParsedVersion secondVersion = ParsedVersion.parse(second);
        if (firstVersion == null || secondVersion == null) {
            assertNull(result, "unparsable version expected");
        } else {
            assertEquals(Integer.signum(firstVersion.compareTo(secondVersion)), result.intValue());
            assertEquals(Integer.signum(secondVersion.compareTo(firstVersion)), -result.intValue());
        }
    }

}
