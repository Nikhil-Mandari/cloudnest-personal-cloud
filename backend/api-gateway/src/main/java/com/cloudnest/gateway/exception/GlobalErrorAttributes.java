package com.cloudnest.gateway.exception;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom error attributes that produce a consistent JSON error response
 * matching the CloudNest API contract format.
 * <p>
 * Overrides the default Spring Boot WebFlux error response with a structured
 * {@code { "success": false, "message": "...", "timestamp": "...", "status": ... }} body.
 */
@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Throwable error = getError(request);
        HttpStatus status = determineHttpStatus(error);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", error.getMessage() != null ? error.getMessage() : "Internal server error");
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("path", request.path());
        body.put("timestamp", Instant.now().toString());

        return body;
    }

    /**
     * Maps common exception types to HTTP status codes.
     */
    private HttpStatus determineHttpStatus(Throwable error) {
        if (error instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (error instanceof java.util.NoSuchElementException) {
            return HttpStatus.NOT_FOUND;
        }
        // Gateway-specific: circuit breaker / timeout fallback
        if (error instanceof java.util.concurrent.TimeoutException) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        // Default to 500 for unhandled exceptions
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
