package com.cloudnest.file.config;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the MinIO object storage client.
 * <p>
 * Builds a {@link MinioClient} from {@link MinioProperties}. No credentials are
 * hardcoded — everything comes from configuration (application.yml, Config
 * Server, or environment variables).
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    /**
     * Creates the {@link MinioClient} bean used by {@code MinioService}.
     * <p>
     * Note: client construction does <em>not</em> perform any network I/O —
     * connectivity is verified lazily on startup (bucket initialisation) and
     * on each storage operation.
     *
     * @param properties the MinIO configuration properties
     * @return a configured MinioClient
     */
    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        log.info("Configuring MinIO client: endpoint={}, bucket='{}', secure={}",
                properties.getEndpoint(), properties.getBucketName(), properties.isSecure());

        // minio-java 8.5+ derives TLS from the endpoint URL scheme (https://),
        // so the deprecated `.secure(...)` builder method is no longer used.
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
}
