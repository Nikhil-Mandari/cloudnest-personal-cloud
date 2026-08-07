package com.cloudnest.auth.repository;

import com.cloudnest.auth.entity.TrustedDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link TrustedDevice} operations.
 */
@Repository
public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, Long> {

    Optional<TrustedDevice> findByUserIdAndDeviceId(Long userId, String deviceId);

    List<TrustedDevice> findByUserIdOrderByLastUsedAtDesc(Long userId);

    void deleteByUserIdAndDeviceId(Long userId, String deviceId);
}
