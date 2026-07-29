package com.cloudnest.file.exception;

/**
 * Exception thrown when the client sends an invalid request.
 * <p>
 * Mapped to HTTP 400 by {@link GlobalExceptionHandler}.
 * Use for business rule violations that are not covered by validation annotations.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
