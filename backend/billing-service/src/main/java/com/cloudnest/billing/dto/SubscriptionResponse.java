package com.cloudnest.billing.dto;

import com.cloudnest.billing.entity.PlanType;
import com.cloudnest.billing.entity.Subscription;
import com.cloudnest.billing.entity.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * The current subscription of a user (defaults to the FREE plan).
 */
@Getter
@Builder
@AllArgsConstructor
public class SubscriptionResponse {

    private final Long userId;

    private final PlanType planType;

    private final SubscriptionStatus status;

    private final LocalDateTime startsAt;

    private final LocalDateTime expiresAt;

    /** Storage quota granted by the subscribed plan, in bytes. */
    private final Long quotaBytes;

    public static SubscriptionResponse of(Subscription subscription, Long quotaBytes) {
        return SubscriptionResponse.builder()
                .userId(subscription.getUserId())
                .planType(subscription.getPlanType())
                .status(subscription.getStatus())
                .startsAt(subscription.getStartsAt())
                .expiresAt(subscription.getExpiresAt())
                .quotaBytes(quotaBytes)
                .build();
    }
}
