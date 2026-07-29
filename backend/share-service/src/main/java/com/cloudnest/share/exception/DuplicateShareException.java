package com.cloudnest.share.exception;

/**
 * Exception thrown when attempting to create a duplicate share
 * (e.g. sharing the same resource with the same user twice).
 */
public class DuplicateShareException extends RuntimeException {

    public DuplicateShareException(String message) {
        super(message);
    }
}
