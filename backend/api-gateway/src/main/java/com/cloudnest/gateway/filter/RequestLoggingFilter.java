package com.cloudnest.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global filter that logs every request routed through the Gateway.
 * <p>
 * Captures:
 * <ul>
 *   <li>A unique request ID (traceable across logs)</li>
 *   <li>HTTP method and path</li>
 *   <li>Downstream service (derived from the route ID)</li>
 *   <li>Response HTTP status code</li>
 *   <li>Execution time in milliseconds</li>
 * </ul>
 */
@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String START_TIME_ATTR = "requestStartTime";

    /**
     * Filters every request, logs it, and logs the response after completion.
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        // Attach a unique request ID to the exchange for traceability
        exchange.getAttributes().put(REQUEST_ID_HEADER, requestId);
        exchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());

        // Add the request ID as a response header
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);

        // Determine the target route ID (set by RouteToRequestUrlFilter)
        Route matchedRoute = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = matchedRoute != null ? matchedRoute.getId() : "unknown";

        log.info("[{}] → {} {} (route: {})",
                requestId, request.getMethod(), request.getURI().getPath(), routeId);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long startTime = (long) exchange.getAttributes().getOrDefault(START_TIME_ATTR, 0L);
            long duration = System.currentTimeMillis() - startTime;

            ServerHttpResponse response = exchange.getResponse();
            int status = response.getRawStatusCode() != null ? response.getRawStatusCode() : 0;

            log.info("[{}] ← {} {} ({}ms)",
                    requestId, status, request.getURI().getPath(), duration);
        }));
    }

    /**
     * Highest precedence — this filter runs first.
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
