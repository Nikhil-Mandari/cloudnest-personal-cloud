package com.cloudnest.auth.service;

import com.cloudnest.auth.entity.UserCredential;
import com.cloudnest.auth.entity.UserSession;
import com.cloudnest.auth.exception.SessionNotFoundException;
import com.cloudnest.auth.repository.UserSessionRepository;
import com.cloudnest.auth.security.ClientInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages active sign-in sessions (one per device). Sessions enable the
 * "Active devices" screen, per-device logout, and silent refresh of access
 * tokens.
 */
@Slf4j
@Service
public class SessionService {

    private final UserSessionRepository sessionRepository;

    public SessionService(UserSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Creates an active session for the given user and device.
     *
     * @return the new session's public id
     */
    @Transactional
    public String create(UserCredential user, ClientInfo info, boolean trusted) {
        String sessionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        UserSession session = UserSession.builder()
                .userId(user.getId())
                .sessionId(sessionId)
                .deviceId(info.device().deviceId())
                .deviceName(info.device().deviceName())
                .browser(info.device().browser())
                .os(info.device().os())
                .deviceType(info.device().deviceType())
                .ipAddress(info.ipAddress())
                .location(info.location())
                .active(true)
                .trusted(trusted)
                .loginTime(now)
                .lastActive(now)
                .build();
        sessionRepository.save(session);
        return sessionId;
    }

    /**
     * Finds an active session by its public id.
     */
    @Transactional(readOnly = true)
    public Optional<UserSession> findActive(String sessionId) {
        return sessionRepository.findBySessionIdAndActiveTrue(sessionId);
    }

    /**
     * Touches the session so "last active" reflects fresh activity.
     */
    @Transactional
    public void touch(String sessionId) {
        if (sessionId == null) {
            return;
        }
        sessionRepository.findBySessionIdAndActiveTrue(sessionId).ifPresent(session -> {
            session.setLastActive(LocalDateTime.now());
            sessionRepository.save(session);
        });
    }

    /**
     * Ends a single session.
     */
    @Transactional
    public void end(String sessionId) {
        if (sessionId == null) {
            return;
        }
        UserSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found"));
        if (Boolean.TRUE.equals(session.getActive())) {
            session.setActive(false);
            session.setEndedAt(LocalDateTime.now());
            sessionRepository.save(session);
            log.info("Ended session {} for userId={}", sessionId, session.getUserId());
        }
    }

    /**
     * Ends every active session for the user.
     *
     * @return the number of sessions ended
     */
    @Transactional
    public int endAll(Long userId) {
        List<UserSession> active = sessionRepository.findByUserIdAndActiveTrueOrderByLastActiveDesc(userId);
        LocalDateTime now = LocalDateTime.now();
        active.forEach(session -> {
            session.setActive(false);
            session.setEndedAt(now);
        });
        sessionRepository.saveAll(active);
        if (!active.isEmpty()) {
            log.info("Ended {} session(s) for userId={}", active.size(), userId);
        }
        return active.size();
    }

    /**
     * Lists active sessions for the user, newest activity first.
     */
    @Transactional(readOnly = true)
    public List<UserSession> listActive(Long userId) {
        return sessionRepository.findByUserIdAndActiveTrueOrderByLastActiveDesc(userId);
    }
}
