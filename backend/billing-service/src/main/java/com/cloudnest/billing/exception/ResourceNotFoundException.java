package com.cloudnest.billing.exception;

/**
 * Thrown when a requested resource (plan, order, subscription) does not exist.
 */
public class ResourceNotFoundException extends BillingException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
