package com.cloudnest.notification.exception;

/**
 * Exception thrown when a notification record is not found.
 */
public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(String message) {
        super(message);
    }
}
