(function () {
    const STATUS_OK = "ok";
    const STATUS_ERROR = "error";
    const REASON_VERIFICATION_FAILED = "VERIFICATION_FAILED";
    const REASON_USER_CANCELLED = "USER_CANCELLED";
    const REASON_NOT_ALLOWED = "NOT_ALLOWED";
    const REASON_NO_AUTHENTICATOR = "NO_AUTHENTICATOR";
    const REASON_UNSUPPORTED = "UNSUPPORTED";
    const REASON_ORIGIN_MISMATCH = "ORIGIN_MISMATCH";
    const DEFAULT_ERROR_MESSAGE = "WebAuthn operation failed.";

    const output = document.getElementById("clientScriptOutputData");
    const successButton = document.getElementById("button_1");
    const errorButton = document.getElementById("button_2");

    if (!output || !successButton || !errorButton) {
        return;
    }

    const base64UrlToUtf8 = function (base64Url) {
        const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
        const paddedBase64 = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
        const binary = atob(paddedBase64);
        const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0));
        return new TextDecoder().decode(bytes);
    };

    const toFailure = function (error) {
        if (!error) {
            return { reason: REASON_VERIFICATION_FAILED, message: DEFAULT_ERROR_MESSAGE };
        }
        switch (error.name) {
        case "AbortError":
            return { reason: REASON_USER_CANCELLED, message: "WebAuthn registration request was cancelled." };
        case "NotAllowedError":
            return { reason: REASON_NOT_ALLOWED,
                message: "WebAuthn registration request was not allowed or timed out." };
        case "NotFoundError":
            return { reason: REASON_NO_AUTHENTICATOR, message: "No compatible authenticator was available." };
        case "NotSupportedError":
            return { reason: REASON_UNSUPPORTED, message: "WebAuthn is not supported by this browser or device." };
        case "SecurityError":
            return { reason: REASON_ORIGIN_MISMATCH,
                message: "WebAuthn was blocked due to origin or security restrictions." };
        case "ConstraintError":
            return { reason: REASON_NO_AUTHENTICATOR,
                message: "Authenticator could not satisfy the requested constraints." };
        case "InvalidStateError":
            return { reason: REASON_VERIFICATION_FAILED,
                message: "This passkey appears to already exist on the authenticator." };
        case "TypeError":
            return { reason: REASON_VERIFICATION_FAILED, message: "WebAuthn registration options are invalid." };
        default:
            return { reason: REASON_VERIFICATION_FAILED, message: error.message || DEFAULT_ERROR_MESSAGE };
        }
    };

    let completed = false;
    let timeoutHandle;

    const completeWithSuccess = function (credential) {
        if (completed) {
            return;
        }
        completed = true;
        clearTimeout(timeoutHandle);
        output.value = JSON.stringify({
            status: STATUS_OK,
            credential: credential && credential.toJSON ? credential.toJSON() : credential
        });
        successButton.click();
    };

    const completeWithError = function (reason, message) {
        if (completed) {
            return;
        }
        completed = true;
        clearTimeout(timeoutHandle);
        output.value = JSON.stringify({
            status: STATUS_ERROR,
            reason: reason || REASON_VERIFICATION_FAILED,
            message: message || DEFAULT_ERROR_MESSAGE
        });
        errorButton.click();
    };

    errorButton.addEventListener("click", function () {
        if (completed) {
            return;
        }
        completed = true;
        clearTimeout(timeoutHandle);
        output.value = JSON.stringify({
            status: STATUS_ERROR,
            reason: REASON_USER_CANCELLED,
            message: "User cancelled passkey setup."
        });
    });

    if (typeof PublicKeyCredential === "undefined" || !navigator.credentials
            || typeof navigator.credentials.create !== "function") {
        completeWithError(REASON_UNSUPPORTED, "WebAuthn is not supported by this browser.");
        return;
    }

    let optionsJson;
    try {
        optionsJson = JSON.parse(base64UrlToUtf8("{publicKeyB64}"));
    } catch (error) {
        completeWithError(REASON_VERIFICATION_FAILED, "Failed to parse WebAuthn registration options.");
        return;
    }

    let requestOptions;
    try {
        requestOptions = PublicKeyCredential.parseCreationOptionsFromJSON(optionsJson);
    } catch (error) {
        const failure = toFailure(error);
        completeWithError(failure.reason, failure.message);
        return;
    }

    const timeoutMs = Math.max((Number(optionsJson.timeout) || 60000) + 10000, 15000);
    timeoutHandle = setTimeout(function () {
        completeWithError(REASON_NOT_ALLOWED, "Timed out waiting for authenticator response.");
    }, timeoutMs);

    navigator.credentials.create({ publicKey: requestOptions })
        .then(completeWithSuccess)
        .catch(function (error) {
            const failure = toFailure(error);
            completeWithError(failure.reason, failure.message);
        });
})();
