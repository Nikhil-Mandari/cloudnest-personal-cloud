package com.cloudnest.file.exception;

/**
 * Exception thrown when MinIO cannot be reached or the configured bucket
 * cannot be created during startup / on-demand initialisation.
 * <p>
 * Mapped to HTTP 500 by {@link GlobalExceptionHandler} (via the
 * {@link MinioException} handler).
 */
public class BucketCreationException extends MinioException {

    public BucketCreationException(String message) {
        super(message);
    }

    public BucketCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
