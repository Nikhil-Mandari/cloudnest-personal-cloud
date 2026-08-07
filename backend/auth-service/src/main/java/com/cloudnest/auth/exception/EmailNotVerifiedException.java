package com.cloudnest.auth.exception;

/**
 * Thrown when a sign-in is attempted on an account whose email address has
 * not been verified via the registration OTP. Maps to HTTP 403.
 */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
