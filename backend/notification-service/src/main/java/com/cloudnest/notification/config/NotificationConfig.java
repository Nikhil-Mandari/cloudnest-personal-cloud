package com.cloudnest.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * General configuration for the Notification Service.
 */
@Slf4j
@Configuration
public class NotificationConfig {

    public NotificationConfig() {
        log.info("NotificationConfig loaded — Notification Service configuration initialised");
    }
}
