package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A registered passkey shown on the Security page.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyCredentialResponse {

    /** Internal id (used to delete the credential). */
    private String id;

    /** User-assigned label. */
    private String nickname;

    /** Authenticator transports (e.g. internal, usb, nfc, hybrid). */
    private List<String> transports;

    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
