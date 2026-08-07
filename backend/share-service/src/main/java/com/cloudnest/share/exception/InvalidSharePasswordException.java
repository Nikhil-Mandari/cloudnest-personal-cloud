package com.cloudnest.share.exception;

/**
 * Thrown when an incorrect password is supplied for a password-protected share.
 */
public class InvalidSharePasswordException extends RuntimeException {

    public InvalidSharePasswordException(String message) {
        super(message);
    }
}
