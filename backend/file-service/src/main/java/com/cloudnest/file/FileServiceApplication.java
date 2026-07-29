package com.cloudnest.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CloudNest File Service — file metadata management hub.
 * <p>
 * Manages file metadata records including CRUD, soft-delete, restore,
 * search, and folder movement. Configuration is fetched from the central
 * Config Server. Registers with Eureka for service discovery.
 * <p>
 * <strong>Note:</strong> Actual binary file storage is not handled by this
 * service yet. Only metadata is persisted.
 */
@SpringBootApplication
public class FileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileServiceApplication.class, args);
    }
}
