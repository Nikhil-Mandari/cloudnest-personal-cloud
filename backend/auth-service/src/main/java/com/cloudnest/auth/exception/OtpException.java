package com.cloudnest.auth.exception;

/**
 * Base class for OTP verification failures. Maps to HTTP 400.
 */
public class OtpException extends RuntimeException {

    public OtpException(String message) {
        super(message);
    }
}
