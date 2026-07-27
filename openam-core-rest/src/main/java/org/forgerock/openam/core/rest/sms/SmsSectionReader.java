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
 * Copyright 2026 Wren Security.
 */

package org.forgerock.openam.core.rest.sms;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceSchema;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

 /**
  * A reader for SMS schema section layouts.
  */
final class SmsSectionReader {

    private SmsSectionReader() {
    }

    /**
     * Read the sections associated with attributes in an SMS schema.
     *
     * @param schema schema whose section resource should be read
     * @param debug caller's AM debug instance
     * @return section names keyed by attribute name in declaration order, or an empty map when no section resource
     *         can be read
     */
    static Map<String, String> readSectionsByAttributeName(ServiceSchema schema, Debug debug) {
        String schemaName = schema.getName();
        if (SmsRequestHandler.USE_PARENT_PATH.equals(schema.getResourceName()) || schemaName == null) {
            schemaName = schema.getServiceName();
        }

        String filename = schemaName + ".section.properties";
        Map<String, String> result = new LinkedHashMap<>();
        InputStream inputStream = SmsSectionReader.class.getClassLoader().getResourceAsStream(filename);
        if (inputStream == null) {
            return result;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String entry = line.trim();
                if (entry.isEmpty() || entry.startsWith("#")) {
                    continue;
                }
                String[] sectionAndAttribute = entry.split("=", 2);
                if (sectionAndAttribute.length == 2) {
                    result.put(sectionAndAttribute[1].trim(), sectionAndAttribute[0].trim());
                }
            }
        } catch (IOException e) {
            if (debug.errorEnabled()) {
                debug.error("Error reading section properties file " + filename, e);
            }
        }
        return result;
    }

}
