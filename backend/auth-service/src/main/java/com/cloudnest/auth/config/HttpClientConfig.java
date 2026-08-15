package com.cloudnest.auth.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * HTTP client configuration for the Auth Service.
 * <p>
 * Provides a {@link RestTemplate} used by the OAuth flow to exchange
 * authorization codes and fetch provider user info. Timeouts keep the
 * callback fast when a provider is slow or unreachable.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }
}
