package com.cloudnest.billing.exception;

/**
 * Thrown when a payment order is requested but the payment provider
 * (Razorpay) is not configured via environment variables.
 * <p>
 * This is deliberate: CloudNest never fakes a successful payment. The API
 * returns 503 so the frontend can show a clean "payments unavailable"
 * state instead of claiming a checkout can start.
 */
public class PaymentProviderNotConfiguredException extends BillingException {

    public PaymentProviderNotConfiguredException(String message) {
        super(message);
    }
}
