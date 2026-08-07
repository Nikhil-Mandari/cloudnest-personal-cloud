package com.cloudnest.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response for OTP dispatch endpoints that must not reveal account existence
 * (forgot-password). When the account exists a {@code challengeToken} is
 * returned so the flow can continue; otherwise only the generic message is.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpDispatchResponse {

    /** Whether an OTP was actually dispatched. */
    private boolean sent;

    /** Short-lived JWT for the pending reset (only when the account exists). */
    private String challengeToken;

    /** Dev-only plain OTP when email delivery is disabled. */
    private String devOtp;

    /** Seconds until a resend is permitted. */
    private Long resendAfterSeconds;

    /** Minutes before the OTP expires. */
    private Integer otpExpiryMinutes;
}
