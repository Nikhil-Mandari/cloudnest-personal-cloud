package com.cloudnest.share.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * General configuration for the Share Service.
 */
@Slf4j
@Configuration
public class ShareConfig {

    public ShareConfig() {
        log.info("ShareConfig loaded — Share Service configuration initialised");
    }
}
