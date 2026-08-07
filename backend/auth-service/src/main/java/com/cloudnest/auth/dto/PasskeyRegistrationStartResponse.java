package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registration ceremony start: the WebAuthn creation options JSON the browser
 * passes to {@code navigator.credentials.create(...)}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyRegistrationStartResponse {

    /** Serialized {@code PublicKeyCredentialCreationOptions}. */
    private String optionsJson;
}
