package com.cloudnest.file.exception;

/**
 * Exception thrown when an operation against MinIO object storage fails
 * (connectivity, permission, or SDK errors).
 * <p>
 * Mapped to HTTP 500 by {@link GlobalExceptionHandler}.
 */
public class MinioException extends RuntimeException {

    public MinioException(String message) {
        super(message);
    }

    public MinioException(String message, Throwable cause) {
        super(message, cause);
    }
}
