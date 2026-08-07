-- ═══════════════════════════════════════════════════════════════════════════
-- CloudNest — Auth Service: Enterprise Authentication schema
-- ═══════════════════════════════════════════════════════════════════════════
-- Adds the tables backing Phase 1 (Enterprise Auth & Security):
--   otp_verifications  – email OTP codes (hashed, expiring, attempt-limited)
--   refresh_tokens     – rotating JWT refresh tokens (hashed, revocable)
--   trusted_devices    – devices that skip the OTP step
--   user_sessions      – active sign-in sessions (devices screen)
--   login_history      – successful/failed sign-in attempts
--   security_logs      – audit trail of security actions
-- Plus new columns on user_credentials (status, lock, verification times).
--
-- Idempotent: safe to run multiple times (uses information_schema guards).
--
-- Target database: auth_db
--   mysql -u root -p auth_db < database/auth-service-enterprise-auth.sql
-- ═══════════════════════════════════════════════════════════════════════════

USE auth_db;

-- ── OTP verifications ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS otp_verifications (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    purpose       VARCHAR(20)  NOT NULL COMMENT 'REGISTRATION | LOGIN | PASSWORD_RESET',
    code_hash     VARCHAR(64)  NOT NULL COMMENT 'SHA-256 of the code (never stored raw)',
    expires_at    DATETIME(6)  NOT NULL,
    attempts      INT          NOT NULL DEFAULT 0,
    max_attempts  INT          NOT NULL DEFAULT 5,
    verified      BOOLEAN      NOT NULL DEFAULT FALSE,
    consumed      BOOLEAN      NOT NULL DEFAULT FALSE,
    requested_at  DATETIME(6)  NOT NULL,
    resent_at     DATETIME(6)  NULL,
    PRIMARY KEY (id),
    KEY idx_otp_user_purpose (user_id, purpose)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Refresh tokens ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    session_id   VARCHAR(36)  NOT NULL,
    token_hash   VARCHAR(64)  NOT NULL COMMENT 'SHA-256 of the raw refresh token',
    jti          VARCHAR(36)  NOT NULL COMMENT 'unique token id claim',
    expires_at   DATETIME(6)  NOT NULL,
    revoked      BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at   DATETIME(6)  NULL,
    replaced_by  VARCHAR(36)  NULL COMMENT 'jti of the token that replaced this one',
    created_at   DATETIME(6)  NOT NULL,
    last_used_at DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_refresh_token_hash (token_hash),
    UNIQUE KEY uq_refresh_jti (jti),
    KEY idx_refresh_user (user_id),
    KEY idx_refresh_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Trusted devices ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS trusted_devices (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    device_id    VARCHAR(64)  NOT NULL COMMENT 'stable client device identifier',
    device_name  VARCHAR(255) NULL,
    browser      VARCHAR(100) NULL,
    os           VARCHAR(100) NULL,
    ip_address   VARCHAR(45)  NULL,
    last_used_at DATETIME(6)  NULL,
    created_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_trusted_user_device (user_id, device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── User sessions ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_sessions (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    session_id  VARCHAR(36)  NOT NULL COMMENT 'public session id embedded in tokens',
    device_id   VARCHAR(64)  NOT NULL,
    device_name VARCHAR(255) NULL,
    browser     VARCHAR(100) NULL,
    os          VARCHAR(100) NULL,
    device_type VARCHAR(20)  NULL COMMENT 'DESKTOP | TABLET | MOBILE | OTHER',
    ip_address  VARCHAR(45)  NULL,
    location    VARCHAR(255) NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    is_trusted  BOOLEAN      NOT NULL DEFAULT FALSE,
    login_time  DATETIME(6)  NOT NULL,
    last_active DATETIME(6)  NOT NULL,
    ended_at    DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_session_id (session_id),
    KEY idx_session_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Login history ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS login_history (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL,
    success        BOOLEAN      NOT NULL,
    ip_address     VARCHAR(45)  NULL,
    browser        VARCHAR(100) NULL,
    os             VARCHAR(100) NULL,
    device_type    VARCHAR(20)  NULL,
    device_name    VARCHAR(255) NULL,
    location       VARCHAR(255) NULL,
    failure_reason VARCHAR(255) NULL,
    login_time     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_login_user_time (user_id, login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Security logs ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS security_logs (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    user_id    BIGINT        NOT NULL,
    action     VARCHAR(50)   NOT NULL,
    details    VARCHAR(1000) NULL,
    ip_address VARCHAR(45)   NULL,
    browser    VARCHAR(100)  NULL,
    os         VARCHAR(100)  NULL,
    location   VARCHAR(255)  NULL,
    created_at DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_security_user_time (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── user_credentials: new security columns ────────────────────────────────
-- Idempotent add-column helper (MySQL has no ADD COLUMN IF NOT EXISTS).
-- Existing rows default to ACTIVE so current accounts keep working.

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = 'auth_db' AND TABLE_NAME = 'user_credentials'
               AND COLUMN_NAME = 'status');
SET @sql := IF(@col = 0,
    'ALTER TABLE user_credentials ADD COLUMN status VARCHAR(25) NOT NULL DEFAULT ''ACTIVE'' AFTER enabled',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = 'auth_db' AND TABLE_NAME = 'user_credentials'
               AND COLUMN_NAME = 'failed_attempts');
SET @sql := IF(@col = 0,
    'ALTER TABLE user_credentials ADD COLUMN failed_attempts INT NOT NULL DEFAULT 0 AFTER status',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = 'auth_db' AND TABLE_NAME = 'user_credentials'
               AND COLUMN_NAME = 'locked_until');
SET @sql := IF(@col = 0,
    'ALTER TABLE user_credentials ADD COLUMN locked_until DATETIME(6) NULL AFTER failed_attempts',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = 'auth_db' AND TABLE_NAME = 'user_credentials'
               AND COLUMN_NAME = 'email_verified_at');
SET @sql := IF(@col = 0,
    'ALTER TABLE user_credentials ADD COLUMN email_verified_at DATETIME(6) NULL AFTER locked_until',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = 'auth_db' AND TABLE_NAME = 'user_credentials'
               AND COLUMN_NAME = 'last_login_at');
SET @sql := IF(@col = 0,
    'ALTER TABLE user_credentials ADD COLUMN last_login_at DATETIME(6) NULL AFTER email_verified_at',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = 'auth_db' AND TABLE_NAME = 'user_credentials'
               AND COLUMN_NAME = 'password_changed_at');
SET @sql := IF(@col = 0,
    'ALTER TABLE user_credentials ADD COLUMN password_changed_at DATETIME(6) NULL AFTER last_login_at',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── Verification ──────────────────────────────────────────────────────────
--   SHOW TABLES;                                     -- 6 new tables
--   SHOW COLUMNS FROM user_credentials;              -- status, failed_attempts, ...
--   SELECT username, status FROM user_credentials;   -- existing rows = ACTIVE
