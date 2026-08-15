package com.cloudnest.file.exception;

/**
 * Thrown when an upload would exceed the user's storage quota.
 */
public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(String message) {
        super(message);
    }
}
