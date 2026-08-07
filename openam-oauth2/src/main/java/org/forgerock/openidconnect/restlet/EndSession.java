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
 * Portions copyright 2026 Wren Security
 */

package org.forgerock.openidconnect.restlet;

import static org.forgerock.openam.oauth2.OAuth2Constants.Custom.LOCALE;
import static org.forgerock.openam.oauth2.OAuth2Constants.Custom.UI_LOCALES;

import com.iplanet.sso.SSOToken;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.core.UriBuilder;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.restlet.ExceptionHandler;
import org.forgerock.oauth2.restlet.OAuth2RestletException;
import org.forgerock.oauth2.restlet.TemplateFactory;
import org.forgerock.openam.rest.service.RestletRealmRouter;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openidconnect.EndSessionParameters;
import org.forgerock.openidconnect.EndSessionService;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Redirector;

/**
 * Handles requests to the OpenId Connect end session endpoint for ending OpenId Connect user sessions.
 * <p>
 * Implementation is conformant with the OpenID Connect RP-Initiated Logout 1.0. This resource only deals with
 * HTTP/Restlet concerns and the rendering of the pages; the actual logout logic lives in {@link EndSessionService}.
 * </p>
 *
 * @since 11.0.0
 * @see <a href="https://openid.net/specs/openid-connect-rpinitiated-1_0.html">OpenID Connect RP-Initiated Logout 1.0</a>
 */
public class EndSession extends ServerResource {

    private final OAuth2RequestFactory requestFactory;
    private final ExceptionHandler exceptionHandler;
    private final EndSessionService endSessionService;
    private final BaseURLProviderFactory baseURLProviderFactory;

    /**
     * Constructs a new EndSession.
     *
     * @param requestFactory An instance of the OAuth2RequestFactory.
     * @param exceptionHandler An instance of the ExceptionHandler.
     * @param endSessionService An instance of the EndSessionService.
     * @param baseURLProviderFactory An instance of the BaseURLProviderFactory.
     */
    @Inject
    public EndSession(OAuth2RequestFactory requestFactory, ExceptionHandler exceptionHandler,
            EndSessionService endSessionService, BaseURLProviderFactory baseURLProviderFactory) {
        this.requestFactory = requestFactory;
        this.exceptionHandler = exceptionHandler;
        this.endSessionService = endSessionService;
        this.baseURLProviderFactory = baseURLProviderFactory;
    }

    /**
     * Handles GET requests to the OpenID Connect end session endpoint.
     */
    @Get
    public Representation endSession() throws OAuth2RestletException {
        return handleLogoutRequest();
    }

    /**
     * Handles POST requests carrying the user's decision submitted from the end session confirmation page.
     *
     * @return The representation of the resulting page, or of the redirect response.
     * @throws OAuth2RestletException If the logout request is invalid or the logout itself fails.
     */
    @Post
    public Representation endSession(Representation ignored) throws OAuth2RestletException {
        String decision = getServletRequest().getParameter("decision");

        if ("logout".equals(decision)) {
            return handleConfirmedLogout();
        }
        if ("cancel".equals(decision)) {
            return getTemplate("canceled");
        }

        return handleLogoutRequest();
    }

    private Representation handleLogoutRequest() throws OAuth2RestletException {
        try {
            EndSessionParameters params = EndSessionParameters.from(getServletRequest());
            OAuth2Request request = requestFactory.create(getRequest());
            validateRequest(request, params);
            SSOToken session = endSessionService.getCurrentSession(getServletRequest()).orElse(null);
            if (params.getIdTokenHint().isEmpty() && session == null) {
                // Nothing to log out
                return getTemplate("done");
            }
            if (endSessionService.canSkipLogoutConfirmation(request, params, session)) {
                return performLogout(request, params, session);
            }
            return getTemplate("confirmation", getConfirmationDataModel(params, session));
        } catch (OAuth2Exception e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(), null);
        }
    }

    private Representation handleConfirmedLogout() throws OAuth2RestletException {
        EndSessionParameters params = EndSessionParameters.from(getServletRequest());
        OAuth2Request request = requestFactory.create(getRequest());
        validateRequest(request, params);
        SSOToken session = endSessionService.getCurrentSession(getServletRequest()).orElse(null);
        try {
            endSessionService.verifyCsrf(request, session);
            return performLogout(request, params, session);
        } catch (OAuth2Exception e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(), null);
        }
    }

    private Representation performLogout(OAuth2Request request, EndSessionParameters params, SSOToken session)
            throws OAuth2RestletException {
        try {
            if (params.getIdTokenHint().isPresent()) {
                endSessionService.endSession(request, params);
                if (params.getPostLogoutRedirectUri().isPresent()) {
                    return handleRedirect(params);
                }
            } else if (session != null) {
                // Without a valid id_token_hint the OP MUST NOT perform post-logout redirection.
                // Just terminate the current session.
                endSessionService.logout(session);
            }
        } catch (OAuth2Exception e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(), null);
        }
        return getTemplate("done");
    }

    private void validateRequest(OAuth2Request request, EndSessionParameters params) throws OAuth2RestletException {
        try {
            endSessionService.validate(request, params);
        } catch (OAuth2Exception e) {
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(), null);
        }
    }

    private TemplateRepresentation getTemplate(String stage) {
        return getTemplate(stage, Map.of());
    }

    private TemplateRepresentation getTemplate(String stage, Map<String, Object> dataModel) {
        Map<String, Object> endSessionDataModel = new HashMap<>(dataModel);
        endSessionDataModel.put("stage", stage);
        endSessionDataModel.put("baseUrl", baseURLProviderFactory.get(getRealm()).getRootURL(getServletRequest()));
        endSessionDataModel.put("realm", getRealm());
        endSessionDataModel.put("locale", getLocale());
        TemplateFactory templateFactory = TemplateFactory.newInstance(getContext());
        TemplateRepresentation representation = templateFactory.getTemplateRepresentation("templates/EndSession.ftl");
        representation.setDataModel(endSessionDataModel);
        return representation;
    }

    private Map<String, Object> getConfirmationDataModel(EndSessionParameters params, SSOToken session)
            throws InvalidClientException, NotFoundException {
        Map<String, Object> dataModel = new HashMap<>();

        endSessionService.resolveClientName(getRealm(), params, getLocale())
                .ifPresent(clientName -> dataModel.put("clientName", clientName));
        endSessionService.resolveUserName(params, session)
                .ifPresent(userName -> dataModel.put("userName", userName));
        endSessionService.resolveCsrfToken(session)
                .ifPresent(csrf -> dataModel.put("csrf", csrf));
        params.getIdTokenHint()
                .ifPresent(idTokenHint -> dataModel.put("idTokenHint", idTokenHint));
        params.getPostLogoutRedirectUri()
                .ifPresent(redirectUri -> dataModel.put("postLogoutRedirectUri", redirectUri));
        params.getState()
                .ifPresent(state -> dataModel.put("state", state));

        return dataModel;
    }

    /**
     * Handles any exception thrown when processing an end session request.
     *
     * @param throwable The throwable.
     */
    @Override
    protected void doCatch(Throwable throwable) {
        exceptionHandler.handle(throwable, getResponse(), error -> getTemplate("error", new HashMap<>(error)));
    }

    private Representation handleRedirect(EndSessionParameters params) {
        String target = params.getPostLogoutRedirectUri().orElseThrow();
        Optional<String> state = params.getState();
        if (state.isPresent()) {
            target = UriBuilder.fromUri(URI.create(target))
                    .queryParam("state", "{state}")
                    .build(state.get())
                    .toString();
        }
        new Redirector(getContext(), target, Redirector.MODE_CLIENT_FOUND)
                .handle(getRequest(), getResponse());
        return new EmptyRepresentation();
    }

    /**
     * Resolves the locale from the {@code ui_locales} or {@code locale} request parameters, falling back to the locale
     * negotiated from the {@code Accept-Language} header. Never returns {@code null}.
     */
    private Locale getLocale() {
        HttpServletRequest request = getServletRequest();
        String uiLocaleParameter = request.getParameter(UI_LOCALES);
        String localeParameter = request.getParameter(LOCALE);
        if (StringUtils.isNotBlank(uiLocaleParameter)) {
            // ui_locales is a space-separated list of BCP 47 tags. Use the first one.
            return Locale.forLanguageTag(uiLocaleParameter.trim().split("\\s+")[0]);
        }
        if (StringUtils.isNotBlank(localeParameter)) {
            return Locale.forLanguageTag(localeParameter);
        }
        return request.getLocale();
    }

    @SuppressWarnings("deprecation")
    private HttpServletRequest getServletRequest() {
        return ServletUtils.getRequest(getRequest());
    }

    @SuppressWarnings("deprecation")
    private String getRealm() {
        return (String) getRequest().getAttributes().get(RestletRealmRouter.REALM);
    }

}
