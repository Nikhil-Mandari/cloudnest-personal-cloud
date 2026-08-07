package com.cloudnest.file.service.impl;

import com.cloudnest.file.config.MinioProperties;
import com.cloudnest.file.dto.MinioStatusResponse;
import com.cloudnest.file.exception.BucketCreationException;
import com.cloudnest.file.exception.MinioException;
import com.cloudnest.file.exception.ResourceNotFoundException;
import com.cloudnest.file.service.MinioService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Implementation of {@link MinioService} backed by the MinIO Java SDK.
 * <p>
 * On application startup the configured bucket is verified and created if
 * missing. Storage failures are wrapped in {@link MinioException} so callers
 * and the global exception handler can respond consistently.
 */
@Slf4j
@Service
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public MinioServiceImpl(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    /**
     * Runs on application startup: connects to MinIO and ensures the bucket
     * exists (creating it if missing).
     * <p>
     * Failures are logged but do <em>not</em> stop the application — the bucket
     * is ensured again on demand before each upload (self-healing), so the
     * service can start even if MinIO is temporarily unavailable.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            ensureBucket();
            log.info("MinIO ready — bucket '{}' is available", minioProperties.getBucketName());
        } catch (Exception e) {
            log.error("MinIO not reachable at startup — bucket '{}' will be ensured on demand: {}",
                    minioProperties.getBucketName(), e.getMessage());
        }
    }

    @Override
    public void ensureBucket() {
        String bucket = minioProperties.getBucketName();
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());

            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket '{}' created successfully", bucket);
            } else {
                log.debug("MinIO bucket '{}' already exists", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to ensure MinIO bucket '{}' exists: {}", bucket, e.getMessage());
            throw new BucketCreationException(
                    "Failed to connect to MinIO or create bucket '" + bucket + "'", e);
        }
    }

    @Override
    public String uploadObject(String objectName, InputStream content, long size, String contentType) {
        // Self-healing: ensures the bucket exists even if it was removed after startup
        ensureBucket();

        String bucket = minioProperties.getBucketName();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(content, size, -1)
                            .contentType(contentType)
                            .build());
            log.info("Uploaded object '{}' to bucket '{}' ({} bytes, {})",
                    objectName, bucket, size, contentType);
            return objectName;
        } catch (Exception e) {
            log.error("Failed to upload object '{}' to bucket '{}': {}", objectName, bucket, e.getMessage());
            throw new MinioException("Failed to upload object '" + objectName + "' to MinIO", e);
        }
    }

    @Override
    public InputStream getObject(String objectName) {
        String bucket = minioProperties.getBucketName();
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
        } catch (ErrorResponseException e) {
            if (e.response() != null && e.response().code() == 404) {
                log.warn("Object '{}' not found in bucket '{}'", objectName, bucket);
                throw new ResourceNotFoundException(
                        "File content not found in storage: " + objectName);
            }
            log.error("Failed to read object '{}' from bucket '{}': {}", objectName, bucket, e.getMessage());
            throw new MinioException("Failed to read object '" + objectName + "' from MinIO", e);
        } catch (Exception e) {
            log.error("Failed to read object '{}' from bucket '{}': {}", objectName, bucket, e.getMessage());
            throw new MinioException("Failed to read object '" + objectName + "' from MinIO", e);
        }
    }

    @Override
    public void deleteObject(String objectName) {
        String bucket = minioProperties.getBucketName();
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
            log.info("Deleted object '{}' from bucket '{}'", objectName, bucket);
        } catch (Exception e) {
            log.error("Failed to delete object '{}' from bucket '{}': {}", objectName, bucket, e.getMessage());
            throw new MinioException("Failed to delete object '" + objectName + "' from MinIO", e);
        }
    }

    @Override
    public boolean objectExists(String objectName) {
        String bucket = minioProperties.getBucketName();
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
            return true;
        } catch (ErrorResponseException e) {
            if (e.response() != null && e.response().code() == 404) {
                return false;
            }
            log.error("Failed to stat object '{}' in bucket '{}': {}", objectName, bucket, e.getMessage());
            throw new MinioException("Failed to check object '" + objectName + "' in MinIO", e);
        } catch (Exception e) {
            log.error("Failed to stat object '{}' in bucket '{}': {}", objectName, bucket, e.getMessage());
            throw new MinioException("Failed to check object '" + objectName + "' in MinIO", e);
        }
    }

    @Override
    public MinioStatusResponse status() {
        String bucket = minioProperties.getBucketName();
        boolean reachable;
        boolean bucketExists = false;

        try {
            bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            reachable = true;
        } catch (Exception e) {
            log.warn("MinIO status probe failed: {}", e.getMessage());
            reachable = false;
        }

        String status;
        if (!reachable) {
            status = "Unreachable";
        } else if (bucketExists) {
            status = "Connected";
        } else {
            status = "Bucket missing";
        }

        return MinioStatusResponse.builder()
                .endpoint(minioProperties.getEndpoint())
                .bucket(bucket)
                .reachable(reachable)
                .bucketExists(bucketExists)
                .status(status)
                .build();
    }
}
