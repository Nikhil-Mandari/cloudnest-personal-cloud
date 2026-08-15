package com.cloudnest.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CloudNest Auth Service — authentication and authorization hub.
 * <p>
 * Manages user registration, login, and JWT token generation/validation.
 * Configuration is fetched from the central Config Server.
 * Registers with Eureka for service discovery. Uses OpenFeign to provision
 * user profiles in the User Service after successful registration.
 */
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
