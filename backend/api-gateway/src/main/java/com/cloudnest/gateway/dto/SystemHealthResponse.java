package com.cloudnest.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Aggregated platform health (admin view): every discovered service plus
 * healthy/total counts and the snapshot time.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemHealthResponse {

    private List<ServiceHealthResponse> services;

    private int healthyCount;

    private int totalCount;

    private LocalDateTime generatedAt;
}
