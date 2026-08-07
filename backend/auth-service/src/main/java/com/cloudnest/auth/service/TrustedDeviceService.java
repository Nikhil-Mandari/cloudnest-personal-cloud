package com.cloudnest.auth.service;

import com.cloudnest.auth.entity.TrustedDevice;
import com.cloudnest.auth.entity.UserCredential;
import com.cloudnest.auth.exception.TrustedDeviceNotFoundException;
import com.cloudnest.auth.repository.TrustedDeviceRepository;
import com.cloudnest.auth.security.ClientInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages the user's trusted devices ("Remember this device"). Trusted
 * devices skip the OTP step on subsequent logins.
 */
@Slf4j
@Service
public class TrustedDeviceService {

    private final TrustedDeviceRepository repository;

    public TrustedDeviceService(TrustedDeviceRepository repository) {
        this.repository = repository;
    }

    /**
     * @return {@code true} when the device is trusted by the user.
     */
    @Transactional(readOnly = true)
    public boolean isTrusted(Long userId, String deviceId) {
        if (deviceId == null || deviceId.isBlank() || "unknown-device".equals(deviceId)) {
            return false;
        }
        return repository.findByUserIdAndDeviceId(userId, deviceId).isPresent();
    }

    /**
     * Marks (or refreshes) the device as trusted for the user.
     */
    @Transactional
    public void markTrusted(UserCredential user, ClientInfo info) {
        String deviceId = info.device().deviceId();
        TrustedDevice trusted = repository.findByUserIdAndDeviceId(user.getId(), deviceId)
                .orElseGet(() -> TrustedDevice.builder()
                        .userId(user.getId())
                        .deviceId(deviceId)
                        .createdAt(LocalDateTime.now())
                        .build());
        trusted.setDeviceName(info.device().deviceName());
        trusted.setBrowser(info.device().browser());
        trusted.setOs(info.device().os());
        trusted.setIpAddress(info.ipAddress());
        trusted.setLastUsedAt(LocalDateTime.now());
        repository.save(trusted);
        log.info("Device {} marked trusted for userId={}", deviceId, user.getId());
    }

    /**
     * Lists the user's trusted devices, most recently used first.
     */
    @Transactional(readOnly = true)
    public List<TrustedDevice> list(Long userId) {
        return repository.findByUserIdOrderByLastUsedAtDesc(userId);
    }

    /**
     * Removes a trusted device by its primary key.
     */
    @Transactional
    public void removeById(Long userId, Long id) {
        TrustedDevice device = repository.findById(id)
                .filter(d -> d.getUserId().equals(userId))
                .orElseThrow(() -> new TrustedDeviceNotFoundException("Trusted device not found"));
        repository.delete(device);
        log.info("Trusted device {} removed for userId={}", id, userId);
    }

    /**
     * Removes a trusted device by device id (e.g. from the session list).
     */
    @Transactional
    public void removeByDeviceId(Long userId, String deviceId) {
        repository.deleteByUserIdAndDeviceId(userId, deviceId);
    }
}
