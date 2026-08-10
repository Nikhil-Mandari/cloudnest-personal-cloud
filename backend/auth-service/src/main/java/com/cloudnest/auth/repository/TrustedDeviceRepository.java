package com.cloudnest.auth.repository;

import com.cloudnest.auth.entity.TrustedDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, Long> {

    List<TrustedDevice> findByUserId(Long userId);

    Optional<TrustedDevice> findByUserIdAndDeviceId(Long userId, String deviceId);

    boolean existsByUserIdAndDeviceId(Long userId, String deviceId);

    long countByUserId(Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
}
