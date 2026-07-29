package com.cloudnest.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CloudNest User Service — profile and account management hub.
 * <p>
 * Manages user profiles, search, and account lifecycle operations.
 * Configuration is fetched from the central Config Server.
 * Registers with Eureka for service discovery.
 */
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
