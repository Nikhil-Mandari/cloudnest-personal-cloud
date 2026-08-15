package com.cloudnest.auth.repository;

import com.cloudnest.auth.entity.TwoFactorSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TwoFactorSettingsRepository extends JpaRepository<TwoFactorSettings, Long> {

    Optional<TwoFactorSettings> findByUserId(Long userId);
}
