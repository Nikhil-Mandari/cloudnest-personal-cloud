package com.cloudnest.share.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security beans for the Share Service.
 * <p>
 * Only the crypto primitives are provided (password hashing for protected share
 * links) — no filter chain is installed so existing routing is untouched.
 */
@Configuration
public class ShareSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
