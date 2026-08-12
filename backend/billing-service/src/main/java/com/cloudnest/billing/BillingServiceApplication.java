package com.cloudnest.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * CloudNest Billing Service.
 * <p>
 * Owns storage plans, per-user subscriptions, payment orders, Razorpay
 * order creation / webhook verification, and payment notifications.
 * Configuration is fetched from the central Config Server. Registers with
 * Eureka for service discovery and talks to the Notification Service via
 * OpenFeign.
 */
@SpringBootApplication
@EnableFeignClients
public class BillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }
}
