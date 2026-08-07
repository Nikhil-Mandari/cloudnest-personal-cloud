package com.cloudnest.auth.exception;

/**
 * Thrown when the verification attempt budget for an OTP is exhausted.
 */
public class OtpMaxAttemptsException extends OtpException {

    public OtpMaxAttemptsException(String message) {
        super(message);
    }
}
