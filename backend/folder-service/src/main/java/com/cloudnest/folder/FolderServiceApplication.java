package com.cloudnest.folder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CloudNest Folder Service — hierarchical folder management hub.
 * <p>
 * Manages folder CRUD, unlimited nesting, soft-delete, recursive path updates,
 * and move operations. Configuration is fetched from the central Config Server.
 * Registers with Eureka for service discovery.
 */
@SpringBootApplication
public class FolderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FolderServiceApplication.class, args);
    }
}
