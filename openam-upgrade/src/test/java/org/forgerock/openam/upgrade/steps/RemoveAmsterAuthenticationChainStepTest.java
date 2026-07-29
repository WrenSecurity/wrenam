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

package org.forgerock.openam.upgrade.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wrensecurity.wrenam.test.AbstractMockBasedTest;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

public final class RemoveAmsterAuthenticationChainStepTest extends AbstractMockBasedTest {

    private static final String AMSTER_CHAIN_NAME = "amsterService";

    private static final String AUTH_CHAIN_SUB_CONFIG_NAME = "Configurations";

    private static final String DEFAULT_AMSTER_CHAIN_ATTRIBUTE_VALUE =
            "<AttributeValuePair><Value>Amster REQUIRED </Value></AttributeValuePair>";

    @Mock
    private PrivilegedAction<SSOToken> adminTokenAction;

    @Mock
    private ConnectionFactory connectionFactory;

    @Mock
    private ServiceConfig authChainsParentServiceConfig;

    @Mock
    private ServiceConfig authChainsServiceConfig;

    @Mock
    private ServiceConfig amsterChainServiceConfig;

    @Mock
    private ServiceConfig subrealmAuthChainsParentServiceConfig;

    @Mock
    private ServiceConfig subrealmAuthChainsServiceConfig;

    @Mock
    private ServiceConfig subrealmAmsterChainServiceConfig;

    private Map<String, ServiceConfig> authChainsParentServiceConfigsByRealm;

    private Set<String> realmNames;

    private MockedConstruction<ServiceConfigManager> serviceConfigManagerConstruction;

    private RemoveAmsterAuthenticationChainStep upgradeStep;

    @BeforeMethod
    public void setUp() throws Exception {
        authChainsParentServiceConfigsByRealm = new HashMap<>();
        authChainsParentServiceConfigsByRealm.put("/", authChainsParentServiceConfig);
        realmNames = new LinkedHashSet<>(Collections.singleton("/"));
        serviceConfigManagerConstruction = mockConstruction(ServiceConfigManager.class,
                (serviceConfigManager, context) ->
                        given(serviceConfigManager.getOrganizationConfig(anyString(), isNull()))
                                .willAnswer(invocation ->
                                        authChainsParentServiceConfigsByRealm.get(invocation.getArgument(0))));
        given(authChainsParentServiceConfig.getSubConfig(AUTH_CHAIN_SUB_CONFIG_NAME))
                .willReturn(authChainsServiceConfig);
        given(authChainsServiceConfig.getSubConfig(AMSTER_CHAIN_NAME)).willReturn(amsterChainServiceConfig);
        upgradeStep = spy(new RemoveAmsterAuthenticationChainStep(adminTokenAction, connectionFactory));
        doReturn(realmNames).when(upgradeStep).getRealmNames();
    }

    @AfterMethod
    public void tearDown() {
        serviceConfigManagerConstruction.close();
    }

    @Test
    public void shouldRemoveDefaultAmsterChainFromAllRealms() throws Exception {
        // Given
        realmNames.add("subrealm");
        authChainsParentServiceConfigsByRealm.put("subrealm", subrealmAuthChainsParentServiceConfig);
        given(amsterChainServiceConfig.getAttributesWithoutDefaultsForRead())
                .willReturn(createDefaultAmsterChainAttributes());
        given(subrealmAuthChainsParentServiceConfig.getSubConfig(AUTH_CHAIN_SUB_CONFIG_NAME))
                .willReturn(subrealmAuthChainsServiceConfig);
        given(subrealmAuthChainsServiceConfig.getSubConfig(AMSTER_CHAIN_NAME))
                .willReturn(subrealmAmsterChainServiceConfig);
        given(subrealmAmsterChainServiceConfig.getAttributesWithoutDefaultsForRead())
                .willReturn(createDefaultAmsterChainAttributes());

        // When
        upgradeStep.initialize();
        String detailedReportBeforeRemoval = upgradeStep.getDetailedReport("");
        upgradeStep.perform();

        // Then
        assertThat(upgradeStep.isApplicable()).isTrue();
        assertThat(detailedReportBeforeRemoval).isEmpty();
        assertThat(upgradeStep.getShortReport("")).isEqualTo("Remove default Amster authentication chain");
        assertThat(upgradeStep.getDetailedReport("\n")).isEqualTo(
                "Realm: /\n\tRemoved unmodified Amster authentication chain\n"
                        + "Realm: subrealm\n\tRemoved unmodified Amster authentication chain\n");
        verify(authChainsServiceConfig).removeSubConfig(AMSTER_CHAIN_NAME);
        verify(subrealmAuthChainsServiceConfig).removeSubConfig(AMSTER_CHAIN_NAME);
    }

    @Test
    public void shouldPreserveModifiedAmsterChainAndRemoveDefaultChainFromOtherRealm() throws Exception {
        // Given
        realmNames.add("subrealm");
        authChainsParentServiceConfigsByRealm.put("subrealm", subrealmAuthChainsParentServiceConfig);
        given(amsterChainServiceConfig.getAttributesWithoutDefaultsForRead())
                .willReturn(createAuthChainAttributes("Amster OPTIONAL "));
        given(subrealmAuthChainsParentServiceConfig.getSubConfig(AUTH_CHAIN_SUB_CONFIG_NAME))
                .willReturn(subrealmAuthChainsServiceConfig);
        given(subrealmAuthChainsServiceConfig.getSubConfig(AMSTER_CHAIN_NAME))
                .willReturn(subrealmAmsterChainServiceConfig);
        given(subrealmAmsterChainServiceConfig.getAttributesWithoutDefaultsForRead())
                .willReturn(createDefaultAmsterChainAttributes());

        // When
        upgradeStep.initialize();
        upgradeStep.perform();

        // Then
        assertThat(upgradeStep.isApplicable()).isTrue();
        assertThat(upgradeStep.getDetailedReport("\n")).isEqualTo(
                "Realm: subrealm\n\tRemoved unmodified Amster authentication chain\n");
        verify(authChainsServiceConfig, never()).removeSubConfig(AMSTER_CHAIN_NAME);
        verify(subrealmAuthChainsServiceConfig).removeSubConfig(AMSTER_CHAIN_NAME);
    }

    @Test
    public void shouldIgnoreMissingAuthChainsParentServiceConfig() throws Exception {
        // Given
        authChainsParentServiceConfigsByRealm.put("/", null);

        // When
        upgradeStep.initialize();

        // Then
        assertThat(upgradeStep.isApplicable()).isFalse();
        verify(authChainsServiceConfig, never()).removeSubConfig(AMSTER_CHAIN_NAME);
    }

    @Test
    public void shouldIgnoreMissingAuthChainsServiceConfig() throws Exception {
        // Given
        given(authChainsParentServiceConfig.getSubConfig(AUTH_CHAIN_SUB_CONFIG_NAME)).willReturn(null);

        // When
        upgradeStep.initialize();

        // Then
        assertThat(upgradeStep.isApplicable()).isFalse();
        verify(authChainsServiceConfig, never()).removeSubConfig(AMSTER_CHAIN_NAME);
    }

    @Test
    public void shouldIgnoreMissingAmsterChainServiceConfig() throws Exception {
        // Given
        given(authChainsServiceConfig.getSubConfig(AMSTER_CHAIN_NAME)).willReturn(null);

        // When
        upgradeStep.initialize();

        // Then
        assertThat(upgradeStep.isApplicable()).isFalse();
        verify(authChainsServiceConfig, never()).removeSubConfig(AMSTER_CHAIN_NAME);
    }

    @Test(dataProvider = "provideNonDefaultAmsterChainAttributes")
    public void shouldPreserveNonDefaultAmsterChain(Map<String, Set<String>> attributes) throws Exception {
        // Given
        given(amsterChainServiceConfig.getAttributesWithoutDefaultsForRead()).willReturn(attributes);

        // When
        upgradeStep.initialize();

        // Then
        assertThat(upgradeStep.isApplicable()).isFalse();
        verify(authChainsServiceConfig, never()).removeSubConfig(AMSTER_CHAIN_NAME);
    }

    @Test
    public void shouldPreserveAmsterChainModifiedAfterInitialization() throws Exception {
        // Given
        given(amsterChainServiceConfig.getAttributesWithoutDefaultsForRead())
                .willReturn(createDefaultAmsterChainAttributes(), createAuthChainAttributes("Amster OPTIONAL "));

        // When
        upgradeStep.initialize();
        upgradeStep.perform();

        // Then
        assertThat(upgradeStep.isApplicable()).isTrue();
        assertThat(upgradeStep.getDetailedReport("")).isEmpty();
        verify(authChainsServiceConfig, never()).removeSubConfig(AMSTER_CHAIN_NAME);
    }

    @DataProvider
    private Object[][] provideNonDefaultAmsterChainAttributes() {
        Map<String, Set<String>> attributesWithAdditionalSetting =
                new HashMap<>(createDefaultAmsterChainAttributes());
        attributesWithAdditionalSetting.put(
                "iplanet-am-auth-login-success-url", Collections.singleton("/after-login"));

        return new Object[][] {
            {Collections.emptyMap()},
            {createAuthChainAttributes("DataStore REQUIRED ")},
            {createAuthChainAttributes("Amster OPTIONAL ")},
            {createAuthChainAttributes("Amster REQUIRED option=value")},
            {createAuthChainAttributes("Amster REQUIRED </Value><Value>DataStore REQUIRED ")},
            {createAuthChainAttributes("not XML")},
            {createAuthChainAttributes(" Amster REQUIRED ")},
            {attributesWithAdditionalSetting}
        };
    }

    private Map<String, Set<String>> createDefaultAmsterChainAttributes() {
        return Collections.singletonMap(AMAuthConfigUtils.ATTR_NAME,
                Collections.singleton(DEFAULT_AMSTER_CHAIN_ATTRIBUTE_VALUE));
    }

    private Map<String, Set<String>> createAuthChainAttributes(String chainConfiguration) {
        return Collections.singletonMap(AMAuthConfigUtils.ATTR_NAME,
                Collections.singleton("<AttributeValuePair><Value>" + chainConfiguration
                        + "</Value></AttributeValuePair>"));
    }

}
