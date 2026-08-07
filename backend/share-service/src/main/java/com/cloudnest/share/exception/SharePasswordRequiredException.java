package com.cloudnest.share.exception;

/**
 * Thrown when a password-protected share link is accessed without a password.
 */
public class SharePasswordRequiredException extends RuntimeException {

    public SharePasswordRequiredException(String message) {
        super(message);
    }
}
