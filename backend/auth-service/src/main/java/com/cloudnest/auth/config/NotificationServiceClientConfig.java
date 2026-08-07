package com.cloudnest.auth.config;

import feign.Request;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign configuration for the Notification Service client: presents the
 * service identity headers (mirroring the User Service client pattern) and
 * applies short timeouts so an unresponsive notification-service can never
 * stall the authentication flow (login, password change, etc.).
 */
@Configuration
public class NotificationServiceClientConfig {

    @Bean
    public RequestInterceptor notificationServiceInterceptor() {
        return template -> {
            template.header("X-User-Role", "ROLE_ADMIN");
            template.header("X-User-Id", "0");
        };
    }

    @Bean
    public Request.Options notificationServiceTimeouts() {
        return new Request.Options(1500, 3000);
    }
}
