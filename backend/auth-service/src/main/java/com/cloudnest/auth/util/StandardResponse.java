package com.cloudnest.auth.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Standard API response wrapper for consistent response formatting.
 * <p>
 * Every successful response from the Auth Service is wrapped in this structure,
 * providing a uniform contract for clients.
 *
 * @param <T> the type of the data payload
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
