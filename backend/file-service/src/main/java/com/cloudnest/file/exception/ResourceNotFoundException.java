package com.cloudnest.file.exception;

/**
 * Exception thrown when a requested resource is not found.
 * <p>
 * Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
