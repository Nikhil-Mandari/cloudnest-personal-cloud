package com.cloudnest.file.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response body returned for all API errors.
 * <p>
 * Follows the same convention as other CloudNest microservices
 * ({@code success: false}) for consistency across the platform.
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final boolean success;
    private final String message;
    private final int status;
    private final String error;
    private final String path;
    private final Instant timestamp;
    private final List<FieldError> fieldErrors;

    /**
     * Represents a single validation field error.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class FieldError {
        private final String field;
        private final String message;
    }
}
