package com.cloudnest.file.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * General configuration for the File Service.
 */
@Slf4j
@Configuration
public class FileConfig {

    public FileConfig() {
        log.info("FileConfig loaded — File Service configuration initialised");
    }
}
