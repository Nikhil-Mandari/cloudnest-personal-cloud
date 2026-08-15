package com.cloudnest.file.exception;

/**
 * Exception thrown when a file storage operation (upload / download / preview /
 * delete) cannot be completed.
 * <p>
 * Mapped to HTTP 500 by {@link GlobalExceptionHandler}.
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
