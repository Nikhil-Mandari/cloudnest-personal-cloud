package com.cloudnest.file.exception;

/**
 * Thrown when a non-admin caller reaches an admin-only endpoint (403).
 */
public class AdminAccessDeniedException extends RuntimeException {

    public AdminAccessDeniedException(String message) {
        super(message);
    }
}
