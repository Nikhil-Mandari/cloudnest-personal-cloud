package com.cloudnest.share.exception;

/**
 * Exception thrown when attempting to access an expired share.
 */
public class ShareExpiredException extends RuntimeException {

    public ShareExpiredException(String message) {
        super(message);
    }
}
