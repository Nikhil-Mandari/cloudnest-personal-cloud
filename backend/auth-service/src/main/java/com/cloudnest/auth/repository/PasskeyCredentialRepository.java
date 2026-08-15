package com.cloudnest.auth.repository;

import com.cloudnest.auth.entity.PasskeyCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredential, Long> {

    List<PasskeyCredential> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<PasskeyCredential> findByUserIdAndCredentialId(Long userId, String credentialId);

    Optional<PasskeyCredential> findByCredentialId(String credentialId);

    void deleteByUserIdAndCredentialId(Long userId, String credentialId);

    long countByUserId(Long userId);
}
