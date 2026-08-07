package com.cloudnest.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result of requesting a TOTP 2FA setup: a fresh secret plus the otpauth URI
 * the frontend renders as a QR code (or lets the user enter manually).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TwoFactorSetupResponse {

    /** Base32 secret the authenticator app stores. */
    private String secret;

    /** otpauth://totp/… URI (QR payload). */
    private String otpauthUri;

    /** Account label used inside the URI (username). */
    private String accountName;

    /** Issuer label (app name). */
    private String issuer;

    private int digits;
    private int periodSeconds;
}
