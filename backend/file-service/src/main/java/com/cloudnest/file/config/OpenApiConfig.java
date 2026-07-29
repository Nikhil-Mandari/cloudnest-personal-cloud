package com.cloudnest.file.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CloudNest File Service API",
                version = "1.0.0",
                description = "REST API for managing file metadata in the CloudNest Personal Cloud platform. " +
                              "Provides endpoints for metadata CRUD, soft-delete, restore, search, " +
                              "and folder movement for file records. Binary storage is handled separately.",
                contact = @Contact(
                        name = "CloudNest Team",
                        email = "support@cloudnest.com"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(
                        url = "http://localhost:8083",
                        description = "Local development server"
                ),
                @Server(
                        url = "http://api-gateway:8080",
                        description = "API Gateway (production)"
                )
        }
)
public class OpenApiConfig {

    public OpenApiConfig() {
        // Configuration is handled via annotations
    }
}
