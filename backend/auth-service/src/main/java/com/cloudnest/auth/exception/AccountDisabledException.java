package com.cloudnest.auth.exception;

/**
 * Thrown when a sign-in attempt targets a disabled account (403).
 */
public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException(String message) {
        super(message);
    }
}
