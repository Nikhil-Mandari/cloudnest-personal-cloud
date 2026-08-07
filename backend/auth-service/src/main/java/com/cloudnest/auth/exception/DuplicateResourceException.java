package com.cloudnest.auth.exception;

/**
 * Exception thrown when attempting to create a resource that already exists
 * (e.g. duplicate username or email during registration).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
