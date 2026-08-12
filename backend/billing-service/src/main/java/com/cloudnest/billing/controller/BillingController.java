package com.cloudnest.billing.controller;

import com.cloudnest.billing.dto.CreateOrderRequest;
import com.cloudnest.billing.dto.PaymentOrderResponse;
import com.cloudnest.billing.dto.PlanResponse;
import com.cloudnest.billing.dto.QuotaResponse;
import com.cloudnest.billing.dto.SubscriptionResponse;
import com.cloudnest.billing.dto.VerifyPaymentRequest;
import com.cloudnest.billing.service.BillingService;
import com.cloudnest.billing.util.StandardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for billing: storage plans, subscriptions, payment
 * orders, payment verification, quota and Razorpay webhooks.
 */
@Slf4j
@RestController
@RequestMapping("/api/billing")
@Tag(name = "Billing", description = "Storage plans, subscriptions, payment orders and quota")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    /** Lists the available storage plans (public, no auth required). */
    @GetMapping("/plans")
    @Operation(summary = "List storage plans")
    public ResponseEntity<StandardResponse<List<PlanResponse>>> getPlans(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(StandardResponse.<List<PlanResponse>>builder()
                .success(true)
                .message("Plans retrieved successfully")
                .data(billingService.getActivePlans())
                .path(httpRequest.getRequestURI())
                .build());
    }

    /** Returns the authenticated user's current subscription. */
    @GetMapping("/subscription")
    @Operation(summary = "Get current subscription")
    public ResponseEntity<StandardResponse<SubscriptionResponse>> getSubscription(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(StandardResponse.<SubscriptionResponse>builder()
                .success(true)
                .message("Subscription retrieved successfully")
                .data(billingService.getCurrentSubscription(userIdHeader))
                .path(httpRequest.getRequestURI())
                .build());
    }

    /** Returns the authenticated user's storage quota. */
    @GetMapping("/quota")
    @Operation(summary = "Get storage quota")
    public ResponseEntity<StandardResponse<QuotaResponse>> getQuota(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(StandardResponse.<QuotaResponse>builder()
                .success(true)
                .message("Quota retrieved successfully")
                .data(billingService.getQuota(userIdHeader))
                .path(httpRequest.getRequestURI())
                .build());
    }

    /** Creates a payment order for a plan upgrade. */
    @PostMapping("/orders")
    @Operation(summary = "Create a payment order")
    public ResponseEntity<StandardResponse<PaymentOrderResponse>> createOrder(
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        log.info("POST /api/billing/orders - userId={}, plan={}", userIdHeader, request.getPlanType());
        PaymentOrderResponse order = billingService.createOrder(userIdHeader, request.getPlanType());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.<PaymentOrderResponse>builder()
                        .success(true)
                        .message("Payment order created")
                        .data(order)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /** Verifies a provider payment confirmation (server-side signature check). */
    @PostMapping("/orders/verify")
    @Operation(summary = "Verify a payment")
    public ResponseEntity<StandardResponse<PaymentOrderResponse>> verifyPayment(
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Valid @RequestBody VerifyPaymentRequest request,
            HttpServletRequest httpRequest) {
        log.info("POST /api/billing/orders/verify - userId={}, orderUuid={}",
                userIdHeader, request.getOrderUuid());
        PaymentOrderResponse order = billingService.verifyPayment(userIdHeader, request);
        return ResponseEntity.ok(StandardResponse.<PaymentOrderResponse>builder()
                .success(true)
                .message("Payment verified successfully")
                .data(order)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /** Cancels a pending order (user abandoned checkout). */
    @PostMapping("/orders/{orderUuid}/cancel")
    @Operation(summary = "Cancel a payment order")
    public ResponseEntity<StandardResponse<PaymentOrderResponse>> cancelOrder(
            @RequestHeader("X-User-Id") Long userIdHeader,
            @PathVariable String orderUuid,
            HttpServletRequest httpRequest) {
        PaymentOrderResponse order = billingService.cancelOrder(userIdHeader, orderUuid);
        return ResponseEntity.ok(StandardResponse.<PaymentOrderResponse>builder()
                .success(true)
                .message("Order cancelled")
                .data(order)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /** Returns the authenticated user's order history. */
    @GetMapping("/orders")
    @Operation(summary = "Get order history")
    public ResponseEntity<StandardResponse<List<PaymentOrderResponse>>> getOrderHistory(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(StandardResponse.<List<PaymentOrderResponse>>builder()
                .success(true)
                .message("Orders retrieved successfully")
                .data(billingService.getOrderHistory(userIdHeader))
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Razorpay webhook endpoint (public — verified by signature).
     * Only a signature-verified event can mark an order paid.
     */
    @PostMapping("/webhook/razorpay")
    @Operation(summary = "Razorpay webhook (signature verified)")
    public ResponseEntity<StandardResponse<Void>> razorpayWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            HttpServletRequest httpRequest) {
        log.info("POST /api/billing/webhook/razorpay - bytes={}", rawBody.length());
        billingService.handleRazorpayWebhook(rawBody, signature);
        return ResponseEntity.ok(StandardResponse.<Void>builder()
                .success(true)
                .message("Webhook processed")
                .path(httpRequest.getRequestURI())
                .build());
    }
}
