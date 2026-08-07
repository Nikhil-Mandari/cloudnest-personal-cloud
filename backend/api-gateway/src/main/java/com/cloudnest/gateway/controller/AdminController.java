package com.cloudnest.gateway.controller;

import com.cloudnest.gateway.dto.ServiceHealthResponse;
import com.cloudnest.gateway.dto.SystemHealthResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin-only platform views served by the gateway itself.
 * <p>
 * {@code /api/admin/**} is not routed to any downstream service (see
 * {@code RouteConfig}), so these handlers run locally. The global
 * {@code AuthenticationFilter} still authenticates the request and sets the
 * {@code X-User-Role} header (replacing any caller-supplied value), which is
 * checked here before any data is returned.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/system")
public class AdminController {

    private static final String ROLE_HEADER = "X-User-Role";
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(3);

    private final DiscoveryClient discoveryClient;
    private final WebClient.Builder webClientBuilder;

    public AdminController(DiscoveryClient discoveryClient, WebClient.Builder webClientBuilder) {
        this.discoveryClient = discoveryClient;
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * Probes every service registered with Eureka and reports per-service
     * actuator health. Requires the ROLE_ADMIN role.
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> systemHealth(
            @RequestHeader(value = ROLE_HEADER, required = false) String roleHeader) {

        if (!"ROLE_ADMIN".equalsIgnoreCase(roleHeader)) {
            log.warn("System health denied — caller is not an admin");
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(buildBody(false, "Admin access required", null)));
        }

        log.debug("Admin requested system health ({} services discovered)",
                discoveryClient.getServices().size());

        return collectHealth().map(health -> ResponseEntity.ok()
                .body(buildBody(true, "System health retrieved", health)));
    }

    // ── Internals ───────────────────────────────────────────────────────────

    private Mono<SystemHealthResponse> collectHealth() {
        List<String> services = discoveryClient.getServices().stream().sorted().toList();

        return Flux.fromIterable(services)
                .flatMap(this::probeService, 16)
                .collectList()
                .map(list -> {
                    int healthy = (int) list.stream()
                            .filter(service -> "UP".equalsIgnoreCase(service.getStatus()))
                            .count();
                    return SystemHealthResponse.builder()
                            .services(list)
                            .healthyCount(healthy)
                            .totalCount(list.size())
                            .generatedAt(LocalDateTime.now())
                            .build();
                });
    }

    private Mono<ServiceHealthResponse> probeService(String name) {
        List<ServiceInstance> instances = discoveryClient.getInstances(name);
        List<String> uris = instances.stream()
                .map(instance -> instance.getUri().toString())
                .toList();

        if (instances.isEmpty()) {
            return Mono.just(ServiceHealthResponse.builder()
                    .name(name)
                    .status("DOWN")
                    .instanceCount(0)
                    .instances(List.of())
                    .build());
        }

        String endpoint = instances.get(0).getUri().toString() + "/actuator/health";

        return webClientBuilder.build()
                .get()
                .uri(endpoint)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(node -> ServiceHealthResponse.builder()
                        .name(name)
                        .status(node.path("status").asText("UP"))
                        .instanceCount(instances.size())
                        .instances(uris)
                        .endpoint(endpoint)
                        .build())
                .timeout(PROBE_TIMEOUT)
                .onErrorResume(error -> {
                    log.warn("Health probe failed for {} ({}): {}", name, endpoint, error.getMessage());
                    return Mono.just(ServiceHealthResponse.builder()
                            .name(name)
                            .status("DOWN")
                            .instanceCount(instances.size())
                            .instances(uris)
                            .endpoint(endpoint)
                            .build());
                });
    }

    private Map<String, Object> buildBody(boolean success, String message, SystemHealthResponse data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", success);
        body.put("message", message);
        if (data != null) {
            body.put("data", data);
        }
        body.put("path", "/api/admin/system/health");
        return body;
    }
}
