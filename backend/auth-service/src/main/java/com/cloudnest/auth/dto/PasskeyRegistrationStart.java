package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Start of a passkey registration ceremony: creation options for the browser. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyRegistrationStart {

    /** {@code PublicKeyCredentialCreationOptions} as JSON for {@code navigator.credentials.create}. */
    private String optionsJson;
}
