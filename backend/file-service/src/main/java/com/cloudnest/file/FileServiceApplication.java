package com.cloudnest.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * CloudNest File Service — file metadata management hub.
 * <p>
 * Manages file metadata records including CRUD, hard-delete, restore, search,
 * and folder movement. Binary file content is stored in MinIO object storage,
 * while metadata (including object key, bucket, content type, size, and
 * SHA-256 checksum) is persisted in MySQL. Configuration is fetched from the
 * central Config Server. Registers with Eureka for service discovery.
 */
@SpringBootApplication
@EnableFeignClients
public class FileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileServiceApplication.class, args);
    }
}
