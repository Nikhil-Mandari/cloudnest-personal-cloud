package com.cloudnest.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CloudNest Notification Service — in-app notification hub.
 * <p>
 * Manages notifications for share events, system alerts, and user activity
 * tracking. Configuration is fetched from the central Config Server.
 * Registers with Eureka for service discovery.
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
