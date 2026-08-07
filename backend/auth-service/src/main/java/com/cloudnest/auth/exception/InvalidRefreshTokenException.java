package com.cloudnest.auth.exception;

/**
 * Thrown when a refresh token is missing, malformed, revoked, expired, or
 * bound to an ended session. Maps to HTTP 401 so the client re-authenticates.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
