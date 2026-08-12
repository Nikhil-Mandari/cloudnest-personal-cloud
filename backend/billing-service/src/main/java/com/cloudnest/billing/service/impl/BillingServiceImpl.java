package com.cloudnest.billing.service.impl;

import com.cloudnest.billing.client.NotificationServiceClient;
import com.cloudnest.billing.dto.NotificationCreateRequest;
import com.cloudnest.billing.dto.PaymentOrderResponse;
import com.cloudnest.billing.dto.PlanResponse;
import com.cloudnest.billing.dto.QuotaResponse;
import com.cloudnest.billing.dto.SubscriptionResponse;
import com.cloudnest.billing.dto.VerifyPaymentRequest;
import com.cloudnest.billing.entity.OrderStatus;
import com.cloudnest.billing.entity.PaymentOrder;
import com.cloudnest.billing.entity.Plan;
import com.cloudnest.billing.entity.PlanType;
import com.cloudnest.billing.entity.Subscription;
import com.cloudnest.billing.entity.SubscriptionStatus;
import com.cloudnest.billing.exception.BadRequestException;
import com.cloudnest.billing.exception.PaymentProviderNotConfiguredException;
import com.cloudnest.billing.exception.ResourceNotFoundException;
import com.cloudnest.billing.repository.PaymentOrderRepository;
import com.cloudnest.billing.repository.PlanRepository;
import com.cloudnest.billing.repository.SubscriptionRepository;
import com.cloudnest.billing.service.BillingService;
import com.cloudnest.billing.service.RazorpayGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link BillingService}.
 * <p>
 * Payment safety rules enforced here:
 * <ul>
 *   <li>Quota is upgraded only after a server-side verification of the
 *       provider signature or a signature-verified webhook event.</li>
 *   <li>Verification is idempotent — an already-PAID order never grants
 *       quota twice.</li>
 *   <li>Without configured provider credentials, order creation returns
 *       503 instead of faking a checkout.</li>
 * </ul>
 */
@Slf4j
@Service
@Transactional
public class BillingServiceImpl implements BillingService {

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final RazorpayGateway razorpayGateway;
    private final NotificationServiceClient notificationServiceClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${billing.order-expiry-minutes:15}")
    private long orderExpiryMinutes;

    public BillingServiceImpl(
            PlanRepository planRepository,
            SubscriptionRepository subscriptionRepository,
            PaymentOrderRepository paymentOrderRepository,
            RazorpayGateway razorpayGateway,
            NotificationServiceClient notificationServiceClient) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.razorpayGateway = razorpayGateway;
        this.notificationServiceClient = notificationServiceClient;
    }

    // ── Plans & subscription ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> getActivePlans() {
        return planRepository.findByActiveTrueOrderByStorageBytesAsc().stream()
                .map(PlanResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getCurrentSubscription(Long userId) {
        requireUserId(userId);
        Subscription subscription = effectiveSubscription(userId);
        return SubscriptionResponse.of(subscription, quotaFor(subscription.getPlanType()));
    }

    @Override
    @Transactional(readOnly = true)
    public QuotaResponse getQuota(Long userId) {
        requireUserId(userId);
        Subscription subscription = effectiveSubscription(userId);
        return QuotaResponse.builder()
                .planType(subscription.getPlanType())
                .quotaBytes(quotaFor(subscription.getPlanType()))
                .build();
    }

    // ── Payment orders ───────────────────────────────────────────────────────

    @Override
    public PaymentOrderResponse createOrder(Long userId, PlanType planType) {
        requireUserId(userId);
        if (planType == null || planType == PlanType.FREE) {
            throw new BadRequestException("The FREE plan is the default and cannot be purchased");
        }

        Plan plan = planRepository.findByPlanType(planType)
                .filter(Plan::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planType));

        if (!razorpayGateway.isConfigured()) {
            log.warn("Order creation blocked for userId={}, plan={}: Razorpay credentials not configured",
                    userId, planType);
            throw new PaymentProviderNotConfiguredException(
                    "Payment provider is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET "
                            + "environment variables to enable payments.");
        }

        PaymentOrder order = PaymentOrder.builder()
                .orderUuid(UUID.randomUUID().toString())
                .userId(userId)
                .planType(planType)
                .amountInr(plan.getPriceInr())
                .currency(plan.getCurrency())
                .status(OrderStatus.CREATED)
                .build();

        // Create the provider order — this carries the real transaction intent
        // (UPI/card checkout and the dynamic payment QR come from the provider).
        String providerOrderId = razorpayGateway.createOrder(
                planType, plan.getPriceInr(), "order-" + order.getOrderUuid());
        order.setProviderOrderId(providerOrderId);
        PaymentOrder saved = paymentOrderRepository.save(order);

        log.info("Payment order created: orderUuid={}, providerOrderId={}, plan={}, amount={}",
                saved.getOrderUuid(), providerOrderId, planType, plan.getPriceInr());

        return PaymentOrderResponse.from(saved, true, razorpayGateway.getKeyId());
    }

    @Override
    public PaymentOrderResponse verifyPayment(Long userId, VerifyPaymentRequest request) {
        requireUserId(userId);
        PaymentOrder order = findOwnedOrder(request.getOrderUuid(), userId);

        // Idempotent: an already-paid order is returned as-is, never re-credited.
        if (order.getStatus() == OrderStatus.PAID) {
            log.info("Order {} already PAID — ignoring duplicate verification", order.getOrderUuid());
            return PaymentOrderResponse.from(order, true, razorpayGateway.getKeyId());
        }

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BadRequestException(
                    "Order is " + order.getStatus().name().toLowerCase() + " and cannot be verified");
        }

        if (isExpired(order)) {
            order.setStatus(OrderStatus.EXPIRED);
            paymentOrderRepository.save(order);
            throw new BadRequestException("Payment order has expired. Please create a new order.");
        }

        if (!razorpayGateway.isConfigured() || order.getProviderOrderId() == null) {
            throw new PaymentProviderNotConfiguredException(
                    "Payment provider is not configured — payment cannot be verified");
        }

        // Server-side signature check: orderId|paymentId signed with the key secret.
        boolean valid = razorpayGateway.verifyPaymentSignature(
                order.getProviderOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());
        if (!valid) {
            // Leave the order CREATED so the user can retry — a provider-confirmed
            // webhook can still recover a genuinely paid order.
            log.warn("Payment signature verification FAILED for order {}", order.getOrderUuid());
            throw new BadRequestException("Payment verification failed. The payment was not accepted.");
        }

        markPaid(order, request.getRazorpayPaymentId(), request.getRazorpaySignature(), null);
        return PaymentOrderResponse.from(order, true, razorpayGateway.getKeyId());
    }

    @Override
    public PaymentOrderResponse cancelOrder(Long userId, String orderUuid) {
        requireUserId(userId);
        PaymentOrder order = findOwnedOrder(orderUuid, userId);
        if (order.getStatus() == OrderStatus.CREATED) {
            order.setStatus(OrderStatus.CANCELLED);
            paymentOrderRepository.save(order);
        }
        return PaymentOrderResponse.from(order, razorpayGateway.isConfigured(), razorpayGateway.getKeyId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentOrderResponse> getOrderHistory(Long userId) {
        requireUserId(userId);
        boolean configured = razorpayGateway.isConfigured();
        String keyId = razorpayGateway.getKeyId();
        return paymentOrderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(order -> PaymentOrderResponse.from(order, configured, keyId))
                .toList();
    }

    // ── Webhook ──────────────────────────────────────────────────────────────

    @Override
    public void handleRazorpayWebhook(String rawBody, String signature) {
        if (!razorpayGateway.verifyWebhookSignature(rawBody, signature)) {
            log.warn("Rejecting Razorpay webhook with invalid signature");
            throw new BadRequestException("Invalid webhook signature");
        }

        try {
            JsonNode event = objectMapper.readTree(rawBody);
            String eventType = event.path("event").asText("");
            String paymentId = event.path("payload").path("payment").path("entity").path("id").asText(null);
            String orderId = event.path("payload").path("payment").path("entity").path("order_id").asText(null);
            String status = event.path("payload").path("payment").path("entity").path("status").asText("");

            if (paymentId == null && orderId == null) {
                log.debug("Webhook event {} carries no payment/order reference — ignored", eventType);
                return;
            }

            PaymentOrder order = orderId != null
                    ? paymentOrderRepository.findByProviderOrderId(orderId).orElse(null)
                    : paymentOrderRepository.findByProviderPaymentId(paymentId).orElse(null);

            if (order == null) {
                log.warn("Webhook referenced unknown order: providerOrderId={}, paymentId={}",
                        orderId, paymentId);
                return;
            }

            if ("payment.captured".equals(eventType) && "captured".equals(status)) {
                markPaid(order, paymentId, null, null);
            } else if ("payment.failed".equals(eventType)) {
                if (order.getStatus() == OrderStatus.CREATED) {
                    order.setStatus(OrderStatus.FAILED);
                    order.setFailureReason("Payment failed at provider");
                    paymentOrderRepository.save(order);
                    sendNotification(order.getUserId(), "PAYMENT_FAILED",
                            "Payment failed",
                            "Your payment for the " + order.getPlanType() + " plan was not completed.");
                }
            } else {
                log.debug("Ignoring webhook event {} for order {}", eventType, order.getOrderUuid());
            }
        } catch (Exception e) {
            log.error("Failed to process Razorpay webhook: {}", e.getMessage());
            throw new BadRequestException("Malformed webhook payload");
        }
    }

    // ── Internals ────────────────────────────────────────────────────────────

    /**
     * Marks an order PAID exactly once, upgrades the subscription and sends
     * the confirmation notification.
     */
    private void markPaid(PaymentOrder order, String paymentId, String signature, String providerOrderIdOverride) {
        if (order.getStatus() == OrderStatus.PAID) {
            log.info("Order {} already PAID — webhook/verification ignored", order.getOrderUuid());
            return;
        }
        order.setStatus(OrderStatus.PAID);
        order.setProviderPaymentId(paymentId);
        if (signature != null) {
            order.setSignature(signature);
        }
        if (providerOrderIdOverride != null) {
            order.setProviderOrderId(providerOrderIdOverride);
        }
        order.setPaidAt(LocalDateTime.now());
        PaymentOrder saved = paymentOrderRepository.save(order);

        upgradeSubscription(order.getUserId(), order.getPlanType());

        log.info("Payment confirmed & plan upgraded: userId={}, plan={}, orderUuid={}",
                saved.getUserId(), saved.getPlanType(), saved.getOrderUuid());

        sendNotification(saved.getUserId(), "PLAN_UPGRADED",
                "Plan upgraded to " + saved.getPlanType().name(),
                "Your CloudNest " + saved.getPlanType().name()
                        + " plan is active. Your storage quota has been increased.");
    }

    /**
     * Upserts the user's subscription (one row per user) to the new plan.
     * <p>
     * A concurrent verify + webhook can both attempt the first-ever insert;
     * on a unique-key collision the loser re-reads the winner's row and
     * updates it instead of failing with a 500.
     */
    private void upgradeSubscription(Long userId, PlanType planType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expires = now.plusMonths(1);
        try {
            Subscription subscription = subscriptionRepository.findByUserId(userId)
                    .map(existing -> {
                        applyPlan(existing, planType, now, expires);
                        return existing;
                    })
                    .orElseGet(() -> Subscription.builder()
                            .userId(userId)
                            .planType(planType)
                            .status(SubscriptionStatus.ACTIVE)
                            .startsAt(now)
                            .expiresAt(expires)
                            .updatedAt(now)
                            .build());
            subscriptionRepository.save(subscription);
        } catch (org.springframework.dao.DataIntegrityViolationException race) {
            log.debug("Subscription insert raced for userId={} — re-reading existing row", userId);
            subscriptionRepository.findByUserId(userId)
                    .ifPresent(existing -> {
                        applyPlan(existing, planType, now, expires);
                        subscriptionRepository.save(existing);
                    });
        }
    }

    private void applyPlan(Subscription subscription, PlanType planType,
                           LocalDateTime startsAt, LocalDateTime expiresAt) {
        subscription.setPlanType(planType);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartsAt(startsAt);
        subscription.setExpiresAt(expiresAt);
        subscription.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Returns the user's subscription, downgraded to the FREE plan when the
     * paid period has expired or was cancelled.
     */
    private Subscription effectiveSubscription(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> defaultFreeSubscription(userId));
        if (subscription.getPlanType() != PlanType.FREE
                && (subscription.getStatus() != SubscriptionStatus.ACTIVE
                    || (subscription.getExpiresAt() != null
                        && subscription.getExpiresAt().isBefore(LocalDateTime.now())))) {
            Subscription effective = defaultFreeSubscription(userId);
            effective.setStartsAt(subscription.getStartsAt());
            return effective;
        }
        return subscription;
    }

    private PaymentOrder findOwnedOrder(String orderUuid, Long userId) {
        PaymentOrder order = paymentOrderRepository.findByOrderUuid(orderUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderUuid));
        if (!order.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found: " + orderUuid);
        }
        return order;
    }

    private boolean isExpired(PaymentOrder order) {
        if (order.getCreatedAt() == null) {
            return false;
        }
        return order.getCreatedAt().plusMinutes(orderExpiryMinutes).isBefore(LocalDateTime.now());
    }

    private Subscription defaultFreeSubscription(Long userId) {
        return Subscription.builder()
                .userId(userId)
                .planType(PlanType.FREE)
                .status(SubscriptionStatus.ACTIVE)
                .startsAt(LocalDateTime.now())
                .build();
    }

    private long quotaFor(PlanType planType) {
        return planRepository.findByPlanType(planType)
                .map(Plan::getStorageBytes)
                .orElse(5L * 1024 * 1024 * 1024); // fallback: 5 GB
    }

    private void sendNotification(Long userId, String type, String title, String message) {
        try {
            notificationServiceClient.createNotification(NotificationCreateRequest.builder()
                    .userId(userId)
                    .type(type)
                    .title(title)
                    .message(message)
                    .build());
        } catch (Exception e) {
            log.warn("Payment notification not delivered to userId={}: {}", userId, e.getMessage());
        }
    }

    private void requireUserId(Long userId) {
        if (userId == null) {
            throw new BadRequestException("Missing user identity");
        }
    }
}
