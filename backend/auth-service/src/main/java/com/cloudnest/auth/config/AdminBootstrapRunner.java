package com.cloudnest.auth.config;

import com.cloudnest.auth.client.UserServiceClient;
import com.cloudnest.auth.dto.CreateUserProfileRequest;
import com.cloudnest.auth.entity.UserCredential;
import com.cloudnest.auth.repository.UserCredentialRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * First-run administrator bootstrap.
 * <p>
 * On startup, ensures an account with {@code auth.admin.email} exists as
 * {@code ROLE_ADMIN} (creating it or promoting an existing matching account),
 * and best-effort-syncs the profile into the User Service. Fully idempotent
 * and configurable via {@code auth.admin.*} (see config-repo/auth-service.yml);
 * setting {@code auth.admin.email} to blank disables it.
 */
@Slf4j
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserCredentialRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties properties;
    private final UserServiceClient userServiceClient;

    public AdminBootstrapRunner(UserCredentialRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                AuthProperties properties,
                                UserServiceClient userServiceClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.userServiceClient = userServiceClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        AuthProperties.Admin config = properties.getAdmin();
        if (config == null || config.getEmail() == null || config.getEmail().isBlank()) {
            log.info("Admin bootstrap skipped — no auth.admin.email configured");
            return;
        }

        try {
            UserCredential admin = userRepository.findByEmail(config.getEmail()).orElse(null);

            if (admin == null) {
                admin = UserCredential.builder()
                        .username(config.getUsername())
                        .email(config.getEmail())
                        .password(passwordEncoder.encode(config.getPassword()))
                        .role("ROLE_ADMIN")
                        .enabled(true)
                        .status(UserCredential.AccountStatus.ACTIVE)
                        .failedAttempts(0)
                        .emailVerifiedAt(LocalDateTime.now())
                        .build();
                userRepository.save(admin);
                log.info("Admin account created: {} ({})", admin.getEmail(), admin.getUsername());
            } else if (config.isPromoteExisting() && !"ROLE_ADMIN".equals(admin.getRole())) {
                admin.setRole("ROLE_ADMIN");
                admin.setEnabled(true);
                admin.setStatus(UserCredential.AccountStatus.ACTIVE);
                if (admin.getEmailVerifiedAt() == null) {
                    admin.setEmailVerifiedAt(LocalDateTime.now());
                }
                userRepository.save(admin);
                log.info("Existing account promoted to admin: {}", admin.getEmail());
            }

            // Ensure the profile exists in the User Service (idempotent).
            try {
                userServiceClient.createProfile(CreateUserProfileRequest.builder()
                        .username(admin.getUsername())
                        .email(admin.getEmail())
                        .displayName("CloudNest Administrator")
                        .role("ROLE_ADMIN")
                        .enabled(true)
                        .build());
                log.debug("Admin profile synced to user-service: {}", admin.getEmail());
            } catch (Exception e) {
                log.warn("Admin profile sync to user-service failed (retried on next start): {}",
                        e.getMessage());
            }
        } catch (Exception e) {
            log.error("Admin bootstrap failed: {}", e.getMessage(), e);
        }
    }
}
