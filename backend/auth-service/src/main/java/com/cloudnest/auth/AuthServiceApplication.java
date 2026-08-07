package com.cloudnest.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * CloudNest Auth Service — authentication and authorization hub.
 * <p>
 * Manages user registration, login, OTP verification, refresh tokens,
 * device/session management, and JWT token generation/validation.
 * Configuration is fetched from the central Config Server.
 * Registers with Eureka for service discovery.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableFeignClients
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
