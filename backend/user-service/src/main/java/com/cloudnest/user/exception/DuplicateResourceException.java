package com.cloudnest.user.exception;

/**
 * Exception thrown when attempting to create or update a resource
 * that conflicts with an existing one (e.g. duplicate email).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
