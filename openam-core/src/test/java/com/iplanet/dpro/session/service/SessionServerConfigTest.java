package com.iplanet.dpro.session.service;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.session.SessionServiceURLService;
import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SessionServerConfigTest {

    private MockedStatic<SystemProperties> systemProperties;
    private MockedStatic<WebtopNaming> webtopNaming;

    @BeforeMethod
    public void setup() {
        this.systemProperties = mockStatic(SystemProperties.class);
        this.webtopNaming = mockStatic(WebtopNaming.class);
    }

    @AfterMethod
    public void cleanup() {
        this.systemProperties.close();
        this.webtopNaming.close();
    }

    @Test
    public void localServerIsPrimaryServerIfNoSiteSetup() throws Exception {
        // Given
        systemProperties.when(() -> SystemProperties.get(Constants.AM_SERVER_PROTOCOL)).thenReturn("http");
        systemProperties.when(() -> SystemProperties.get(Constants.AM_SERVER_HOST)).thenReturn("openam.example.com");
        systemProperties.when(() -> SystemProperties.get(Constants.AM_SERVER_PORT)).thenReturn("8080");
        systemProperties.when(() -> SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR)).thenReturn("/openam");

        String primary = "01";
        webtopNaming.when(() -> WebtopNaming.getServerID("http", "openam.example.com", "8080", "/openam")).thenReturn(primary);
        webtopNaming.when(WebtopNaming::getAMServerID).thenReturn("01");
        webtopNaming.when(WebtopNaming::getLocalServer).thenReturn("http://openam.example.com:8080/openam");

        // When
        SessionServerConfig config = new SessionServerConfig(mock(Debug.class), mock(SessionServiceURLService.class));

        // Then
        assertThat(config.getPrimaryServerID()).isEqualTo(primary);
    }

    @Test
    public void localServerAndPrimaryServerDifferIfSiteSetup() throws Exception {
        // Given
        systemProperties.when(() -> SystemProperties.get(Constants.AM_SERVER_PROTOCOL)).thenReturn("http");
        systemProperties.when(() -> SystemProperties.get(Constants.AM_SERVER_HOST)).thenReturn("openam.example.com");
        systemProperties.when(() -> SystemProperties.get(Constants.AM_SERVER_PORT)).thenReturn("8080");
        systemProperties.when(() -> SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR)).thenReturn("/openam");

        String primary = "01";
        webtopNaming.when(() -> WebtopNaming.getServerID("http", "openam.example.com", "8080", "/openam")).thenReturn(primary);
        webtopNaming.when(() -> WebtopNaming.isSiteEnabled(anyString())).thenReturn(true); // enable site
        webtopNaming.when(() -> WebtopNaming.getSiteID(anyString())).thenReturn("02");
        webtopNaming.when(WebtopNaming::getAMServerID).thenReturn("01");
        webtopNaming.when(WebtopNaming::getLocalServer).thenReturn("http://openam.example.com:8080/openam");

        // When
        SessionServerConfig config = new SessionServerConfig(mock(Debug.class), mock(SessionServiceURLService.class));

        // Then
        assertThat(config.getPrimaryServerID()).isNotEqualTo(primary);
    }

}
