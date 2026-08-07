-- =============================================================================
-- CloudNest — Phase 6: Two-Factor Authentication (TOTP) + WebAuthn Passkeys
-- -----------------------------------------------------------------------------
-- Run this against the auth_db BEFORE starting auth-service (the service uses
-- spring.jpa.hibernate.ddl-auto=validate, so new tables must exist already).
--
--   mysql -u root -p auth_db < auth-service-2fa-passkeys.sql
--
-- Idempotent: CREATE TABLE IF NOT EXISTS is safe to re-run.
-- =============================================================================

-- TOTP 2FA setting per user (secret kept until disabled).
CREATE TABLE IF NOT EXISTS two_factor_settings (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    user_id            BIGINT       NOT NULL,
    secret             VARCHAR(64)  NOT NULL,
    enabled            BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled_at         DATETIME     NULL,
    -- Last accepted TOTP time-step counter (replay protection).
    last_used_counter  BIGINT       NULL,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_two_factor_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Backfill for databases that applied an earlier revision without the
-- replay-protection column (MySQL has no ADD COLUMN IF NOT EXISTS).
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'two_factor_settings'
      AND COLUMN_NAME = 'last_used_counter');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE two_factor_settings ADD COLUMN last_used_counter BIGINT NULL AFTER enabled_at',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Single-use backup codes (SHA-256 hashes; the plaintext is shown once).
CREATE TABLE IF NOT EXISTS backup_codes (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    code_hash  VARCHAR(64) NOT NULL,
    used       BOOLEAN     NOT NULL DEFAULT FALSE,
    used_at    DATETIME    NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_backup_codes_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- WebAuthn (passkey) credentials — COSE public keys, base64url-encoded.
CREATE TABLE IF NOT EXISTS passkey_credentials (
    id             VARCHAR(36)   NOT NULL,
    user_id        BIGINT        NOT NULL,
    credential_id  VARCHAR(512)  NOT NULL,
    user_handle    VARCHAR(255)  NOT NULL,
    public_key_cose VARCHAR(2048) NOT NULL,
    signature_count BIGINT       NOT NULL DEFAULT 0,
    transports     VARCHAR(255)  NULL,
    nickname       VARCHAR(100)  NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at   DATETIME      NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_passkey_credential (credential_id),
    KEY idx_passkeys_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
