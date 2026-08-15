package com.cloudnest.billing.exception;

/**
 * Base class for all billing-service domain exceptions.
 */
public class BillingException extends RuntimeException {

    public BillingException(String message) {
        super(message);
    }

    public BillingException(String message, Throwable cause) {
        super(message, cause);
    }
}
