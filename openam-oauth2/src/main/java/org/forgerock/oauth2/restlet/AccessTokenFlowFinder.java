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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions Copyright 2023 Wren Security.
 */

package org.forgerock.oauth2.restlet;

import static org.forgerock.openam.oauth2.OAuth2Constants.TokenEndpoint.*;
import static org.forgerock.openam.rest.service.RestletUtils.wrap;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.restlet.resource.Finder;

public class AccessTokenFlowFinder extends OAuth2FlowFinder {

    public AccessTokenFlowFinder() {
        super(InjectorHolder.getInstance(OAuth2RequestFactory.class),
                InjectorHolder.getInstance(ExceptionHandler.class),
                getEndpointClasses());
    }

    private static Map<String, Finder> getEndpointClasses() {
        Map<String, Finder> endpointClasses = new HashMap<>();
        endpointClasses.put(AUTHORIZATION_CODE, wrap(TokenEndpointResource.class));
        endpointClasses.put(REFRESH_TOKEN, wrap(RefreshTokenResource.class));
        endpointClasses.put(CLIENT_CREDENTIALS, wrap(TokenEndpointResource.class));
        endpointClasses.put(PASSWORD, wrap(TokenEndpointResource.class));
        endpointClasses.put(DEVICE_CODE, wrap(TokenEndpointResource.class));
        endpointClasses.put(SAML2_BEARER, wrap(TokenEndpointResource.class));
        return endpointClasses;
    }

}
