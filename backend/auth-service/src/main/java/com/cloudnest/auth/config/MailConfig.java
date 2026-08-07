package com.cloudnest.auth.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * SMTP sender wiring for the Auth Service.
 * <p>
 * The {@link JavaMailSender} bean is created only when {@code mail.enabled}
 * is {@code true} (e.g. with Gmail app-password credentials); otherwise the
 * {@link com.cloudnest.auth.service.EmailService} falls back to logging
 * rendered emails to the console so OTP flows remain testable in dev.
 */
@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnProperty(name = "mail.enabled", havingValue = "true")
    public JavaMailSender javaMailSender(MailProperties properties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.getHost());
        sender.setPort(properties.getPort());
        sender.setUsername(properties.getUsername());
        sender.setPassword(properties.getPassword());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(properties.isStarttls()));
        props.put("mail.smtp.ssl.trust", properties.getHost());
        props.put("mail.debug", "false");

        return sender;
    }
}
