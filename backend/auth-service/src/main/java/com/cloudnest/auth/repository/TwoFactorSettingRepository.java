package com.cloudnest.auth.repository;

import com.cloudnest.auth.entity.TwoFactorSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link TwoFactorSetting}.
 */
@Repository
public interface TwoFactorSettingRepository extends JpaRepository<TwoFactorSetting, Long> {

    Optional<TwoFactorSetting> findByUserId(Long userId);

    boolean existsByUserIdAndEnabledTrue(Long userId);

    void deleteByUserId(Long userId);
}
