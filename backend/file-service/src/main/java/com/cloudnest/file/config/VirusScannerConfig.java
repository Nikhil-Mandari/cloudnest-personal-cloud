package com.cloudnest.file.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link VirusScannerProperties} so the {@code virus-scanner.*}
 * configuration block is bound from the Config Server.
 */
@Configuration
@EnableConfigurationProperties(VirusScannerProperties.class)
public class VirusScannerConfig {
}
