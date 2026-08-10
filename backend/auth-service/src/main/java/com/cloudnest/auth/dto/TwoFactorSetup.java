package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Secret + otpauth URI returned by the 2FA setup step (QR payload). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorSetup {

    private String secret;
    private String otpauthUri;
    private String accountName;
    private String issuer;
    private int digits;
    private int periodSeconds;
}
