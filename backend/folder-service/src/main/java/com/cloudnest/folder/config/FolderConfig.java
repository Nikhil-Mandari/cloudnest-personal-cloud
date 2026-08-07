package com.cloudnest.folder.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * General configuration for the Folder Service.
 */
@Slf4j
@Configuration
public class FolderConfig {

    public FolderConfig() {
        log.info("FolderConfig loaded — Folder Service configuration initialised");
    }
}
