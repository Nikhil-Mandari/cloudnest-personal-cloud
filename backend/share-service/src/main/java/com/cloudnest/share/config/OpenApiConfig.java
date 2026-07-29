package com.cloudnest.share.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CloudNest Share Service API",
                version = "1.0.0",
                description = "REST API for managing file and folder sharing in the CloudNest Personal Cloud platform. " +
                              "Provides endpoints for sharing resources with other users, managing public share tokens, " +
                              "updating permissions, revoking shares, and retrieving shared resources.",
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
                        url = "http://localhost:8086",
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
