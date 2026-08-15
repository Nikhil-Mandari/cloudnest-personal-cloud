package com.cloudnest.billing.entity;

/**
 * Lifecycle of a user's subscription.
 */
public enum SubscriptionStatus {
    /** The plan is currently active and grants its storage quota. */
    ACTIVE,

    /** The subscription period ended and the user is back on FREE quota. */
    EXPIRED,

    /** Explicitly cancelled — the quota is reverted on expiry. */
    CANCELLED
}
