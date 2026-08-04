package com.cloudnest.file.exception;

/**
 * Exception thrown when the caller is not authenticated / no user identity
 * could be resolved (e.g. missing {@code X-User-Id} context).
 * <p>
 * Mapped to HTTP 401 by {@link GlobalExceptionHandler}.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
