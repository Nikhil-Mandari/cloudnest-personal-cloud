package com.cloudnest.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CloudNest Auth Service — authentication and authorization hub.
 * <p>
 * Manages user registration, login, and JWT token generation/validation.
 * Configuration is fetched from the central Config Server.
 * Registers with Eureka for service discovery.
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
