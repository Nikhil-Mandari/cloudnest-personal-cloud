package com.cloudnest.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Core application configuration for the Auth Service.
 * <p>
 * Serves as a home for service-level bean definitions and
 * configuration that does not fit into more specialised
 * configuration classes (e.g. {@code SecurityConfig}).
 */
@Slf4j
@Configuration
public class AuthConfig {

    public AuthConfig() {
        log.info("AuthConfig loaded — Auth Service configuration initialised");
    }
}
