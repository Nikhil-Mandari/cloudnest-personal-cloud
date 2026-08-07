package com.cloudnest.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SMTP delivery settings for transactional email (OTP, security alerts).
 * <p>
 * Bound from {@code mail.*} properties. When {@code mail.enabled} is
 * {@code false} — the default for local development — emails are rendered and
 * written to the application log instead of being sent over SMTP, so the
 * OTP flows remain testable without credentials. Set {@code MAIL_ENABLED=true}
 * plus {@code MAIL_HOST}/{@code MAIL_USERNAME}/{@code MAIL_PASSWORD}
 * (e.g. a Gmail app password) to enable real delivery.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    /** Master switch — {@code false} logs emails to the console. */
    private boolean enabled = false;

    /** SMTP host, e.g. smtp.gmail.com. */
    private String host = "smtp.gmail.com";

    /** SMTP port, e.g. 587 for Gmail STARTTLS. */
    private int port = 587;

    /** SMTP username (the full Gmail address for Gmail). */
    private String username;

    /** SMTP password / app password. */
    private String password;

    /** From address shown on outgoing email. */
    private String from = "CloudNest <noreply@cloudnest.local>";

    /** Whether the connection requires STARTTLS. */
    private boolean starttls = true;

    /** Display name of the application used in the email footer. */
    private String appName = "CloudNest";
}
