package com.cloudnest.share;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * CloudNest Share Service — share management hub.
 * <p>
 * Manages file and folder sharing between users, public share tokens,
 * permission management, and share revocation. Configuration is fetched
 * from the central Config Server. Registers with Eureka for service
 * discovery. Uses OpenFeign to communicate with User, File, and Folder
 * services.
 */
@SpringBootApplication
@EnableFeignClients
public class ShareServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShareServiceApplication.class, args);
    }
}
