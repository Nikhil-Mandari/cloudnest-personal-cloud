package com.cloudnest.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fallback controller invoked when a downstream service is unreachable
 * or the circuit breaker opens (Resilience4j).
 * <p>
 * Each route in {@code gateway.yml} can reference these fallback URIs
 * to provide graceful degradation instead of hard errors.
 * <p>
 * Example configuration for a route:
 * <pre>{@code
 * filters:
 *   - name: CircuitBreaker
 *     args:
 *       name: fileServiceCircuitBreaker
 *       fallbackUri: forward:/fallback/file-service
 * }</pre>
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * Generic fallback – called when no service-specific endpoint matches.
     */
    @RequestMapping("/default")
    public Mono<ResponseEntity<Map<String, Object>>> defaultFallback() {
        return Mono.just(buildResponse("Service is temporarily unavailable. Please try again later."));
    }

    @RequestMapping("/auth-service")
    public Mono<ResponseEntity<Map<String, Object>>> authServiceFallback() {
        log.warn("Auth Service is unavailable – circuit breaker open");
        return Mono.just(buildResponse("Authentication service is temporarily unavailable."));
    }

    @RequestMapping("/user-service")
    public Mono<ResponseEntity<Map<String, Object>>> userServiceFallback() {
        log.warn("User Service is unavailable – circuit breaker open");
        return Mono.just(buildResponse("User service is temporarily unavailable."));
    }

    @RequestMapping("/file-service")
    public Mono<ResponseEntity<Map<String, Object>>> fileServiceFallback() {
        log.warn("File Service is unavailable – circuit breaker open");
        return Mono.just(buildResponse("File service is temporarily unavailable."));
    }

    @RequestMapping("/folder-service")
    public Mono<ResponseEntity<Map<String, Object>>> folderServiceFallback() {
        log.warn("Folder Service is unavailable – circuit breaker open");
        return Mono.just(buildResponse("Folder service is temporarily unavailable."));
    }

    @RequestMapping("/share-service")
    public Mono<ResponseEntity<Map<String, Object>>> shareServiceFallback() {
        log.warn("Share Service is unavailable – circuit breaker open");
        return Mono.just(buildResponse("Share service is temporarily unavailable."));
    }

    @RequestMapping("/notification-service")
    public Mono<ResponseEntity<Map<String, Object>>> notificationServiceFallback() {
        log.warn("Notification Service is unavailable – circuit breaker open");
        return Mono.just(buildResponse("Notification service is temporarily unavailable."));
    }

    // -- Private helpers -----------------------------------------------------

    /**
     * Builds a consistent JSON error response for fallback scenarios.
     */
    private ResponseEntity<Map<String, Object>> buildResponse(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "Service Unavailable");
        body.put("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
