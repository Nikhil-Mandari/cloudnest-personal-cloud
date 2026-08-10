package com.cloudnest.auth.exception;

/**
 * Thrown when an action is attempted too soon (e.g. OTP resend inside the
 * server-side cooldown window). Mapped to HTTP 429 Too Many Requests.
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
