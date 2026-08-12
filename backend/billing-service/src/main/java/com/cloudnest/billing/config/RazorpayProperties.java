package com.cloudnest.billing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Razorpay payment-provider configuration.
 * <p>
 * All values come from environment variables and are never committed.
 * When {@code keyId}/{@code keySecret} are blank the provider is disabled
 * and order creation returns 503 instead of faking a payment.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "razorpay")
public class RazorpayProperties {

    private String keyId = "";
    private String keySecret = "";
    private String webhookSecret = "";
    private boolean enabled = false;

    public boolean isConfigured() {
        return enabled && keyId != null && !keyId.isBlank()
                && keySecret != null && !keySecret.isBlank();
    }
}
