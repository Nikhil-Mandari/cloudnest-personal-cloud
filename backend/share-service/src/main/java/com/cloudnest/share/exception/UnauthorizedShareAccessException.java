package com.cloudnest.share.exception;

/**
 * Exception thrown when a user attempts to access or modify a share they do not own.
 */
public class UnauthorizedShareAccessException extends RuntimeException {

    public UnauthorizedShareAccessException(String message) {
        super(message);
    }
}
