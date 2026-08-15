package com.cloudnest.billing.dto;

import com.cloudnest.billing.entity.PlanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * The storage quota granted to a user by their current subscription.
 * <p>
 * Consumed by the File Service (via Feign) to enforce upload limits and by
 * the frontend to render the usage meter.
 */
@Getter
@Builder
@AllArgsConstructor
public class QuotaResponse {

    private final PlanType planType;

    /** Maximum allowed storage in bytes. */
    private final Long quotaBytes;
}
