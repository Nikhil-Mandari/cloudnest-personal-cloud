package com.cloudnest.share.exception;

/**
 * Exception thrown when a share record is not found.
 */
public class ShareNotFoundException extends RuntimeException {

    public ShareNotFoundException(String message) {
        super(message);
    }
}
