package com.cloudnest.billing.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Standard API response wrapper for consistent response formatting.
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"success", "message", "data", "path", "timestamp"})
public class StandardResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final String path;

    @Builder.Default
    private final Instant timestamp = Instant.now();
}
