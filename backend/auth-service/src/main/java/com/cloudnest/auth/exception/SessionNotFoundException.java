package com.cloudnest.auth.exception;

/**
 * Thrown when a session id cannot be found (unknown or already ended).
 */
public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(String message) {
        super(message);
    }
}
