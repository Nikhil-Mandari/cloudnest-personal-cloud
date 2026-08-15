package com.cloudnest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Finishes a passkey sign-in with the browser's assertion response. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyAuthenticationFinish {

    /** The exact {@code requestJson} returned by authenticate/start. */
    @NotBlank(message = "requestJson is required")
    private String requestJson;

    /** The serialized {@code PublicKeyCredential} from {@code navigator.credentials.get}. */
    @NotBlank(message = "responseJson is required")
    private String responseJson;
}
