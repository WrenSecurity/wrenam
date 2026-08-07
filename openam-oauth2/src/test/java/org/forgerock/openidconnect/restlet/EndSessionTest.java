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
 * Copyright 2026 Wren Security
 */

package org.forgerock.openidconnect.restlet;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.fail;

import com.iplanet.sso.SSOToken;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.CsrfException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.restlet.ExceptionHandler;
import org.forgerock.oauth2.restlet.OAuth2RestletException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.rest.service.RestletRealmRouter;
import org.forgerock.openam.services.baseurl.BaseURLProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openidconnect.EndSessionParameters;
import org.forgerock.openidconnect.EndSessionService;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.engine.adapter.HttpRequest;
import org.restlet.ext.servlet.internal.ServletCall;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class EndSessionTest {

    private static final String REALM = "/";
    private static final String CLIENT_ID = "test-client";
    private static final String ID_TOKEN = "id-token-hint";
    private static final String REDIRECT_URI = "https://rp.example.com/loggedout";
    private static final String STATE = "opaque-state";

    private Request restletRequest;
    private EndSessionService endSessionService;
    private BaseURLProviderFactory baseURLProviderFactory;
    private ExceptionHandler exceptionHandler;
    private HttpServletRequest servletRequest;
    private OAuth2Request oAuth2Request;
    private Response response;
    private EndSession resource;

    @BeforeMethod
    @SuppressWarnings("deprecation")
    public void setup() throws Exception {
        OAuth2RequestFactory requestFactory = mock(OAuth2RequestFactory.class);
        baseURLProviderFactory = mock(BaseURLProviderFactory.class);
        exceptionHandler = mock(ExceptionHandler.class);
        endSessionService = mock(EndSessionService.class);
        servletRequest = mock(HttpServletRequest.class);
        oAuth2Request = mock(OAuth2Request.class);
        response = mock(Response.class);
        // The resource resolves the servlet request and the realm from the restlet request the same way the
        // framework does: through the wrapped servlet call and the realm request attribute set by the realm router.
        ServletCall servletCall = mock(ServletCall.class);
        given(servletCall.getRequest()).willReturn(servletRequest);
        HttpRequest httpRequest = mock(HttpRequest.class);
        given(httpRequest.getHttpCall()).willReturn(servletCall);
        given(httpRequest.getAttributes())
                .willReturn(new ConcurrentHashMap<>(Map.of(RestletRealmRouter.REALM, REALM)));
        restletRequest = httpRequest;
        given(requestFactory.create(restletRequest)).willReturn(oAuth2Request);
        BaseURLProvider baseURLProvider = mock(BaseURLProvider.class);
        given(baseURLProviderFactory.get(REALM)).willReturn(baseURLProvider);
        given(baseURLProvider.getRootURL(servletRequest)).willReturn("https://am.example.com/auth");

        resource = spy(new EndSession(requestFactory, exceptionHandler, endSessionService, baseURLProviderFactory));
        doReturn(restletRequest).when(resource).getRequest();
        doReturn(response).when(resource).getResponse();
        // The template factory loads templates through the client dispatcher (clap://), i.e. from the classpath.
        Context context = new Context();
        context.setClientDispatcher(new Client(context, Protocol.CLAP));
        doReturn(context).when(resource).getContext();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dataModel(Representation representation) {
        return (Map<String, Object>) ((TemplateRepresentation) representation).getDataModel();
    }

    @Test
    public void endSessionGet_withoutIdTokenHint_rendersConfirmation() throws Exception {
        given(endSessionService.getCurrentSession(servletRequest)).willReturn(Optional.of(mock(SSOToken.class)));

        Map<String, Object> dataModel = dataModel(resource.endSession());

        assertThat(dataModel.get("stage")).isEqualTo("confirmation");
        assertThat(dataModel.get("baseUrl")).isEqualTo("https://am.example.com/auth");
        verify(endSessionService).validate(eq(oAuth2Request), any(EndSessionParameters.class));
    }

    @Test
    public void endSessionGet_logoutDecisionDoesNotConfirmLogout() throws Exception {
        given(servletRequest.getParameter("decision")).willReturn("logout");
        given(endSessionService.getCurrentSession(servletRequest)).willReturn(Optional.of(mock(SSOToken.class)));

        Map<String, Object> dataModel = dataModel(resource.endSession());

        assertThat(dataModel.get("stage")).isEqualTo("confirmation");
        verify(endSessionService, never()).verifyCsrf(any(), any());
        verify(endSessionService, never()).endSession(any(), any());
        verify(endSessionService, never()).logout(any());
    }

    @Test
    public void endSessionGet_noSessionAndNoIdTokenHint_rendersDone() throws Exception {
        given(endSessionService.getCurrentSession(servletRequest)).willReturn(Optional.empty());

        Map<String, Object> dataModel = dataModel(resource.endSession());

        assertThat(dataModel.get("stage")).isEqualTo("done");
        verify(endSessionService, never()).resolveClientName(any(), any(), any());
    }

    @Test
    public void endSessionGet_validRequest_validatesAndRenders() throws Exception {
        given(servletRequest.getParameter(OAuth2Constants.Params.END_SESSION_ID_TOKEN_HINT)).willReturn(ID_TOKEN);
        given(servletRequest.getParameter(OAuth2Constants.Params.CLIENT_ID)).willReturn(CLIENT_ID);
        given(servletRequest.getParameter(OAuth2Constants.Params.POST_LOGOUT_REDIRECT_URI)).willReturn(REDIRECT_URI);

        assertThat(resource.endSession()).isNotNull();

        verify(endSessionService).validate(eq(oAuth2Request), any(EndSessionParameters.class));
    }

    @Test
    public void endSessionGet_trustedRequestSkipsConfirmationAndEndsSession() throws Exception {
        SSOToken session = mock(SSOToken.class);
        given(servletRequest.getParameter(OAuth2Constants.Params.END_SESSION_ID_TOKEN_HINT)).willReturn(ID_TOKEN);
        given(endSessionService.getCurrentSession(servletRequest)).willReturn(Optional.of(session));
        given(endSessionService.canSkipLogoutConfirmation(eq(oAuth2Request), any(EndSessionParameters.class),
                eq(session))).willReturn(true);

        resource.endSession();

        verify(endSessionService).endSession(eq(oAuth2Request), any(EndSessionParameters.class));
        verify(endSessionService, never()).verifyCsrf(any(), any());
        verify(endSessionService, never()).resolveClientName(any(), any(), any());
    }

    @Test
    public void endSessionGet_populatesTemplateDataModel() throws Exception {
        given(servletRequest.getParameter(OAuth2Constants.Params.END_SESSION_ID_TOKEN_HINT)).willReturn(ID_TOKEN);
        given(servletRequest.getParameter(OAuth2Constants.Params.CLIENT_ID)).willReturn(CLIENT_ID);
        given(servletRequest.getParameter(OAuth2Constants.Params.POST_LOGOUT_REDIRECT_URI)).willReturn(REDIRECT_URI);
        given(servletRequest.getParameter(OAuth2Constants.Params.STATE)).willReturn(STATE);
        given(servletRequest.getParameter(OAuth2Constants.Custom.UI_LOCALES)).willReturn("fr-CA en");
        given(endSessionService.resolveClientName(eq(REALM), any(EndSessionParameters.class), eq(Locale.CANADA_FRENCH)))
                .willReturn(Optional.of("Example RP"));
        given(endSessionService.resolveUserName(any(EndSessionParameters.class), any()))
                .willReturn(Optional.of("demo"));

        Map<String, Object> dataModel = dataModel(resource.endSession());

        assertThat(dataModel.get("stage")).isEqualTo("confirmation");
        assertThat(dataModel.get("baseUrl")).isEqualTo("https://am.example.com/auth");
        assertThat(dataModel.get("realm")).isEqualTo(REALM);
        assertThat(dataModel.get("locale")).isEqualTo(Locale.CANADA_FRENCH);
        assertThat(dataModel.get("clientName")).isEqualTo("Example RP");
        assertThat(dataModel.get("userName")).isEqualTo("demo");
        assertThat(dataModel.get("idTokenHint")).isEqualTo(ID_TOKEN);
        assertThat(dataModel.get("postLogoutRedirectUri")).isEqualTo(REDIRECT_URI);
        assertThat(dataModel.get("state")).isEqualTo(STATE);
    }

    @Test(expectedExceptions = OAuth2RestletException.class)
    public void endSessionGet_invalidIdTokenHint_throwsException() throws Exception {
        given(servletRequest.getParameter(OAuth2Constants.Params.END_SESSION_ID_TOKEN_HINT)).willReturn(ID_TOKEN);
        willThrow(new BadRequestException("id_token_hint signature could not be verified"))
                .given(endSessionService).validate(any(), any());

        resource.endSession();
    }

    @Test(expectedExceptions = OAuth2RestletException.class)
    public void endSessionGet_unregisteredRedirectUri_throwsException() throws Exception {
        given(servletRequest.getParameter(OAuth2Constants.Params.END_SESSION_ID_TOKEN_HINT)).willReturn(ID_TOKEN);
        given(servletRequest.getParameter(OAuth2Constants.Params.POST_LOGOUT_REDIRECT_URI)).willReturn(REDIRECT_URI);
        willThrow(new RedirectUriMismatchException())
                .given(endSessionService).validate(any(), any());

        resource.endSession();
    }

    @Test
    public void endSession_logoutDecision_validatesEndsSessionAndRendersDone() throws Exception {
        given(servletRequest.getParameter("decision")).willReturn("logout");
        given(servletRequest.getParameter(OAuth2Constants.Params.END_SESSION_ID_TOKEN_HINT)).willReturn(ID_TOKEN);

        assertThat(resource.endSession(null)).isNotNull();

        verify(endSessionService).validate(eq(oAuth2Request), any(EndSessionParameters.class));
        verify(endSessionService).endSession(eq(oAuth2Request), any(EndSessionParameters.class));
    }

    @Test
    public void endSession_registeredRedirectUri_validatesBeforeEndingSession() throws Exception {
        given(servletRequest.getParameter("decision")).willReturn("logout");
        given(servletRequest.getParameter(OAuth2Constants.Params.END_SESSION_ID_TOKEN_HINT)).willReturn(ID_TOKEN);
        given(servletRequest.getParameter(OAuth2Constants.Params.POST_LOGOUT_REDIRECT_URI)).willReturn(REDIRECT_URI);

        resource.endSession(null);

        InOrder inOrder = inOrder(endSessionService);
        inOrder.verify(endSessionService).validate(eq(oAuth2Request), any(EndSessionParameters.class));
        inOrder.verify(endSessionService).endSession(eq(oAuth2Request), any(EndSessionParameters.class));
    }

    @Test
    public void endSession_unregisteredRedirectUri_doesNotEndSession() throws Exception {
        given(servletRequest.getParameter("decision")).willReturn("logout");
        given(servletRequest.getParameter(OAuth2Constants.Params.END_SESSION_ID_TOKEN_HINT)).willReturn(ID_TOKEN);
        given(servletRequest.getParameter(OAuth2Constants.Params.POST_LOGOUT_REDIRECT_URI)).willReturn(REDIRECT_URI);
        willThrow(new RedirectUriMismatchException())
                .given(endSessionService).validate(any(), any());

        try {
            resource.endSession(null);
            fail("Expected OAuth2RestletException");
        } catch (OAuth2RestletException e) {
            // The session must stay untouched when the request is invalid.
        }
        verify(endSessionService, never()).endSession(any(), any());
        verify(endSessionService, never()).logout(any());
    }

    @Test
    public void endSession_cancelDecision_rendersCanceledWithoutEndingSession() throws Exception {
        given(servletRequest.getParameter("decision")).willReturn("cancel");

        Map<String, Object> dataModel = dataModel(resource.endSession(null));

        assertThat(dataModel.get("stage")).isEqualTo("canceled");
        verify(endSessionService, never()).validate(any(), any());
        verify(endSessionService, never()).endSession(any(), any());
        verify(endSessionService, never()).logout(any());
    }

    @Test
    public void endSession_unknownDecision_rendersConfirmation() throws Exception {
        given(servletRequest.getParameter("decision")).willReturn("ignore");
        given(endSessionService.getCurrentSession(servletRequest)).willReturn(Optional.of(mock(SSOToken.class)));

        Map<String, Object> dataModel = dataModel(resource.endSession(null));

        assertThat(dataModel.get("stage")).isEqualTo("confirmation");
        verify(endSessionService, never()).endSession(any(), any());
        verify(endSessionService, never()).logout(any());
    }

    @Test
    public void endSession_registeredRedirectUri_redirectsWithState() throws Exception {
        Response realResponse = new Response(restletRequest);
        doReturn(realResponse).when(resource).getResponse();
        given(servletRequest.getParameter("decision")).willReturn("logout");
        given(servletRequest.getParameter(OAuth2Constants.Params.END_SESSION_ID_TOKEN_HINT)).willReturn(ID_TOKEN);
        given(servletRequest.getParameter(OAuth2Constants.Params.POST_LOGOUT_REDIRECT_URI)).willReturn(REDIRECT_URI);
        given(servletRequest.getParameter(OAuth2Constants.Params.STATE)).willReturn(STATE);

        Representation representation = resource.endSession(null);

        assertThat(representation).isInstanceOf(EmptyRepresentation.class);
        assertThat(realResponse.getStatus()).isEqualTo(Status.REDIRECTION_FOUND);
        assertThat(realResponse.getLocationRef().toString()).isEqualTo(REDIRECT_URI + "?state=" + STATE);
        verify(endSessionService).endSession(eq(oAuth2Request), any(EndSessionParameters.class));
    }

    @Test
    public void endSession_blankRedirectUri_rendersDoneWithoutRedirect() throws Exception {
        given(servletRequest.getParameter("decision")).willReturn("logout");
        given(servletRequest.getParameter(OAuth2Constants.Params.END_SESSION_ID_TOKEN_HINT)).willReturn(ID_TOKEN);
        given(servletRequest.getParameter(OAuth2Constants.Params.POST_LOGOUT_REDIRECT_URI)).willReturn("   ");

        Map<String, Object> dataModel = dataModel(resource.endSession(null));

        // A blank redirect URI is treated as absent, so no redirect may be performed.
        assertThat(dataModel.get("stage")).isEqualTo("done");
        verify(endSessionService).endSession(eq(oAuth2Request), any(EndSessionParameters.class));
    }

    @Test
    public void endSession_withoutIdTokenHint_logsOutBySession() throws Exception {
        given(servletRequest.getParameter("decision")).willReturn("logout");
        SSOToken ssoToken = mock(SSOToken.class);
        given(endSessionService.getCurrentSession(servletRequest)).willReturn(Optional.of(ssoToken));

        assertThat(resource.endSession(null)).isNotNull();

        verify(endSessionService).logout(ssoToken);
        verify(endSessionService, never()).endSession(any(), any());
    }

    @Test
    public void endSession_withoutIdTokenHintAndNoSession_rendersDoneWithoutLogout() throws Exception {
        given(servletRequest.getParameter("decision")).willReturn("logout");

        Map<String, Object> dataModel = dataModel(resource.endSession(null));

        assertThat(dataModel.get("stage")).isEqualTo("done");
        verify(endSessionService, never()).logout(any());
    }

    @Test
    public void endSession_withoutIdTokenHint_ignoresPostLogoutRedirectUri() throws Exception {
        given(servletRequest.getParameter("decision")).willReturn("logout");
        given(servletRequest.getParameter(OAuth2Constants.Params.POST_LOGOUT_REDIRECT_URI)).willReturn(REDIRECT_URI);
        SSOToken ssoToken = mock(SSOToken.class);
        given(endSessionService.getCurrentSession(servletRequest)).willReturn(Optional.of(ssoToken));

        Map<String, Object> dataModel = dataModel(resource.endSession(null));

        assertThat(dataModel.get("stage")).isEqualTo("done");
        verify(endSessionService).logout(ssoToken);
        verify(response, never()).setStatus(Status.REDIRECTION_FOUND);
        verify(response, never()).setLocationRef(any(String.class));
    }

    @Test
    public void endSession_logoutDecision_verifiesCsrf() throws Exception {
        given(servletRequest.getParameter("decision")).willReturn("logout");
        given(servletRequest.getParameter(OAuth2Constants.Params.END_SESSION_ID_TOKEN_HINT)).willReturn(ID_TOKEN);

        resource.endSession(null);

        verify(endSessionService).verifyCsrf(any(OAuth2Request.class), any());
    }

    @Test
    public void endSession_csrfFailure_doesNotEndSession() throws Exception {
        given(servletRequest.getParameter("decision")).willReturn("logout");
        given(servletRequest.getParameter(OAuth2Constants.Params.END_SESSION_ID_TOKEN_HINT)).willReturn(ID_TOKEN);
        willThrow(new CsrfException()).given(endSessionService).verifyCsrf(any(), any());

        try {
            resource.endSession(null);
            fail("Expected OAuth2RestletException");
        } catch (OAuth2RestletException e) {
            // A forged logout must be rejected before the session is destroyed.
        }
        verify(endSessionService, never()).endSession(any(), any());
        verify(endSessionService, never()).logout(any());
    }

    @Test(expectedExceptions = OAuth2RestletException.class)
    public void endSessionGet_unknownClient_throwsOAuth2RestletException() throws Exception {
        given(servletRequest.getParameter(OAuth2Constants.Params.CLIENT_ID)).willReturn(CLIENT_ID);
        given(endSessionService.getCurrentSession(servletRequest)).willReturn(Optional.of(mock(SSOToken.class)));
        given(endSessionService.resolveClientName(any(), any(), any()))
                .willThrow(InvalidClientException.class);

        resource.endSession();
    }

    @Test
    public void doCatch_delegatesToExceptionHandler() {
        Throwable throwable = new RuntimeException(new OAuth2RestletException(400, "invalid_request", "foobar", null));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Function<Map<String, String>, Representation>> representationFactory =
                ArgumentCaptor.forClass(Function.class);

        resource.doCatch(throwable);

        verify(exceptionHandler).handle(eq(throwable), eq(response), representationFactory.capture());
        Map<String, Object> dataModel = dataModel(representationFactory.getValue().apply(
                Map.of("error", "invalid_request", "error_description", "foobar")));
        assertThat(dataModel.get("stage")).isEqualTo("error");
        assertThat(dataModel.get("error")).isEqualTo("invalid_request");
        assertThat(dataModel.get("error_description")).isEqualTo("foobar");
    }

}
