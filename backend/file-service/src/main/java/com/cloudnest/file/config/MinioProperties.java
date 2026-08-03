package com.cloudnest.file.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for MinIO object storage.
 * <p>
 * Bound from the {@code minio.*} prefix in application.yml / config-repo.
 * Credentials are never hardcoded — they are supplied through environment
 * variables or the central Config Server.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * MinIO server endpoint (e.g. {@code http://localhost:9000} locally,
     * {@code http://minio:9000} inside Docker).
     */
    private String endpoint;

    /**
     * MinIO access key.
     */
    private String accessKey;

    /**
     * MinIO secret key.
     */
    private String secretKey;

    /**
     * Name of the bucket used for CloudNest files.
     */
    private String bucketName;

    /**
     * Whether the connection uses TLS/HTTPS.
     */
    private boolean secure = false;

    /**
     * Maximum allowed file size in bytes for uploads (default 100 MB).
     */
    private long maxFileSize = 104_857_600L;
}
