package com.cloudnest.folder.exception;

/**
 * Exception thrown when attempting to create or rename a folder whose name
 * conflicts with an existing folder at the same hierarchical level.
 * <p>
 * Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class DuplicateFolderException extends RuntimeException {

    public DuplicateFolderException(String message) {
        super(message);
    }
}
