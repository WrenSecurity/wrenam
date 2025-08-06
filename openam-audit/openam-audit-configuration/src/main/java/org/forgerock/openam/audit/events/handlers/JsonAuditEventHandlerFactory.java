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
 * Copyright 2025 Wren Security. All rights reserved.
 */
package org.forgerock.openam.audit.events.handlers;

import static com.iplanet.am.util.SystemProperties.CONFIG_PATH;
import static com.sun.identity.shared.Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR;
import static com.sun.identity.shared.datastruct.CollectionHelper.getBooleanMapAttr;
import static com.sun.identity.shared.datastruct.CollectionHelper.getIntMapAttr;
import static com.sun.identity.shared.datastruct.CollectionHelper.getLongMapAttr;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.debug.Debug;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Singleton;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.handlers.json.JsonAuditEventHandler;
import org.forgerock.audit.handlers.json.JsonAuditEventHandlerConfiguration;
import org.forgerock.audit.handlers.json.JsonAuditEventHandlerConfiguration.EventBufferingConfiguration;
import org.forgerock.openam.audit.AuditEventHandlerFactory;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfiguration;

/**
 * This factory is responsible for creating an instance of the {@link JsonAuditEventHandler}.
 */
@Singleton
public final class JsonAuditEventHandlerFactory implements AuditEventHandlerFactory {

    private static final Debug DEBUG = Debug.getInstance("amAudit");

    @Override
    public AuditEventHandler create(final AuditEventHandlerConfiguration configuration) throws AuditException {
        Map<String, Set<String>> attributes = configuration.getAttributes();
        JsonAuditEventHandlerConfiguration jsonHandlerConfiguration = new JsonAuditEventHandlerConfiguration();
        jsonHandlerConfiguration.setLogDirectory(getLogDirectory(attributes));
        jsonHandlerConfiguration.setTopics(attributes.get("topics"));
        jsonHandlerConfiguration.setName(configuration.getHandlerName());
        jsonHandlerConfiguration.setEnabled(getBooleanMapAttr(attributes, "enabled", true));
        jsonHandlerConfiguration.setElasticsearchCompatible(
                getBooleanMapAttr(attributes, "elasticsearchCompatible", false));
        jsonHandlerConfiguration.setRotationRetentionCheckInterval(
                getIntMapAttr(attributes, "rotationRetentionCheckInterval", 20, DEBUG) + " s");
        setFileRotationPolicies(jsonHandlerConfiguration, attributes);
        setFileRetentionPolicies(jsonHandlerConfiguration, attributes);
        jsonHandlerConfiguration.setBuffering(getBufferingConfiguration(attributes));
        return new JsonAuditEventHandler(jsonHandlerConfiguration, configuration.getEventTopicsMetaData());
    }

    private String getLogDirectory(Map<String, Set<String>> attributes) {
        String location = getMapAttr(attributes, "location", "");
        String baseDir = SystemProperties.get(CONFIG_PATH);
        if (baseDir != null) {
            location = location.replace("%BASE_DIR%", baseDir);
        }
        String serverUri = SystemProperties.get(AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        if (serverUri != null) {
            location = location.replace("%SERVER_URI%", serverUri);
        }
        return location;
    }

    private void setFileRotationPolicies(JsonAuditEventHandlerConfiguration jsonHandlerConfiguration,
            Map<String, Set<String>> attributes) throws AuditException {
        jsonHandlerConfiguration.getFileRotation()
                .setRotationEnabled(getBooleanMapAttr(attributes, "rotationEnabled", true));
        jsonHandlerConfiguration.getFileRotation()
                .setMaxFileSize(getLongMapAttr(attributes, "rotationMaxFileSize", 100000000L, DEBUG));
        jsonHandlerConfiguration.getFileRotation()
                .setRotationFilePrefix(getMapAttr(attributes, "rotationFilePrefix", ""));
        jsonHandlerConfiguration.getFileRotation()
                .setRotationFileSuffix(getMapAttr(attributes, "rotationFileSuffix", "-yyyy.MM.dd-HH.mm.ss"));
        jsonHandlerConfiguration.getFileRotation()
                .setRotationInterval(parseRotationInterval(getMapAttr(attributes, "rotationInterval", "-1")));

        List<String> times = new ArrayList<>();
        Set<String> rotationTimesAttribute = attributes.get("rotationTimes");
        if (rotationTimesAttribute != null && !rotationTimesAttribute.isEmpty()) {
            for (String rotationTime : rotationTimesAttribute) {
                times.add(rotationTime + " seconds");
            }
            jsonHandlerConfiguration.getFileRotation().setRotationTimes(times);
        }
    }

    private String parseRotationInterval(String interval) throws AuditException {
        try {
            Long intervalAsLong = Long.valueOf(interval);

            if (intervalAsLong <= 0) {
                // If interval is 0 or a negative value, the feature is disabled
                return "disabled";
            } else {
                // If interval is a positive number, add seconds as the time unit
                return interval + " seconds";
            }
        } catch (NumberFormatException nfe) {
            throw new AuditException("Attribute 'rotationInterval' is invalid: " + interval);
        }
    }

    private void setFileRetentionPolicies(JsonAuditEventHandlerConfiguration jsonHandlerConfiguration,
            Map<String, Set<String>> attributes) {
        jsonHandlerConfiguration.getFileRetention()
                .setMaxNumberOfHistoryFiles(getIntMapAttr(attributes, "retentionMaxNumberOfHistoryFiles", 1, DEBUG));
        jsonHandlerConfiguration.getFileRetention()
                .setMaxDiskSpaceToUse(getLongMapAttr(attributes, "retentionMaxDiskSpaceToUse", -1L, DEBUG));
        jsonHandlerConfiguration.getFileRetention()
                .setMinFreeSpaceRequired(getLongMapAttr(attributes, "retentionMinFreeSpaceRequired", -1L, DEBUG));
    }

    private EventBufferingConfiguration getBufferingConfiguration(Map<String, Set<String>> attributes) {
        EventBufferingConfiguration bufferingConfiguration = new EventBufferingConfiguration();
        bufferingConfiguration.setMaxSize(getIntMapAttr(attributes, "maxSize", 10000, DEBUG));
        bufferingConfiguration.setWriteInterval(getIntMapAttr(attributes, "writeInterval", 1000, DEBUG) + " millis");
        return bufferingConfiguration;
    }

}
