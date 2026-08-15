package com.cloudnest.file.exception;

/**
 * Exception thrown when attempting to create or update a resource
 * that conflicts with an existing one (e.g. duplicate stored file name).
 * <p>
 * Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
