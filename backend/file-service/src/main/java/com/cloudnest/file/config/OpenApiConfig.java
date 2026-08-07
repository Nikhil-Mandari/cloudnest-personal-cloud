package com.cloudnest.file.config;

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
 * OpenAPI / Swagger documentation configuration for the File Service.
 * <p>
 * Exposes the CloudNest API documentation (Swagger UI at
 * {@code /swagger-ui/index.html}, OpenAPI JSON at {@code /v3/api-docs}) with a
 * JWT Bearer security scheme — the Swagger UI "Authorize" button lets callers
 * paste the token returned by {@code POST /api/auth/login}.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CloudNest Personal Cloud API",
                version = "v1.0",
                description = "REST APIs for CloudNest Personal Cloud. " +
                              "File binary content is stored in MinIO object storage while " +
                              "metadata (object key, bucket, content type, size, SHA-256 checksum) " +
                              "is persisted in MySQL.",
                contact = @Contact(
                        name = "Nikhil Mandari"
                ),
                license = @License(
                        name = "MIT",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(
                        url = "http://localhost:8083",
                        description = "Local development server"
                ),
                @Server(
                        url = "http://localhost:8080",
                        description = "API Gateway (single entry point)"
                ),
                @Server(
                        url = "http://api-gateway:8080",
                        description = "API Gateway (production / Docker)"
                )
        },
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT Bearer token obtained from POST /api/auth/login. " +
                      "Paste the token into the Authorize dialog; all business endpoints require it."
)
public class OpenApiConfig {

    public OpenApiConfig() {
        // Configuration is handled via annotations
    }
}
