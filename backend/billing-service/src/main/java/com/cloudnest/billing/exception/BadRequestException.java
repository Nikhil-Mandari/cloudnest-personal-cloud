package com.cloudnest.billing.exception;

/**
 * Thrown when a request is invalid (bad plan, expired order, etc.).
 */
public class BadRequestException extends BillingException {

    public BadRequestException(String message) {
        super(message);
    }
}
