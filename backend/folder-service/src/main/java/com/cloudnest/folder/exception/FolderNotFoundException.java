package com.cloudnest.folder.exception;

/**
 * Exception thrown when a requested folder is not found.
 * <p>
 * Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class FolderNotFoundException extends RuntimeException {

    public FolderNotFoundException(String message) {
        super(message);
    }
}
