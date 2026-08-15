package com.cloudnest.billing.entity;

/**
 * The storage plans offered by CloudNest.
 * <p>
 * The FREE plan is the default quota for every account; the paid plans
 * upgrade the user's storage quota when their subscription is active.
 */
public enum PlanType {
    FREE,
    PLUS,
    PRO,
    PREMIUM
}
