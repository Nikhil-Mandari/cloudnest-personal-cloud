package com.cloudnest.file.exception;

/**
 * Exception thrown when the authenticated user tries to access a file they do
 * not own.
 * <p>
 * Mapped to HTTP 403 by {@link GlobalExceptionHandler}.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
