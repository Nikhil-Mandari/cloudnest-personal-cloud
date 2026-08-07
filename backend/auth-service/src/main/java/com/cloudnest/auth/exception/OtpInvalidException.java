package com.cloudnest.auth.exception;

/**
 * Thrown when the submitted OTP code is incorrect, missing, or there is no
 * active code for the user/purpose.
 */
public class OtpInvalidException extends OtpException {

    public OtpInvalidException(String message) {
        super(message);
    }
}
