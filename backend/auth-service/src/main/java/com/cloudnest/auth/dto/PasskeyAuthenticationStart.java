package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Start of a passkey sign-in: the assertion request echoed back on finish. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyAuthenticationStart {

    /** The full server-side {@code AssertionRequest} serialized as JSON (echoed on finish). */
    private String requestJson;

    /** {@code PublicKeyCredentialRequestOptions} as JSON for {@code navigator.credentials.get}. */
    private String credentialsGetJson;
}
