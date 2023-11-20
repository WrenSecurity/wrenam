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
 * Copyright 2023 Wren Security
 */
package org.wrensecurity.wrenam.authentication.modules.duo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * Cisco Duo authentication client.
 */
public class DuoClient {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final DuoClientConfig config;
    private final DuoAuthorizationResolver authResolver;
    private final HttpClient client;

    public DuoClient(DuoClientConfig config) {
        this.config = config;
        this.authResolver = new DuoAuthorizationResolver(config.getSecretKey());
        this.client = HttpClient.newBuilder().build();
    }

    /**
     * Initialize second-factor authentication for user with the specified username.
     * @param username Username of the user to initialize authentication. Never null.
     * @return Transaction identifier. Never null.
     */
    public String initAuth(String username) {
        URI uri = URI.create(config.getApiBaseUrl() + "/auth/v2/auth");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("async", "true");
        params.put("device", "auto");
        params.put("factor", "push");
        params.put("username", username);
        String formData = encodeFormData(params);
        String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now());
        // Resolve password for the request
        String password = authResolver.resolvePassword(date, "POST", uri.getHost(), uri.getPath(), formData);
        // Prepare HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", resolveAuthorizationHeader(password))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Date", date)
                .POST(BodyPublishers.ofString(formData))
                .timeout(Duration.ofSeconds(30))
                .build();
        // Perform HTTP request
        HttpResponse<String> response = sendRequest(request);
        // Process response body
        DuoAuthResponse authResponse = checkAuthResponse(response.body());
        if (StringUtils.isBlank(authResponse.getTransactionId())) {
            throw new IllegalStateException("Missing transaction ID for '" + username + "'.");
        }
        return authResponse.getTransactionId();
    }

    /**
     * Get status of authentication for the specified transaction identifier.
     * @param transactionId Transaction identifier to get status. Never null.
     * @return Authentication status. Never null.
     */
    public CompletableFuture<DuoAuthStatus> getAuthStatus(String transactionId) {
        URI uri = URI.create(config.getApiBaseUrl() + "/auth/v2/auth_status?txid="
                + URLEncoder.encode(transactionId, StandardCharsets.UTF_8));
        String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now());
        // Resolve password for the request
        String password = authResolver.resolvePassword(date, "GET", uri.getHost(), uri.getPath(), uri.getQuery());
        // Prepare HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", resolveAuthorizationHeader(password))
                .header("Date", date)
                .GET()
                .timeout(Duration.ofMinutes(2))
                .build();
        // Perform HTTP request
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Invalid HTTP response status. HTTP status code: '"
                        + response.statusCode() + "', Response: '" + response.body() + "'.");
            }
            DuoAuthResponse authResponse = checkAuthResponse(response.body());
            if (authResponse.getAuthStatus() == null) {
                throw new IllegalStateException("Invalid authentication status for '" + transactionId + "'.");
            }
            return authResponse.getAuthStatus();
        });
    }

    /**
     * Send the specified HTTP request.
     * @param request HTTP request to be send. Never null.
     * @return {@link HttpResponse} instance. Never null.
     */
    private HttpResponse<String> sendRequest(HttpRequest request) {
        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send request to Cisco Duo API'.", e);
        }
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Invalid HTTP response status. HTTP status code: '"
                    + response.statusCode() + "', Response: '" + response.body() + "'.");
        }
        return response;
    }

    /**
     * Check status of Cisco Duo authentication response.
     * @param response Cisco Duo authentication response serialized in JSON format. Never null.
     * @return Parsed {@link DuoAuthResponse} when response is valid. Never null.
     */
    private DuoAuthResponse checkAuthResponse(String response) {
        DuoAuthResponse authResponse = null;
        try {
            authResponse = mapper.readValue(response, DuoAuthResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse authentication response.", e);
        }
        if (!authResponse.isOk()) {
            throw new IllegalStateException("Invalid response status. Response status: '"
                    + authResponse.getStatus() + "'.");
        }
        return authResponse;
    }

    /**
     * Serialize specified data as 'x-www-form-urlencoded' value string.
     */
    private String encodeFormData(Map<String, String> data) {
        return data.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    /**
     * Resolve HTTP authorization header value (i.e. HTTP BASIC authentication value).
     */
    private String resolveAuthorizationHeader(String password) {
        String credentials = config.getIntegrationKey() + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    /**
     * Class representing Cisco Duo authentication response.
     */
    static class DuoAuthResponse {

        @JsonProperty("stat")
        private String status;
        private Map<String, String> response;

        public String getStatus() {
            return status;
        }
        public void setStatus(String status) {
            this.status = status;
        }

        public Map<String, String> getResponse() {
            return response;
        }
        public void setResponse(Map<String, String> response) {
            this.response = response;
        }

        @JsonIgnore
        public boolean isOk() {
            return "OK".equalsIgnoreCase(status);
        }

        @JsonIgnore
        public String getTransactionId() {
            return response != null ? response.get("txid") : null;
        }

        @JsonIgnore
        public DuoAuthStatus getAuthStatus() {
            if (response == null || StringUtils.isBlank(response.get("result"))) {
                return null;
            }
            return DuoAuthStatus.valueOf(response.get("result").toUpperCase());
        }

    }

}
