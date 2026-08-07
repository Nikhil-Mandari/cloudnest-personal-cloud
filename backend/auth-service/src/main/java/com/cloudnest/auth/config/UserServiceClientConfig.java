package com.cloudnest.auth.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign configuration for the User Service client: presents the service
 * identity so downstream admin guards accept the call. These headers are only
 * meaningful on the internal service network — external callers reach the
 * User Service through the API Gateway, which overwrites identity headers
 * from the validated JWT.
 */
@Configuration
public class UserServiceClientConfig {

    @Bean
    public RequestInterceptor userServiceAdminInterceptor() {
        return template -> {
            template.header("X-User-Role", "ROLE_ADMIN");
            template.header("X-User-Id", "0");
        };
    }
}
