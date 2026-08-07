package com.cloudnest.user.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;


@Slf4j
@Configuration
public class UserConfig {

    public UserConfig() {
        log.info("UserConfig loaded — User Service configuration initialised");
    }
}
