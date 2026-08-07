package com.cloudnest.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Enterprise authentication tuning knobs for the Auth Service.
 * <p>
 * Bound from {@code auth.*} properties in the central configuration
 * (config-repo/auth-service.yml). Every value has a sane default so the
 * service works out of the box and can be overridden per environment.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /** OTP verification settings. */
    private Otp otp = new Otp();

    /** Brute-force account lock settings. */
    private Lock lock = new Lock();

    /** Token lifecycle settings. */
    private Token token = new Token();

    /** Security behaviour toggles. */
    private Security security = new Security();

    /** First-run administrator bootstrap. */
    private Admin admin = new Admin();

    /**
     * OTP configuration.
     */
    @Getter
    @Setter
    public static class Otp {
        /** Number of digits in the generated OTP. */
        private int length = 6;

        /** Minutes before an unverified OTP expires. */
        private int expiryMinutes = 5;

        /** Seconds a user must wait before requesting a resend. */
        private long resendCooldownSeconds = 60;

        /** Maximum verification attempts before the OTP is invalidated. */
        private int maxAttempts = 5;

        /**
         * Server-side secret mixed into the stored code hash (HMAC-SHA256) so
         * an offline database leak cannot be brute-forced. Override per
         * environment via {@code AUTH_OTP_PEPPER}.
         */
        private String pepper = "cloudnest-dev-otp-pepper";
    }

    /**
     * Account lock configuration (failed-login brute-force protection).
     */
    @Getter
    @Setter
    public static class Lock {
        /** Consecutive failed password attempts before the account locks. */
        private int maxFailedAttempts = 5;

        /** Minutes the account stays locked after the threshold is reached. */
        private int durationMinutes = 15;
    }

    /**
     * Token lifecycle configuration.
     */
    @Getter
    @Setter
    public static class Token {
        /** Life of a refresh token in days (rotating). */
        private int refreshExpirationDays = 30;

        /** Life of an OTP challenge token in minutes. */
        private int challengeExpirationMinutes = 10;
    }

    /**
     * Security behaviour toggles.
     */
    @Getter
    @Setter
    public static class Security {
        /**
         * When {@code true}, logins from a trusted device skip the OTP step.
         */
        private boolean skipOtpOnTrustedDevice = true;
    }

    /**
     * First-run administrator bootstrap (created/promoted on startup).
     */
    @Getter
    @Setter
    public static class Admin {
        /** Email of the admin account. Blank disables the bootstrap. */
        private String email = "admin@cloudnest.test";

        /** Username of the admin account (used when the account is created). */
        private String username = "admin";

        /** Initial password of the admin account (used when created). */
        private String password = "Admin@123456";

        /**
         * When {@code true}, an existing account with the configured email is
         * promoted to ROLE_ADMIN on startup (idempotent).
         */
        private boolean promoteExisting = true;
    }
}
