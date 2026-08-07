package com.cloudnest.auth.service;

<<<<<<< Updated upstream
import com.cloudnest.auth.client.NotificationServiceClient;
import com.cloudnest.auth.dto.NotificationCreateRequest;
=======
>>>>>>> Stashed changes
import com.cloudnest.auth.entity.LoginHistory;
import com.cloudnest.auth.entity.SecurityLog;
import com.cloudnest.auth.entity.UserCredential;
import com.cloudnest.auth.entity.UserSession;
import com.cloudnest.auth.repository.LoginHistoryRepository;
import com.cloudnest.auth.repository.SecurityLogRepository;
import com.cloudnest.auth.repository.UserSessionRepository;
import com.cloudnest.auth.security.ClientInfo;
import com.cloudnest.auth.dto.SecurityLogResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Central sink for security-relevant events: writes login history rows,
 * appends to the security log, and raises email alerts for new / unknown
 * device sign-ins.
 */
@Slf4j
@Service
public class SecurityEventService {

    /** Security log action constants. */
    public static final String ACTION_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String ACTION_LOGIN_FAILED = "LOGIN_FAILED";
    public static final String ACTION_LOGIN_LOCKED = "LOGIN_LOCKED";
    public static final String ACTION_OTP_VERIFIED = "OTP_VERIFIED";
    public static final String ACTION_PASSWORD_CHANGED = "PASSWORD_CHANGED";
    public static final String ACTION_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_LOGOUT_ALL = "LOGOUT_ALL";
    public static final String ACTION_SESSION_ENDED = "SESSION_ENDED";
    public static final String ACTION_DEVICE_TRUSTED = "DEVICE_TRUSTED";
    public static final String ACTION_DEVICE_UNTRUSTED = "DEVICE_UNTRUSTED";
    public static final String ACTION_ACCOUNT_ACTIVATED = "ACCOUNT_ACTIVATED";
    public static final String ACTION_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String ACTION_ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
    public static final String ACTION_ACCOUNT_ENABLED = "ACCOUNT_ENABLED";
    public static final String ACTION_ROLE_CHANGED = "ROLE_CHANGED";

    private final LoginHistoryRepository loginHistoryRepository;
    private final SecurityLogRepository securityLogRepository;
    private final UserSessionRepository sessionRepository;
    private final EmailService emailService;
    private final TrustedDeviceService trustedDeviceService;
<<<<<<< Updated upstream
    private final NotificationServiceClient notificationClient;
=======
>>>>>>> Stashed changes

    public SecurityEventService(LoginHistoryRepository loginHistoryRepository,
                                SecurityLogRepository securityLogRepository,
                                UserSessionRepository sessionRepository,
                                EmailService emailService,
<<<<<<< Updated upstream
                                TrustedDeviceService trustedDeviceService,
                                NotificationServiceClient notificationClient) {
=======
                                TrustedDeviceService trustedDeviceService) {
>>>>>>> Stashed changes
        this.loginHistoryRepository = loginHistoryRepository;
        this.securityLogRepository = securityLogRepository;
        this.sessionRepository = sessionRepository;
        this.emailService = emailService;
        this.trustedDeviceService = trustedDeviceService;
<<<<<<< Updated upstream
        this.notificationClient = notificationClient;
=======
>>>>>>> Stashed changes
    }

    /**
     * Records a sign-in attempt (success or failure) in the login history.
     */
    @Transactional
    public void recordLogin(UserCredential user, boolean success, ClientInfo info, String failureReason) {
        LoginHistory history = LoginHistory.builder()
                .userId(user.getId())
                .success(success)
                .ipAddress(info.ipAddress())
                .browser(info.device().browser())
                .os(info.device().os())
                .deviceType(info.device().deviceType())
                .deviceName(info.device().deviceName())
                .location(info.location())
                .failureReason(failureReason)
                .loginTime(LocalDateTime.now())
                .build();
        loginHistoryRepository.save(history);
    }

    /**
     * Paginated security log for the user, newest first.
     */
    @Transactional(readOnly = true)
    public Page<SecurityLogResponse> pageLogs(Long userId, int page, int size) {
        return securityLogRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(entry -> SecurityLogResponse.builder()
                        .id(entry.getId())
                        .action(entry.getAction())
                        .details(entry.getDetails())
                        .ipAddress(entry.getIpAddress())
                        .browser(entry.getBrowser())
                        .os(entry.getOs())
                        .location(entry.getLocation())
                        .createdAt(entry.getCreatedAt())
                        .build());
    }

    /**
     * Appends an entry to the user's security log.
     */
    @Transactional
    public void log(UserCredential user, String action, ClientInfo info, String details) {
        SecurityLog entry = SecurityLog.builder()
                .userId(user.getId())
                .action(action)
                .details(details)
                .ipAddress(info.ipAddress())
                .browser(info.device().browser())
                .os(info.device().os())
                .location(info.location())
                .createdAt(LocalDateTime.now())
                .build();
        securityLogRepository.save(entry);
    }

    /**
     * Detects whether the device has been seen before for this user: trusted
     * device or any previous (active or ended) session with the same device id.
     */
    @Transactional(readOnly = true)
    public boolean isKnownDevice(UserCredential user, ClientInfo info) {
        if (trustedDeviceService.isTrusted(user.getId(), info.device().deviceId())) {
            return true;
        }
        return sessionRepository.existsByUserIdAndDeviceId(user.getId(), info.device().deviceId());
    }

    /**
<<<<<<< Updated upstream
     * Sends the appropriate sign-in alert (email + in-app notification)
     * after a successful login. New/unknown devices get a stricter alert.
=======
     * Sends the appropriate sign-in alert email after a successful login.
>>>>>>> Stashed changes
     */
    public void sendLoginAlert(UserCredential user, ClientInfo info) {
        if (!isKnownDevice(user, info)) {
            emailService.sendUnknownDeviceAlert(user.getEmail(), user.getUsername(), info);
<<<<<<< Updated upstream
            notify(user, NotificationServiceClient.TYPE_UNKNOWN_DEVICE_LOGIN,
                    "New device sign-in",
                    "Signed in from " + describeClient(info)
                            + " — a device we hadn't seen before.");
            log(user, ACTION_LOGIN_SUCCESS, info, "Sign-in from an unknown device — alert email sent");
        } else {
            emailService.sendNewLoginAlert(user.getEmail(), user.getUsername(), info);
            notify(user, NotificationServiceClient.TYPE_LOGIN_ALERT,
                    "New sign-in",
                    "Signed in from " + describeClient(info) + ".");
=======
            log(user, ACTION_LOGIN_SUCCESS, info, "Sign-in from an unknown device — alert email sent");
        } else {
            emailService.sendNewLoginAlert(user.getEmail(), user.getUsername(), info);
>>>>>>> Stashed changes
        }
    }

    /**
<<<<<<< Updated upstream
     * Sends the account-locked alert (email + in-app notification).
     */
    public void sendAccountLockedAlert(UserCredential user, ClientInfo info, int lockMinutes) {
        emailService.sendAccountLockedAlert(user.getEmail(), user.getUsername(), lockMinutes);
        notify(user, NotificationServiceClient.TYPE_ACCOUNT_LOCKED,
                "Account temporarily locked",
                "Too many failed sign-in attempts — your account is locked for "
                        + lockMinutes + " minutes.");
        log(user, ACTION_ACCOUNT_LOCKED, info, "Account locked after repeated failed attempts");
    }

    /**
     * Creates an in-app notification for the user. Best-effort and
     * fire-and-forget: a notification-service outage is logged and never
     * blocks the authentication flow.
     */
    public void notify(UserCredential user, String type, String title, String message) {
        try {
            notificationClient.create(NotificationCreateRequest.builder()
                    .userId(user.getId())
                    .type(type)
                    .title(title)
                    .message(message)
                    .build());
        } catch (Exception e) {
            log.warn("In-app notification failed for userId={} (type={}): {}",
                    user.getId(), type, e.getMessage());
        }
    }

    /**
     * Renders a short human-readable client description for notification
     * messages: device name · browser · location (unknown parts omitted).
     */
    public String describeClient(ClientInfo info) {
        if (info == null || info.device() == null) {
            return "an unrecognised device";
        }
        StringBuilder sb = new StringBuilder(info.device().deviceName() == null
                || info.device().deviceName().isBlank()
                ? "an unrecognised device"
                : info.device().deviceName());
        String browser = info.device().browser();
        if (browser != null && !browser.isBlank()) {
            sb.append(" · ").append(browser);
            String os = info.device().os();
            if (os != null && !os.isBlank()) {
                sb.append(" on ").append(os);
            }
        }
        String location = info.location();
        if (location != null && !location.isBlank() && !"Unknown".equalsIgnoreCase(location)) {
            sb.append(" · ").append(location);
        }
        return sb.toString();
    }
=======
     * Sends the account-locked alert email.
     */
    public void sendAccountLockedAlert(UserCredential user, ClientInfo info, int lockMinutes) {
        emailService.sendAccountLockedAlert(user.getEmail(), user.getUsername(), lockMinutes);
        log(user, ACTION_ACCOUNT_LOCKED, info, "Account locked after repeated failed attempts");
    }
>>>>>>> Stashed changes
}
