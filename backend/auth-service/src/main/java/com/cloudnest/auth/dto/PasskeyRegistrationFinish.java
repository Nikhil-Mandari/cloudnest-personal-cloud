package com.cloudnest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Finishes a passkey registration with the browser's credential response. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyRegistrationFinish {

    /** The exact {@code optionsJson} returned by register/start. */
    @NotBlank(message = "optionsJson is required")
    private String optionsJson;

    /** The serialized {@code PublicKeyCredential} from {@code navigator.credentials.create}. */
    @NotBlank(message = "responseJson is required")
    private String responseJson;

    /** Optional user-chosen label for the passkey. */
    private String nickname;
}
