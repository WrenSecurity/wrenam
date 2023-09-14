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
 * Copyright 2013-2016 ForgeRock AS.
 * Portions Copyright 2021-2023 Wren Security.
 */
package org.forgerock.openam.sm.datalayer.impl.ldap;

import static org.fest.assertions.Assertions.*;
import static org.forgerock.openam.utils.CollectionUtils.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

import java.util.Set;

import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.impl.CTSDataLayerConfiguration;
import org.forgerock.openam.ldap.LDAPURL;
import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.shared.debug.Debug;

public class ExternalLdapConfigTest {

    private Debug debug;
    private LdapDataLayerConfiguration dataLayerConfiguration;
    private MockedStatic<SystemProperties> systemProperties;
    private MockedStatic<WebtopNaming> webtopNaming;

    @BeforeMethod
    public void setup() {
        this.debug = mock(Debug.class);
        this.dataLayerConfiguration = spy(new CTSDataLayerConfiguration("ou=root-dn"));
        this.systemProperties = mockStatic(SystemProperties.class);
        this.webtopNaming = mockStatic(WebtopNaming.class);
    }

    @AfterMethod
    public void cleanup() {
        this.systemProperties.close();
        this.webtopNaming.close();
    }

    @Test
    public void shouldUseSystemPropertiesWrapperForNotifyChanges() throws Exception {
        // Given
        ExternalLdapConfig config = new ExternalLdapConfig(debug);
        // When
        config.update(dataLayerConfiguration);
        // Then
        systemProperties.verify(() -> SystemProperties.get(anyString()), times(3));
        systemProperties.verify(() -> SystemProperties.getAsBoolean(anyString(), anyBoolean()), times(2));
        systemProperties.verify(() -> SystemProperties.getAsInt(anyString(), eq(-1)));
    }

    @Test
    public void shouldIndicateHasChanged() {
        systemProperties.when(() -> SystemProperties.get(CoreTokenConstants.CTS_STORE_HOSTNAME)).thenReturn("badger");

        ExternalLdapConfig config = new ExternalLdapConfig(debug);
        // When
        config.update(dataLayerConfiguration);
        // Then
        assertThat(config.hasChanged()).isTrue();
    }

    @Test
    public void shouldBeNullForNullPassword() {
        systemProperties.when(() -> SystemProperties.get(CoreTokenConstants.CTS_STORE_PASSWORD)).thenReturn(null);

        ExternalLdapConfig config = new ExternalLdapConfig(debug);
        config.update(dataLayerConfiguration);

        // When
        char[] result = config.getBindPassword();
        // Then
        assertThat(result).isNull();
    }

    @Test
    public void shouldPrioritizeServerList() throws Exception {
        // Given
        systemProperties.when(() -> SystemProperties.get(CoreTokenConstants.CTS_STORE_HOSTNAME))
                .thenReturn("test1.com:389|03,test2.com|02,test3.com|01");
        systemProperties.when(() -> SystemProperties.getAsBoolean(CoreTokenConstants.CTS_STORE_SSL_ENABLED, false)).thenReturn(true);

        webtopNaming.when(WebtopNaming::getAMServerID).thenReturn("01");
        webtopNaming.when(() -> WebtopNaming.getSiteID("01")).thenReturn("02");

        ExternalLdapConfig config = new ExternalLdapConfig(debug);
        config.update(dataLayerConfiguration);

        //When
        Set<LDAPURL> urls = config.getLDAPURLs();

        //Then
        assertThat(urls).isEqualTo(asOrderedSet(valueOf("test3.com"), valueOf("test1.com"), valueOf("test2.com")));
    }

    private LDAPURL valueOf(String host) {
        return LDAPURL.valueOf(host, 389, true);
    }
}
