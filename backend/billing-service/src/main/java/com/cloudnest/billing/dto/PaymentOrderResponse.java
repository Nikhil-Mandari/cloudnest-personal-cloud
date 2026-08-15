package com.cloudnest.billing.dto;

import com.cloudnest.billing.entity.OrderStatus;
import com.cloudnest.billing.entity.PaymentOrder;
import com.cloudnest.billing.entity.PlanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Public representation of a payment order.
 * <p>
 * Contains only client-safe data — never the provider secret.
 */
@Getter
@Builder
@AllArgsConstructor
public class PaymentOrderResponse {

    private final String orderUuid;

    private final PlanType planType;

    private final Long amountInr;

    private final String currency;

    private final OrderStatus status;

    /** Provider order id (present only when the provider is configured). */
    private final String providerOrderId;

    /** Provider public key id — client-safe, required by the checkout SDK. */
    private final String providerKeyId;

    private final LocalDateTime createdAt;

    private final LocalDateTime paidAt;

    /** True when the payment provider is configured and checkout can start. */
    private final Boolean checkoutAvailable;

    public static PaymentOrderResponse from(PaymentOrder order, boolean checkoutAvailable, String providerKeyId) {
        return PaymentOrderResponse.builder()
                .orderUuid(order.getOrderUuid())
                .planType(order.getPlanType())
                .amountInr(order.getAmountInr())
                .currency(order.getCurrency())
                .status(order.getStatus())
                .providerOrderId(order.getProviderOrderId())
                .providerKeyId(providerKeyId)
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .checkoutAvailable(checkoutAvailable)
                .build();
    }
}
