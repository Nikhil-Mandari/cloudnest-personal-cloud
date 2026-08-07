package com.cloudnest.auth.exception;

/**
 * Thrown when a trusted device record cannot be found.
 */
public class TrustedDeviceNotFoundException extends RuntimeException {

    public TrustedDeviceNotFoundException(String message) {
        super(message);
    }
}
