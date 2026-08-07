package com.cloudnest.auth.exception;

/**
 * Thrown when the OTP has passed its expiry window.
 */
public class OtpExpiredException extends OtpException {

    public OtpExpiredException(String message) {
        super(message);
    }
}
