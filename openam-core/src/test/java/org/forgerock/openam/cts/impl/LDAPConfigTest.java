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
 * Copyright 2013-2017 ForgeRock AS.
 * Portions Copyright 2023 Wren Security
 */

package org.forgerock.openam.cts.impl;

import com.iplanet.am.util.SystemProperties;
import org.forgerock.opendj.ldap.DN;
import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

/**
 * @author robert.wapshott@forgerock.com
 */
public class LDAPConfigTest {

    private MockedStatic<SystemProperties> systemProperties;

    @BeforeMethod
    public void setup() {
        this.systemProperties = mockStatic(SystemProperties.class);
    }

    @AfterMethod
    public void cleanup() {
        this.systemProperties.close();
    }

    @Test
    public void shouldIndicateHasChanged() {
        // Given
        systemProperties.when(() -> SystemProperties.get("test-root-suffix")).thenReturn("badger");

        LDAPConfig config = new TestLDAPConfig();

        // Then
        assertThat(config.hasChanged()).isTrue();
    }

    @Test
    public void shouldIndicateHasNotChanged() {
        // Given
        systemProperties.when(() -> SystemProperties.get("test-root-suffix")).thenReturn(null);

        LDAPConfig config = new TestLDAPConfig();

        // Then
        assertThat(config.hasChanged()).isFalse();
    }

    @Test
    public void shouldReturnDefaultRootSuffix() {
        // Given
        systemProperties.when(() -> SystemProperties.get("test-root-suffix")).thenReturn(null);

        LDAPConfig config = new TestLDAPConfig();

        DN defaultRootSuffix = config.getDefaultRootSuffix();
        DN returnedRootSuffix = config.getTokenStoreRootSuffix();

        // Then
        assertEquals(defaultRootSuffix, returnedRootSuffix);
    }

    @Test
    public void shouldReturnExternalRootSuffix() {
        // Given
        systemProperties.when(() -> SystemProperties.get("test-root-suffix")).thenReturn("dc=google,dc=com");

        LDAPConfig config = new TestLDAPConfig();

        DN defaultRootSuffix = config.getDefaultRootSuffix();
        DN returnedRootSuffix = config.getTokenStoreRootSuffix();

        // Then
        assertNotEquals(defaultRootSuffix, returnedRootSuffix);
    }

    private static class TestLDAPConfig extends LDAPConfig {

        public TestLDAPConfig() {
            super("ou=unit-test");
        }

        @Override
        protected DN setDefaultTokenDNPrefix(DN root) {
            return root.child("ou=config-test");
        }

        @Override
        protected String getCustomTokenRootSuffixProperty() {
            return "test-root-suffix";
        }
    }
}
