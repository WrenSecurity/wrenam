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
 * Copyright 2025 Wren Security. All rights reserved.
 */
package org.wrensecurity.wrenam.authentication.modules.webauthn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Locale;

/**
 * Parses the hidden callback payload returned by WebAuthn browser scripts.
 */
public class WebAuthnCallbackResultParser {

    // Credential JSON stays small in practice; keep a hard cap for malformed or abusive payloads
    private static final int MAX_CALLBACK_PAYLOAD_LENGTH = 256 * 1024;

    private static final int MAX_FAILURE_DETAIL_LENGTH = 240;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String STATUS_OK = "ok";

    private static final String STATUS_ERROR = "error";

    private WebAuthnCallbackResultParser() {
    }

    public static Result parse(String callbackPayload, boolean hasError) {
        if (callbackPayload == null || callbackPayload.isBlank()) {
            if (hasError) {
                return Result.failure(WebAuthnFailureReason.NOT_ALLOWED, null);
            }
            return Result.failure(WebAuthnFailureReason.VERIFICATION_FAILED, "Missing WebAuthn response payload.");
        }
        if (callbackPayload.length() > MAX_CALLBACK_PAYLOAD_LENGTH) {
            return Result.failure(
                    WebAuthnFailureReason.VERIFICATION_FAILED,
                    "WebAuthn response payload exceeded supported size.");
        }

        JsonNode root = parsePayload(callbackPayload);
        if (root == null || !root.isObject()) {
            return Result.failure(WebAuthnFailureReason.VERIFICATION_FAILED, "Invalid WebAuthn response payload.");
        }

        String status = normalizeStatus(root.path("status").asText(null));
        if (STATUS_OK.equals(status)) {
            return parseSuccessPayload(root, hasError);
        }
        if (STATUS_ERROR.equals(status)) {
            return parseFailurePayload(root);
        }
        if (hasError) {
            return Result.failure(WebAuthnFailureReason.NOT_ALLOWED, null);
        }

        return Result.failure(WebAuthnFailureReason.VERIFICATION_FAILED, "Invalid WebAuthn response status.");
    }

    private static Result parseSuccessPayload(JsonNode root, boolean hasError) {
        if (hasError) {
            return Result.failure(WebAuthnFailureReason.VERIFICATION_FAILED, "Inconsistent WebAuthn callback state.");
        }
        JsonNode credential = root.get("credential");
        if (credential == null || credential.isNull() || credential.isMissingNode()) {
            return Result.failure(
                    WebAuthnFailureReason.VERIFICATION_FAILED,
                    "Missing credential in WebAuthn success payload.");
        }
        return Result.success(credential.toString());
    }

    private static Result parseFailurePayload(JsonNode root) {
        WebAuthnFailureReason reason = WebAuthnFailureReason.fromReasonCode(root.path("reason").asText(null));
        String message = sanitizeFailureDetail(root.path("message").asText(null));
        return Result.failure(reason, message);
    }

    private static JsonNode parsePayload(String payload) {
        try {
            return OBJECT_MAPPER.readTree(payload);
        } catch (IOException e) {
            return null;
        }
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim().toLowerCase(Locale.ROOT);
    }

    private static String sanitizeFailureDetail(String detail) {
        if (detail == null || detail.isBlank()) {
            return null;
        }

        String normalized = detail
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() > MAX_FAILURE_DETAIL_LENGTH
                ? normalized.substring(0, MAX_FAILURE_DETAIL_LENGTH) + "..."
                : normalized;
    }

    public static final class Result {

        private final boolean success;

        private final String credentialJson;

        private final WebAuthnFailureReason failureReason;

        private final String failureMessage;

        private Result(boolean success, String credentialJson, WebAuthnFailureReason failureReason,
                String failureMessage) {
            this.success = success;
            this.credentialJson = credentialJson;
            this.failureReason = failureReason;
            this.failureMessage = failureMessage;
        }

        static Result success(String credentialJson) {
            return new Result(true, credentialJson, null, null);
        }

        static Result failure(WebAuthnFailureReason reason, String message) {
            return new Result(false, null, reason, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getCredentialJson() {
            return credentialJson;
        }

        public WebAuthnFailureReason getFailureReason() {
            return failureReason;
        }

        public String getFailureMessage() {
            return failureMessage;
        }
    }
}
