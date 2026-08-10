package com.cloudnest.auth.service.impl;

import com.cloudnest.auth.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails via SMTP using Spring's {@link JavaMailSender}.
 * <p>
 * When {@code mail.host} is empty/absent the service will throw
 * {@link EmailSendException} so the caller can fall back to development mode
 * (returning the OTP in the API response).
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final boolean enabled;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${app.mail.from:noreply@cloudnest.app}") String fromAddress,
            @Value("${app.mail.enabled:false}") boolean enabled) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.enabled = enabled;
        log.info("Email service initialized: enabled={}, from={}", enabled, fromAddress);
    }

    @Override
    public void sendOtpEmail(String to, String otpCode, int expiryMinutes) throws EmailSendException {
        if (!enabled) {
            throw new EmailSendException("Mail service is disabled (app.mail.enabled=false)", null);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Your CloudNest verification code");

            String html = buildOtpEmailHtml(otpCode, expiryMinutes);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("OTP email sent successfully to {}", to);
        } catch (Exception e) {
            throw new EmailSendException("Failed to send OTP email to " + to, e);
        }
    }

    private String buildOtpEmailHtml(String otpCode, int expiryMinutes) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"/></head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 0; background-color: #f4f6f8;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color: #f4f6f8; padding: 40px 0;">
                    <tr>
                        <td align="center">
                            <table role="presentation" width="480" cellspacing="0" cellpadding="0" style="background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.06);">
                                <tr>
                                    <td style="padding: 40px 32px 0; text-align: center;">
                                        <h1 style="font-size: 24px; font-weight: 700; color: #1a1a2e; margin: 0 0 8px;">CloudNest</h1>
                                        <p style="font-size: 15px; color: #666; margin: 0 0 24px;">Your verification code</p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 0 32px; text-align: center;">
                                        <div style="background: #f0f4ff; border-radius: 12px; padding: 24px; margin-bottom: 24px;">
                                            <p style="font-size: 14px; color: #555; margin: 0 0 12px;">Enter this code to continue</p>
                                            <p style="font-family: 'Courier New', monospace; font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #4f46e5; margin: 0;">%s</p>
                                            <p style="font-size: 13px; color: #888; margin: 16px 0 0;">Expires in %d minutes</p>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 0 32px 40px; text-align: center;">
                                        <p style="font-size: 12px; color: #aaa; margin: 0;">
                                            If you didn't request this code, you can safely ignore this email.<br/>
                                            &copy; 2026 CloudNest Technologies
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(otpCode, expiryMinutes);
    }
}