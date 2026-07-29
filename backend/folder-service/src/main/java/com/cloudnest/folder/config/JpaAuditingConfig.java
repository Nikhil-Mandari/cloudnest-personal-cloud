package com.cloudnest.folder.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing for automatic timestamp management.
 * <p>
 * Activates support for {@code @CreatedDate} and {@code @LastModifiedDate}
 * annotations on entity fields.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    public JpaAuditingConfig() {
        // Configuration is handled via annotations
    }
}
