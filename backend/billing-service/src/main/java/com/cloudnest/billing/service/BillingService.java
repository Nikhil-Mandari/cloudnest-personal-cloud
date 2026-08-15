package com.cloudnest.billing.service;

import com.cloudnest.billing.dto.PaymentOrderResponse;
import com.cloudnest.billing.dto.PlanResponse;
import com.cloudnest.billing.dto.QuotaResponse;
import com.cloudnest.billing.dto.SubscriptionResponse;
import com.cloudnest.billing.dto.VerifyPaymentRequest;
import com.cloudnest.billing.entity.PlanType;

import java.util.List;

/**
 * Core billing operations: plans, subscriptions, payment orders,
 * payment verification, webhook handling and quota resolution.
 */
public interface BillingService {

    List<PlanResponse> getActivePlans();

    SubscriptionResponse getCurrentSubscription(Long userId);

    /** Creates a payment order for a plan upgrade (503 when provider unconfigured). */
    PaymentOrderResponse createOrder(Long userId, PlanType planType);

    /**
     * Verifies a provider payment confirmation and — only when the
     * signature validates — marks the order PAID and upgrades the plan.
     */
    PaymentOrderResponse verifyPayment(Long userId, VerifyPaymentRequest request);

    PaymentOrderResponse cancelOrder(Long userId, String orderUuid);

    List<PaymentOrderResponse> getOrderHistory(Long userId);

    /** Storage quota granted by the user's current subscription. */
    QuotaResponse getQuota(Long userId);

    /**
     * Handles a Razorpay webhook event after verifying its signature.
     * Idempotent: repeated events never double-credit quota.
     */
    void handleRazorpayWebhook(String rawBody, String signature);
}
