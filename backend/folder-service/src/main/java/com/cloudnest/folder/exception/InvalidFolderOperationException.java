package com.cloudnest.folder.exception;

/**
 * Exception thrown when an invalid folder operation is attempted,
 * such as moving a folder into itself or into one of its descendants.
 * <p>
 * Mapped to HTTP 400 by {@link GlobalExceptionHandler}.
 */
public class InvalidFolderOperationException extends RuntimeException {

    public InvalidFolderOperationException(String message) {
        super(message);
    }
}
