package com.cloudnest.share.exception;

/**
 * Thrown when a password-protected share link is accessed without the correct
 * password. Mapped to {@code 401 Unauthorized} so the frontend can re-show its
 * password gate (a 403 would conflate this with a plain permission denial).
 */
public class SharePasswordException extends RuntimeException {

    public SharePasswordException(String message) {
        super(message);
    }
}
