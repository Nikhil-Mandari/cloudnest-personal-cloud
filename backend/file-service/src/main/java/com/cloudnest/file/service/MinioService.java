package com.cloudnest.file.service;

import com.cloudnest.file.dto.MinioStatusResponse;

import java.io.InputStream;

/**
 * Service contract for MinIO object storage operations.
 * <p>
 * All binary file content lives in MinIO while metadata lives in MySQL.
 * Implementations must never log credentials or secrets.
 */
public interface MinioService {

    /**
     * Ensures the configured bucket exists, creating it if missing.
     * <p>
     * Idempotent — safe to call on every upload (self-healing) and on startup.
     *
     * @throws com.cloudnest.file.exception.BucketCreationException if MinIO is
     *         unreachable or the bucket cannot be created
     */
    void ensureBucket();

    /**
     * Uploads an object to the configured bucket.
     *
     * @param objectName  the unique object key
     * @param content     the binary content stream
     * @param size        the exact content length in bytes
     * @param contentType the MIME type of the content
     * @return the object key that was uploaded
     * @throws com.cloudnest.file.exception.MinioException if the upload fails
     */
    String uploadObject(String objectName, InputStream content, long size, String contentType);

    /**
     * Reads an object from the configured bucket.
     *
     * @param objectName the object key
     * @return a stream of the object's content (caller must close it)
     * @throws com.cloudnest.file.exception.MinioException if the read fails
     */
    InputStream getObject(String objectName);

    /**
     * Deletes an object from the configured bucket.
     * <p>
     * Deleting a non-existent object succeeds silently (idempotent).
     *
     * @param objectName the object key
     * @throws com.cloudnest.file.exception.MinioException if the deletion fails
     */
    void deleteObject(String objectName);

    /**
     * Checks whether an object exists in the configured bucket.
     *
     * @param objectName the object key
     * @return {@code true} if the object exists
     * @throws com.cloudnest.file.exception.MinioException if the check cannot be performed
     */
    boolean objectExists(String objectName);

    /**
     * Probes MinIO connectivity and bucket existence for the admin dashboard.
     * Never throws — connectivity problems are reported in the response so the
     * dashboard can surface them.
     *
     * @return the current status
     */
    MinioStatusResponse status();
}
