package com.cloudnest.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * Programmatic route definitions for the API Gateway.
 * <p>
 * Every external request hits the Gateway first and is forwarded to the
 * appropriate microservice via Eureka service discovery ({@code lb://}).
 * <p>
 * Routes can also be overridden at runtime via centralized configuration
 * in Config Server (see {@code backend/config-repo/gateway.yml}).
 */
@Configuration
public class RouteConfig {

    @Value("${app.gateway.auth-service:auth-service}")
    private String authService;

    @Value("${app.gateway.user-service:user-service}")
    private String userService;

    @Value("${app.gateway.file-service:file-service}")
    private String fileService;

    @Value("${app.gateway.folder-service:folder-service}")
    private String folderService;

    @Value("${app.gateway.share-service:share-service}")
    private String shareService;

    @Value("${app.gateway.notification-service:notification-service}")
    private String notificationService;

    /**
     * Registers all downstream service routes.
     * Each route strips the {@code /api/<service>} prefix before forwarding.
     */
    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service",   r -> apiRoute(r, "/api/auth/**",         authService))
                .route("user-service",   r -> apiRoute(r, "/api/users/**",        userService))
                .route("file-service",   r -> apiRoute(r, "/api/files/**",        fileService))
                .route("folder-service", r -> apiRoute(r, "/api/folders/**",      folderService))
                .route("share-service",  r -> apiRoute(r, "/api/share/**",        shareService))
                .route("notification-service", r -> apiRoute(r, "/api/notifications/**", notificationService))
                .build();
    }

    // -- Helper: create a single route with path predicate and lb:// URI -----

    /**
     * Builds a route whose predicate matches the given {@code pathPattern}
     * and forwards to the {@code lb://<serviceName>} load-balanced URI.
     * <p>
     * The {@code stripPrefix=1} ensures the leading path segment (e.g.
     * {@code /api}) is removed before reaching the downstream service.
     *
     * @param spec        the predicate spec (route builder entry point)
     * @param pathPattern the request path pattern to match, e.g. {@code /api/auth/**}
     * @param serviceName the Eureka service ID, e.g. {@code auth-service}
     * @return a {@code Buildable<Route>} to be consumed by the fluent builder
     */
    private Buildable<Route> apiRoute(PredicateSpec spec, String pathPattern, String serviceName) {
        return spec
                .path(pathPattern)
                .filters(f -> f.stripPrefix(1))
                .uri("lb://" + serviceName);
    }
}
