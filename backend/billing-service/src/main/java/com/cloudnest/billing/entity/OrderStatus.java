package com.cloudnest.billing.entity;

/**
 * Lifecycle of a payment order.
 * <p>
 * An order only grants quota when it reaches {@code PAID} through a
 * verified provider confirmation (signature check or webhook) — never
 * through a frontend callback alone.
 */
public enum OrderStatus {
    /** Created locally (and with the payment provider when configured). */
    CREATED,

    /** Provider confirmed the payment and the signature/event verified. */
    PAID,

    /** Provider reported the payment as failed. */
    FAILED,

    /** The user abandoned the checkout. */
    CANCELLED,

    /** The order exceeded its validity window without being paid. */
    EXPIRED
}
