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

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.EventTopicsMetaDataBuilder;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.handlers.json.JsonAuditEventHandler;
import org.forgerock.openam.audit.AuditEventHandlerFactory;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test the {@link JsonAuditEventHandlerFactory} class.
 */
public class JsonAuditEventHandlerFactoryTest {

    private AuditEventHandlerFactory factory;

    private EventTopicsMetaData eventTopicsMetaData;

    private String logDirName;

    @BeforeMethod
    public void setUp() throws IOException {
        factory = new JsonAuditEventHandlerFactory();
        eventTopicsMetaData = EventTopicsMetaDataBuilder.coreTopicSchemas().build();
        Path tempDir = Files.createTempDirectory("tmpJsonLogDir");
        logDirName = tempDir.toString();
        tempDir.toFile().deleteOnExit();
    }

    @Test
    private void shouldCreateJsonEventHandler() throws AuditException {
        // Given
        Map<String, Set<String>> configAttributes = new HashMap<>();
        configAttributes.put("enabled", singleton("true"));
        configAttributes.put("topics", singleton("access"));
        configAttributes.put("location", singleton(logDirName));
        configAttributes.put("maxSize", singleton("10000"));
        configAttributes.put("writeInterval", singleton("1000"));
        configAttributes.put("elasticsearchCompatible", singleton("false"));
        configAttributes.put("rotationRetentionCheckInterval", singleton("30"));
        configAttributes.put("rotationEnabled", singleton("true"));
        configAttributes.put("rotationMaxFileSize", singleton("100000000"));
        configAttributes.put("rotationFilePrefix", singleton("access"));
        configAttributes.put("rotationFileSuffix", singleton("-yyyy.MM.dd-HH.mm.ss"));
        configAttributes.put("rotationInterval", singleton("3600"));
        configAttributes.put("rotationTimes", singleton("1800"));
        configAttributes.put("retentionMaxNumberOfHistoryFiles", singleton("5"));
        configAttributes.put("retentionMaxDiskSpaceToUse", singleton("500000000"));
        configAttributes.put("retentionMinFreeSpaceRequired", singleton("100000000"));
        AuditEventHandlerConfiguration configuration = AuditEventHandlerConfiguration.builder().withName("JSON Handler")
                .withAttributes(configAttributes).withEventTopicsMetaData(eventTopicsMetaData).build();

        // When
        AuditEventHandler handler = factory.create(configuration);

        // Then
        assertThat(handler).isInstanceOf(JsonAuditEventHandler.class);
        assertThat(handler.getName()).isEqualTo("JSON Handler");
        assertThat(handler.getHandledTopics()).containsExactly("access");
        assertThat(handler.isEnabled()).isTrue();
    }

    @Test
    private void shouldCreateJsonEventHandlerWhenDisabled() throws AuditException {
        // Given
        Map<String, Set<String>> configAttributes = new HashMap<>();
        configAttributes.put("enabled", singleton("false"));
        configAttributes.put("topics", singleton("access"));
        configAttributes.put("location", singleton(logDirName));
        configAttributes.put("maxSize", singleton("10000"));
        configAttributes.put("writeInterval", singleton("1000"));
        configAttributes.put("elasticsearchCompatible", singleton("true"));
        configAttributes.put("rotationRetentionCheckInterval", singleton("30"));
        configAttributes.put("rotationEnabled", singleton("true"));
        configAttributes.put("rotationMaxFileSize", singleton("100000000"));
        configAttributes.put("rotationFilePrefix", singleton("access"));
        configAttributes.put("rotationFileSuffix", singleton("-yyyy.MM.dd-HH.mm.ss"));
        configAttributes.put("rotationInterval", singleton("3600"));
        configAttributes.put("rotationTimes", singleton("1800"));
        configAttributes.put("retentionMaxNumberOfHistoryFiles", singleton("5"));
        configAttributes.put("retentionMaxDiskSpaceToUse", singleton("500000000"));
        configAttributes.put("retentionMinFreeSpaceRequired", singleton("100000000"));
        AuditEventHandlerConfiguration configuration = AuditEventHandlerConfiguration.builder().withName("JSON Handler")
                .withAttributes(configAttributes).withEventTopicsMetaData(eventTopicsMetaData).build();

        // When
        AuditEventHandler handler = factory.create(configuration);

        // Then
        assertThat(handler).isInstanceOf(JsonAuditEventHandler.class);
        assertThat(handler.getName()).isEqualTo("JSON Handler");
        assertThat(handler.getHandledTopics()).containsExactly("access");
        assertThat(handler.isEnabled()).isFalse();
    }

}
