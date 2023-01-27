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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 * Portions Copyrighted 2016 Agile Digital Engineering
 * Portions Copyrighted 2022 Wren Security
 */

package org.forgerock.openam.sts.soap.publish;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ws.security.sts.provider.SecurityTokenServiceProvider;
import org.assertj.core.api.Assertions;
import org.wrensecurity.guava.common.collect.Sets;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.soap.config.user.SoapDeploymentConfig;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;
import org.forgerock.openam.sts.soap.config.user.SoapSTSKeystoreConfig;
import org.forgerock.openam.sts.soap.healthcheck.HealthCheck;
import org.forgerock.openam.sts.soap.healthcheck.HealthCheckImpl;
import org.mockito.ArgumentMatchers;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Note that the process of unit-testing the SoapSTSInstancePublisherImpl involves ultimately creating the SoapSTSInstanceModule
 * corresponding to a to-be-published soap-sts instance. Exceptions in this process are logged in the SoapSTSInstancePublisherImpl#run
 * method, but they don't show up in the maven output. Test errors in this class are almost always a function of missing
 * guice configurations, which can be best debugged by putting a breakpoint in SoapSTSInstancePublisherImpl#publishInstance,
 * so that the guice errors can be examined directly, as these errors are logged to the OpenAM debug log context, and thus
 * don't seem to surface in the maven output.
 */
public class SoapSTSInstancePublisherImplTest {

    private SoapSTSInstanceLifecycleManager mockLifecycleManager;
    private PublishServiceConsumer mockPublishServiceConsumer;
    private SoapSTSInstancePublisher instancePublisher;
    private Server mockServer;
    private HealthCheck healthCheck;

    class MyModule extends AbstractModule {
        @Override
        protected void configure() {
            mockLifecycleManager = mock(SoapSTSInstanceLifecycleManager.class);
            mockPublishServiceConsumer = mock(PublishServiceConsumer.class);
            mockServer = mock(Server.class);
            Logger mockLogger = mock(Logger.class);
            bind(SoapSTSInstanceLifecycleManager.class).toInstance(mockLifecycleManager);
            bind(PublishServiceConsumer.class).toInstance(mockPublishServiceConsumer);
            bind(Logger.class).toInstance(mockLogger);
            bind(SoapSTSInstancePublisher.class).to(SoapSTSInstancePublisherImpl.class);
            bind(HealthCheck.class).to(HealthCheckImpl.class).in(Scopes.SINGLETON);
        }
    }

    //need to re-create the bindings before each test method because invocations against a mock being calculated.
    @BeforeMethod
    public void setUp() {
        Injector injector = Guice.createInjector(new MyModule());
        instancePublisher = injector.getInstance(SoapSTSInstancePublisher.class);
        healthCheck = injector.getInstance(HealthCheck.class);
    }
    @SuppressWarnings("unchecked")
    @Test
    public void testPublishAndRemove() throws ResourceException, UnsupportedEncodingException {
        Assertions.assertThat(healthCheck.getNumPublishedInstances()).isEqualTo(0);
        Set<SoapSTSInstanceConfig> initialSet = Sets.newHashSet(createInstanceConfig("instanceOne",
                "http://host.com:8080/am"));
        when(mockPublishServiceConsumer.getPublishedInstances()).thenReturn(initialSet);
        when(mockLifecycleManager.exposeSTSInstanceAsWebService(
                any(Map.class), any(SecurityTokenServiceProvider.class), any(SoapSTSInstanceConfig.class))).thenReturn(mockServer);
        instancePublisher.run();
        verify(mockPublishServiceConsumer, times(1)).getPublishedInstances();
        verify(mockLifecycleManager, times(1)).exposeSTSInstanceAsWebService(any(Map.class),
                any(SecurityTokenServiceProvider.class), any(SoapSTSInstanceConfig.class));
        Assertions.assertThat(healthCheck.getNumPublishedInstances()).isEqualTo(1);

        when(mockPublishServiceConsumer.getPublishedInstances()).thenReturn(Sets.<SoapSTSInstanceConfig>newHashSet());
        instancePublisher.run();
        verify(mockPublishServiceConsumer, times(2)).getPublishedInstances();
        verify(mockLifecycleManager, times(1)).exposeSTSInstanceAsWebService(any(Map.class),
                any(SecurityTokenServiceProvider.class), any(SoapSTSInstanceConfig.class));
        verify(mockLifecycleManager, times(1)).destroySTSInstance(any(Server.class));
        Assertions.assertThat(healthCheck.getNumPublishedInstances()).isEqualTo(0);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNoUpdate() throws ResourceException, UnsupportedEncodingException {
        SoapSTSInstanceConfig instanceConfig = createInstanceConfig("instanceOne",
                "http://host.com:8080/am");
        Set<SoapSTSInstanceConfig> initialSet = Sets.newHashSet(instanceConfig);
        when(mockPublishServiceConsumer.getPublishedInstances()).thenReturn(initialSet);
        when(mockLifecycleManager.exposeSTSInstanceAsWebService(
                any(Map.class), any(SecurityTokenServiceProvider.class), any(SoapSTSInstanceConfig.class))).thenReturn(mockServer);
        instancePublisher.run();
        verify(mockLifecycleManager, times(1)).exposeSTSInstanceAsWebService(any(Map.class),
                any(SecurityTokenServiceProvider.class), any(SoapSTSInstanceConfig.class));

        when(mockPublishServiceConsumer.getPublishedInstances()).thenReturn(initialSet);
        instancePublisher.run();
        verify(mockLifecycleManager, times(1)).exposeSTSInstanceAsWebService(any(Map.class),
                any(SecurityTokenServiceProvider.class), any(SoapSTSInstanceConfig.class));
        verify(mockLifecycleManager, times(0)).destroySTSInstance(any(Server.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdate() throws ResourceException, UnsupportedEncodingException {
        SoapSTSInstanceConfig instanceConfig = createInstanceConfig("instanceOne",
                "http://host.com:8080/am");
        Set<SoapSTSInstanceConfig> initialSet = Sets.newHashSet(instanceConfig);
        when(mockPublishServiceConsumer.getPublishedInstances()).thenReturn(initialSet);
        when(mockLifecycleManager.exposeSTSInstanceAsWebService(
                any(Map.class), any(SecurityTokenServiceProvider.class), any(SoapSTSInstanceConfig.class))).thenReturn(mockServer);
        instancePublisher.run();
        verify(mockLifecycleManager, times(1)).exposeSTSInstanceAsWebService(any(Map.class),
                any(SecurityTokenServiceProvider.class), any(SoapSTSInstanceConfig.class));

        SoapSTSInstanceConfig updatedConfig = createInstanceConfig("instanceOne",
                "http://host.com:8080/am2");
        when(mockPublishServiceConsumer.getPublishedInstances()).thenReturn(Sets.newHashSet(updatedConfig));
        instancePublisher.run();
        verify(mockLifecycleManager, times(2)).exposeSTSInstanceAsWebService(any(Map.class),
                any(SecurityTokenServiceProvider.class), any(SoapSTSInstanceConfig.class));
        verify(mockLifecycleManager, times(1)).destroySTSInstance(any(Server.class));
    }

    private SoapSTSInstanceConfig createInstanceConfig(String uriElement, String amDeploymentUrl) throws UnsupportedEncodingException {
        AuthTargetMapping mapping = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "service", "ldap")
                .build();

        SoapDeploymentConfig deploymentConfig =
                SoapDeploymentConfig.builder()
                        .portQName(AMSTSConstants.STANDARD_STS_PORT_QNAME)
                        .serviceQName(AMSTSConstants.STANDARD_STS_SERVICE_NAME)
                        .wsdlLocation("wsdl_loc")
                        .realm("realm")
                        .amDeploymentUrl(amDeploymentUrl)
                        .uriElement(uriElement)
                        .authTargetMapping(mapping)
                        .build();

        SoapSTSKeystoreConfig keystoreConfig = SoapSTSKeystoreConfig.builder()
                            .keystoreFileName("stsstore.jks")
                            .keystorePassword("frstssrvkspw".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                            .encryptionKeyAlias("frstssrval")
                            .encryptionKeyPassword("frstssrvpw".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                            .signatureKeyAlias("frstssrval")
                            .signatureKeyPassword("frstssrvpw".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                            .build();

        SoapSTSInstanceConfig.SoapSTSInstanceConfigBuilderBase<?> builder = SoapSTSInstanceConfig.builder();
        builder.addSecurityPolicyTokenValidationConfiguration(TokenType.OPENAM, false);
        builder.addSecurityPolicyTokenValidationConfiguration(TokenType.USERNAME, true);

        builder.addIssueTokenType(TokenType.SAML2);
        Map<String,String> attributeMap = new HashMap<>();
        attributeMap.put("mail", "email");
        attributeMap.put("uid", "id");
        SAML2Config saml2Config =
                SAML2Config.builder()
                        .nameIdFormat("transient")
                        .tokenLifetimeInSeconds(500000)
                        .spEntityId("http://host.com/saml2/sp/entity/id")
                        .encryptAssertion(true)
                        .signAssertion(true)
                        .encryptionAlgorithm("http://www.w3.org/2001/04/xmlenc#aes128-cbc")
                        .encryptionKeyAlias("test")
                        .signatureKeyAlias("test")
                        .signatureKeyPassword("super.secret".getBytes())
                        .encryptionAlgorithmStrength(128)
                        .keystoreFile("da/directory/file")
                        .keystorePassword("super.secret".getBytes())
                        .attributeMap(attributeMap)
                        .idpId("da_idp")
                        .build();

        return  builder
                .deploymentConfig(deploymentConfig)
                .soapSTSKeystoreConfig(keystoreConfig)
                .saml2Config(saml2Config)
                .build();
    }
}
