package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Passkey sign-in start: the assertion request JSON is echoed back on finish
 * (the server is stateless), while {@code credentialsGetJson} is what the
 * browser passes to {@code navigator.credentials.get(...)}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyAuthenticationStartResponse {

    /** Serialized {@code AssertionRequest} — returned on finish. */
    private String requestJson;

    /** Serialized {@code PublicKeyCredentialRequestOptions} for the browser. */
    private String credentialsGetJson;
}
