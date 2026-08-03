package com.cloudnest.file.exception;

/**
 * Exception thrown when an uploaded file exceeds the configured maximum size.
 * <p>
 * Mapped to HTTP 413 by {@link GlobalExceptionHandler}.
 */
public class FileTooLargeException extends RuntimeException {

    public FileTooLargeException(String message) {
        super(message);
    }
}
