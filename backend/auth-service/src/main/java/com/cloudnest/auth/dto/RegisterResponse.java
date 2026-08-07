package com.cloudnest.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response returned after a registration request: the account is created in
 * the {@code PENDING_VERIFICATION} state and an activation OTP is emailed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterResponse {

    private Long userId;
    private String email;
    private String message;

    /**
     * The plain OTP — present <em>only</em> when email delivery is disabled
     * (development mode) so the flow can be exercised without an SMTP server.
     * Never present in production.
     */
    private String devOtp;

    /** Seconds until a resend is permitted. */
    private Long resendAfterSeconds;

    /** Minutes before the OTP expires. */
    private Integer otpExpiryMinutes;
}
