package com.cloudnest.auth.service;

import com.cloudnest.auth.config.MailProperties;
import com.cloudnest.auth.security.ClientInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Sends transactional email (OTP codes, security alerts, confirmations).
 * <p>
 * When {@code mail.enabled} is {@code false} the rendered messages are logged
 * at INFO level instead of being sent — the standard local-development mode.
 * All failures are logged and swallowed: an email that cannot be delivered
 * must never block the authentication flow.
 */
@Slf4j
@Service
public class EmailService {

    private final MailProperties properties;
    private final JavaMailSender mailSender;

    public EmailService(MailProperties properties, ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.properties = properties;
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    /**
     * @return {@code true} when real SMTP delivery is configured.
     */
    public boolean isEnabled() {
        return properties.isEnabled() && mailSender != null;
    }

    /**
     * Sends the six-digit verification code for the given purpose.
     */
    public void sendOtp(String to, String name, String code, String purposeLabel, int expiryMinutes) {
        String subject = purposeLabel + " code — " + properties.getAppName();
        String body = """
                <p>Hi %s,</p>
                <p>Use this code to %s:</p>
                <h2 style="letter-spacing:8px;font-size:28px;">%s</h2>
                <p>This code expires in <strong>%d minutes</strong>. If you didn't request it,
                you can safely ignore this email — your account stays protected.</p>
                """.formatted(escape(name), purposeText(purposeLabel), code, expiryMinutes);
        dispatch(to, subject, body);
    }

    /**
     * Welcome email after the account is activated.
     */
    public void sendWelcome(String to, String name) {
        String subject = "Welcome to " + properties.getAppName() + " 🎉";
        String body = """
                <p>Hi %s,</p>
                <p>Your %s account is now active. You can upload, organise, preview and
                share files from anywhere.</p>
                """.formatted(escape(name), properties.getAppName());
        dispatch(to, subject, body);
    }

    /**
     * Password reset confirmation email.
     */
    public void sendPasswordResetConfirmation(String to, String name) {
        String subject = "Your password was reset — " + properties.getAppName();
        String body = """
                <p>Hi %s,</p>
                <p>Your password was successfully reset. If this wasn't you, sign in and
                change it immediately, and remove any unrecognised devices.</p>
                """.formatted(escape(name));
        dispatch(to, subject, body);
    }

    /**
     * Alert sent after the password is changed from the settings page.
     */
    public void sendPasswordChangedAlert(String to, String name, ClientInfo info) {
        String subject = "Your password was changed — " + properties.getAppName();
        String body = """
                <p>Hi %s,</p>
                <p>The password for your account was changed.</p>
                %s
                <p>If this wasn't you, contact support immediately.</p>
                """.formatted(escape(name), clientDetailsHtml(info));
        dispatch(to, subject, body);
    }

    /**
     * New-login notification email.
     */
    public void sendNewLoginAlert(String to, String name, ClientInfo info) {
        String subject = "New sign-in to your account — " + properties.getAppName();
        String body = """
                <p>Hi %s,</p>
                <p>A new sign-in happened on your account.</p>
                %s
                <p>If this was you, no action is needed.</p>
                """.formatted(escape(name), clientDetailsHtml(info));
        dispatch(to, subject, body);
    }

    /**
     * Unknown-device sign-in alert email (stricter tone).
     */
    public void sendUnknownDeviceAlert(String to, String name, ClientInfo info) {
        String subject = "⚠️ New device sign-in — " + properties.getAppName();
        String body = """
                <p>Hi %s,</p>
                <p>We noticed a sign-in from a device we haven't seen before.</p>
                %s
                <p>If this wasn't you, <strong>change your password now</strong> and review
                your active sessions in Security settings.</p>
                """.formatted(escape(name), clientDetailsHtml(info));
        dispatch(to, subject, body);
    }

    /**
     * Account-locked alert after too many failed attempts.
     */
    public void sendAccountLockedAlert(String to, String name, int minutes) {
        String subject = "Your account was locked — " + properties.getAppName();
        String body = """
                <p>Hi %s,</p>
                <p>Your account was temporarily locked after too many failed sign-in
                attempts. It will unlock automatically in <strong>%d minutes</strong>.</p>
                <p>If this wasn't you, change your password once you're back in.</p>
                """.formatted(escape(name), minutes);
        dispatch(to, subject, body);
    }

    // -- Private helpers -----------------------------------------------------

    private void dispatch(String to, String subject, String htmlBody) {
        String body = wrap(htmlBody);
        if (isEnabled()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject(subject);
                message.setText(toPlainText(htmlBody));
                message.setFrom(properties.getFrom());
                mailSender.send(message);
                log.info("Email sent to {} — subject='{}'", to, subject);
            } catch (Exception e) {
                log.warn("Failed to send email to {} (subject '{}'): {}", to, subject, e.getMessage());
            }
            return;
        }
        // Dev/console mode — log the message so flows stay testable.
        log.info("[{}] TO: {} | SUBJECT: {}\n{}", properties.getAppName(), to, subject, body);
    }

    private String clientDetailsHtml(ClientInfo info) {
        if (info == null) {
            return "";
        }
        String when = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        StringBuilder sb = new StringBuilder("<table style=\"border-collapse:collapse;font-size:14px;margin:16px 0;\">");
        row(sb, "Time", when);
        row(sb, "Device", info.device() == null ? "Unknown" : info.device().deviceName());
        row(sb, "Browser", info.device() == null ? "Unknown" : info.device().browser());
        row(sb, "Operating system", info.device() == null ? "Unknown" : info.device().os());
        row(sb, "IP address", info.ipAddress() == null ? "Unknown" : info.ipAddress());
        row(sb, "Approximate location", info.location());
        sb.append("</table>");
        return sb.toString();
    }

    private void row(StringBuilder sb, String label, String value) {
        sb.append("<tr><td style=\"padding:4px 12px 4px 0;color:#6b7280;\">").append(label)
                .append("</td><td style=\"padding:4px 0;font-weight:500;\">")
                .append(escape(value == null ? "Unknown" : value)).append("</td></tr>");
    }

    private String purposeText(String purpose) {
        return switch (purpose.toLowerCase()) {
            case "login" -> "finish signing in";
            case "password reset" -> "reset your password";
            default -> "activate your account";
        };
    }

    private String wrap(String htmlBody) {
        return """
                <!doctype html><html><body style="font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;background:#f9fafb;margin:0;padding:24px;">
                <div style="max-width:520px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:12px;padding:32px;">
                  <div style="font-size:20px;font-weight:700;color:#4f46e5;margin-bottom:16px;">☁️ %s</div>
                  %s
                  <p style="margin-top:24px;font-size:12px;color:#9ca3af;border-top:1px solid #f3f4f6;padding-top:12px;">
                    You received this email because of activity on your %s account.</p>
                </div></body></html>
                """.formatted(properties.getAppName(), htmlBody, properties.getAppName());
    }

    private String toPlainText(String html) {
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
