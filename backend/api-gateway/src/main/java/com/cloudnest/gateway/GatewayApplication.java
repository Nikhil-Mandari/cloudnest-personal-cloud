package com.cloudnest.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CloudNest API Gateway — single entry point for all client requests.
 * <p>
 * Uses Spring Cloud Gateway (reactive / WebFlux) to route requests to
 * downstream microservices via Eureka service discovery ({@code lb://}).
 * Configuration is fetched from the central Config Server.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
