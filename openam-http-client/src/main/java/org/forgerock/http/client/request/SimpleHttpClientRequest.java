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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.http.client.request;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A basic implementation of {@link HttpClientRequest} that a script can send over a {@code org.forgerock.http.client.HttpClient}.
 *
 * @since 12.0.0
 */
public class SimpleHttpClientRequest implements HttpClientRequest {

    private final Map<String, String> headers = new HashMap<String, String>();
    private final Map<String, String> queryParameters = new HashMap<String, String>();
    private final Set<HttpClientRequestCookie> cookies = new HashSet<HttpClientRequestCookie>();
    private String method;
    private String uri;
    private String entity;

    /**
     * {@inheritDoc}
     */
    @Override
    public void addHeader(String field, String value) {
        this.headers.put(field, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addQueryParameter(String field, String value) {
        this.queryParameters.put(field, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCookie(String domain, String field, String value) {
        this.cookies.add(new HttpClientRequestCookie(domain, field, value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEntity(String entity) {
        this.entity = entity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMethod() {
        return method;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUri() {
        return uri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEntity() {
        return entity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<HttpClientRequestCookie> getCookies() {
        return cookies;
    }

}
