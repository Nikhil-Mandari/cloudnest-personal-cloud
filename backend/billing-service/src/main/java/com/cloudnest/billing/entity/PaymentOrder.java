package com.cloudnest.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * A payment order created for a plan upgrade.
 * <p>
 * The order references a Razorpay order id when the provider is
 * configured. Quota is only upgraded after the provider confirmation is
 * verified server-side (signature check or webhook).
 */
@Entity
@Table(name = "payment_orders")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public-facing UUID returned to the frontend. */
    @Column(name = "order_uuid", nullable = false, unique = true, length = 36)
    private String orderUuid;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 20)
    private PlanType planType;

    /** Amount in INR (paise*100). */
    @Column(name = "amount_inr", nullable = false)
    private Long amountInr;

    @Column(name = "currency", nullable = false, length = 8)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    /** Razorpay order id (null when the provider is not configured). */
    @Column(name = "provider_order_id", length = 64)
    private String providerOrderId;

    /** Razorpay payment id confirmed by the webhook / signature check. */
    @Column(name = "provider_payment_id", length = 64)
    private String providerPaymentId;

    /** Razorpay signature verified server-side before marking PAID. */
    @Column(name = "signature", length = 128)
    private String signature;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
