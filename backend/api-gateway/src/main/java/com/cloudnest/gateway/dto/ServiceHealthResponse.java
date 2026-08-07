package com.cloudnest.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Health snapshot of a single discovered service (admin view).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceHealthResponse {

    /** Registered Eureka service name (e.g. AUTH-SERVICE). */
    private String name;

    /** UP / DOWN / UNKNOWN — from the service's actuator health endpoint. */
    private String status;

    /** Number of registered instances. */
    private int instanceCount;

    /** Instance URIs (scheme://host:port). */
    private List<String> instances;

    /** The actuator health endpoint that was probed. */
    private String endpoint;
}
