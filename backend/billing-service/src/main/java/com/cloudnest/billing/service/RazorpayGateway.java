package com.cloudnest.billing.service;

import com.cloudnest.billing.config.RazorpayProperties;
import com.cloudnest.billing.entity.PlanType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal Razorpay API gateway.
 * <p>
 * Uses plain {@link HttpClient} (no extra dependency). Responsible for:
 * <ul>
 *   <li>Creating orders ({@code POST /v1/orders}) with the amount in paise;</li>
 *   <li>Verifying payment signatures (HMAC-SHA256 over
 *       {@code orderId|paymentId} with the key secret);</li>
 *   <li>Verifying webhook events with the webhook secret.</li>
 * </ul>
 * Only the client-safe order id is ever returned to the frontend.
 */
@Slf4j
@Component
public class RazorpayGateway {

    private static final String API_BASE = "https://api.razorpay.com/v1";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final RazorpayProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    public RazorpayGateway(RazorpayProperties properties) {
        this.properties = properties;
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    /** Client-safe public key id (safe to send to the frontend). */
    public String getKeyId() {
        return properties.getKeyId();
    }

    /**
     * Creates a Razorpay order for the given plan.
     *
     * @return the Razorpay order id
     */
    public String createOrder(PlanType planType, long amountInr, String receipt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amountInr * 100); // paise
        body.put("currency", "INR");
        body.put("receipt", receipt);
        Map<String, String> notes = new LinkedHashMap<>();
        notes.put("plan", planType.name());
        body.put("notes", notes);

        try {
            String response = post("/orders", body);
            JsonNode node = objectMapper.readTree(response);
            String orderId = node.path("id").asText(null);
            if (orderId == null || orderId.isBlank()) {
                log.error("Razorpay order creation returned no id: {}", response);
                throw new IllegalStateException("Payment provider returned an invalid order");
            }
            log.info("Razorpay order created: providerOrderId={}, plan={}", orderId, planType);
            return orderId;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create Razorpay order for plan {}: {}", planType, e.getMessage());
            throw new IllegalStateException("Payment provider is temporarily unavailable", e);
        }
    }

    /**
     * Verifies a payment signature (HMAC-SHA256 of {@code orderId|paymentId}).
     */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        String payload = orderId + "|" + paymentId;
        return hmacSha256(payload, properties.getKeySecret()).equals(signature);
    }

    /**
     * Verifies a webhook signature: {@code HMAC-SHA256(body, webhookSecret)}
     * encoded as hex.
     */
    public boolean verifyWebhookSignature(String body, String signature) {
        if (properties.getWebhookSecret() == null || properties.getWebhookSecret().isBlank()) {
            log.warn("Razorpay webhook secret not configured — webhook signature cannot be verified");
            return false;
        }
        return hmacSha256Hex(body, properties.getWebhookSecret()).equalsIgnoreCase(signature);
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private String post(String path, Map<String, Object> body) throws Exception {
        String json = objectMapper.writeValueAsString(body);
        String basic = "Basic " + Base64.getEncoder().encodeToString(
                (properties.getKeyId() + ":" + properties.getKeySecret()).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + path))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", basic)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            log.error("Razorpay API error {} on {}: {}", response.statusCode(), path, response.body());
            throw new IllegalStateException("Payment provider returned status " + response.statusCode());
        }
        return response.body();
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute payment signature", e);
        }
    }

    private String hmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute webhook signature", e);
        }
    }

}
