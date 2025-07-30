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
 * Copyright 2016 ForgeRock AS.
 * Portions Copyright 2021 Wren Security.
 */

package org.forgerock.openam.notifications;

import static com.sun.identity.common.configuration.AgentConfiguration.AGENT_TYPE_J2EE;
import static com.sun.identity.common.configuration.AgentConfiguration.AGENT_TYPE_OAUTH2;
import static com.sun.identity.common.configuration.AgentConfiguration.AGENT_TYPE_WEB;
import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.guice.core.GuiceModuleLoader;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.guice.core.InjectorConfiguration;
import org.forgerock.openam.authentication.service.AuthUtilsWrapper;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.rest.SSOTokenFactory;
import org.forgerock.openam.utils.CollectionUtils;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdRepoException;

public class NotificationsWebSocketFilterTest extends GuiceTestCase {

    private final static String COOKIE_NAME = "COOKIE_NAME";
    private final static String TOKEN_ID = "TOKEN_ID";
    private SSOTokenFactory ssoTokenFactory;
    private AuthUtilsWrapper authUtilsWrapper;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;
    private NotificationsWebSocketFilter filter;
    private SSOToken ssoToken;
    private AMIdentity identity;
    private CoreWrapper coreWrapper;

    @Override
    public void configure(Binder binder) {
        ssoTokenFactory = mock(SSOTokenFactory.class);
        authUtilsWrapper = mock(AuthUtilsWrapper.class);
        coreWrapper = mock(CoreWrapper.class);
        binder.bind(SSOTokenFactory.class).toInstance(ssoTokenFactory);
        binder.bind(AuthUtilsWrapper.class).toInstance(authUtilsWrapper);
        binder.bind(CoreWrapper.class).toInstance(coreWrapper);
    }

    // Make sure Guice is not eagerly loading everything on class-path.
    // This worked due to luck when RestRouterIT was run before this test.
    // XXX This should be in the parent class or maybe the guice test should be done in a different way?
    @BeforeClass
    public static void setupGuice() throws Exception {
        InjectorConfiguration.setGuiceModuleLoader(new GuiceModuleLoader() {
            @Override
            public Set<Class<? extends Module>> getGuiceModules(Class<? extends Annotation> clazz) {
                return new HashSet<>();
            }
        });
    }

    @BeforeMethod
    public void setUp() throws Exception {
        ssoToken = mock(SSOToken.class);
        identity = mock(AMIdentity.class);
        when(authUtilsWrapper.getCookieName()).thenReturn(COOKIE_NAME);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        filter = new NotificationsWebSocketFilter();
        filter.init(null);
    }

    @Test
    public void filterReturnsUnauthorizedIfTokenIdIsMissing() throws Exception {
        filter.doFilter(request, response, filterChain);

        assertThatResponseStatusIsSetTo(SC_UNAUTHORIZED);
    }

    @Test
    public void filterReturnsUnauthorizedIfTokenIdIsEmpty() throws Exception {
        when(request.getHeader(COOKIE_NAME)).thenReturn("");

        filter.doFilter(request, response, filterChain);

        assertThatResponseStatusIsSetTo(SC_UNAUTHORIZED);
    }

    @Test
    public void filterReturnsUnauthorizedIfTokenIsMissing() throws Exception {
        when(request.getHeader(COOKIE_NAME)).thenReturn(TOKEN_ID);
        when(ssoTokenFactory.getTokenFromId(TOKEN_ID)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThatResponseStatusIsSetTo(SC_UNAUTHORIZED);
    }

    @Test
    public void filterReturnsForbiddenIfTokenDoesNotBelongToJ2EEOrWebAgent() throws Exception {
        createSSOTokenFor(AGENT_TYPE_OAUTH2);

        filter.doFilter(request, response, filterChain);

        assertThatResponseStatusIsSetTo(SC_FORBIDDEN);
    }

    @Test
    public void filterContinuesExecutionIfTokenBelongsToJ2EEAgent() throws Exception {
        createSSOTokenFor(AGENT_TYPE_J2EE);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    public void filterContinuesExecutionIfTokenBelongsToWebAgent() throws Exception {
        createSSOTokenFor(AGENT_TYPE_WEB);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    public void filterReturnsUnauthorizedIfCannotGetIdentity() throws Exception {
        when(request.getHeader(COOKIE_NAME)).thenReturn(TOKEN_ID);
        when(ssoTokenFactory.getTokenFromId(TOKEN_ID)).thenReturn(ssoToken);
        when(coreWrapper.getIdentity(ssoToken)).thenThrow(new IdRepoException());

        filter.doFilter(request, response, filterChain);

        assertThatResponseStatusIsSetTo(SC_UNAUTHORIZED);
    }

    @Test
    public void filterReturnsUnauthorizedIfTokenException() throws Exception {
        when(request.getHeader(COOKIE_NAME)).thenReturn(TOKEN_ID);
        when(ssoTokenFactory.getTokenFromId(TOKEN_ID)).thenReturn(ssoToken);
        when(coreWrapper.getIdentity(ssoToken)).thenThrow(new SSOException("SSO token exception"));

        filter.doFilter(request, response, filterChain);

        assertThatResponseStatusIsSetTo(SC_UNAUTHORIZED);
    }

    private void assertThatResponseStatusIsSetTo(int status) {
        ArgumentCaptor<Integer> responseStatus = ArgumentCaptor.forClass(Integer.class);
        verify(response).setStatus(responseStatus.capture());
        assertThat(responseStatus.getValue()).isEqualTo(status);
    }

    private void createSSOTokenFor(String agentType) throws IdRepoException, SSOException {
        when(coreWrapper.getIdentity(ssoToken)).thenReturn(identity);
        when(request.getHeader(COOKIE_NAME)).thenReturn(TOKEN_ID);
        when(ssoTokenFactory.getTokenFromId(TOKEN_ID)).thenReturn(ssoToken);
        when(identity.getAttribute(IdConstants.AGENT_TYPE)).thenReturn(CollectionUtils.asSet(agentType));
    }
}
