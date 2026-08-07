package com.cloudnest.auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger documentation configuration for the Auth Service.
 * <p>
 * Swagger UI at {@code /swagger-ui/index.html}, OpenAPI JSON at
 * {@code /v3/api-docs}. Public OTP/registration endpoints are marked
 * anonymous; protected endpoints require the JWT Bearer token from
 * {@code POST /api/auth/login}.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CloudNest Auth Service API",
                version = "v1.1",
                description = "Enterprise authentication APIs: email-OTP registration, OTP login, "
                        + "forgot-password, JWT refresh/rotation, sessions, trusted devices, "
                        + "login history and security logs.",
                contact = @Contact(name = "Nikhil Mandari"),
                license = @License(name = "MIT", url = "https://opensource.org/licenses/MIT")
        ),
        servers = {
                @Server(url = "http://localhost:8081", description = "Local development server"),
                @Server(url = "http://localhost:8080", description = "API Gateway (single entry point)"),
                @Server(url = "http://api-gateway:8080", description = "API Gateway (production / Docker)")
        },
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Access token obtained from POST /api/auth/login (or /login/verify). "
                + "Public endpoints (register, login, otp, forgot-password, refresh) do not require it."
)
public class OpenApiConfig {

    public OpenApiConfig() {
        // Configuration is handled via annotations
    }
}
