-- ═══════════════════════════════════════════════════════════════════════
-- CloudNest — File Service Phase 2 schema (file management power features)
-- ═══════════════════════════════════════════════════════════════════════
-- Adds:
--   1. file_metadata.scan_status      — virus-scan lifecycle column
--   2. file_versions                  — archived content snapshots
--   3. audit_logs                     — immutable file-activity trail
--
-- Target database: file_db
-- Idempotent: safe to run more than once.
-- ═══════════════════════════════════════════════════════════════════════

-- ── 1. Virus-scan status on file_metadata ──────────────────────────────
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'file_metadata'
      AND COLUMN_NAME = 'scan_status'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE file_metadata ADD COLUMN scan_status VARCHAR(20) NULL AFTER updated_at',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE file_metadata SET scan_status = 'CLEAN' WHERE scan_status IS NULL;

-- ── 2. file_versions ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS file_versions (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_metadata_id BIGINT       NOT NULL,
    version_number   INT          NOT NULL,
    object_name      VARCHAR(512) NOT NULL,
    stored_file_name VARCHAR(512) NOT NULL,
    file_size        BIGINT       NOT NULL,
    checksum         VARCHAR(64)  NULL,
    content_type     VARCHAR(100) NOT NULL,
    uploaded_by      BIGINT       NOT NULL,
    note             VARCHAR(512) NULL,
    created_at       DATETIME(6)  NOT NULL,
    UNIQUE KEY uk_file_version (file_metadata_id, version_number),
    CONSTRAINT fk_version_file FOREIGN KEY (file_metadata_id)
        REFERENCES file_metadata(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_file_versions_metadata ON file_versions (file_metadata_id, version_number DESC);

-- ── 3. audit_logs ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_logs (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id      BIGINT        NOT NULL,
    action        VARCHAR(40)   NOT NULL,
    resource_type VARCHAR(20)   NULL,
    resource_id   VARCHAR(64)   NULL,
    resource_name VARCHAR(255)  NULL,
    details       VARCHAR(1024) NULL,
    ip_address    VARCHAR(45)   NULL,
    user_agent    VARCHAR(512)  NULL,
    created_at    DATETIME(6)   NOT NULL,
    KEY idx_audit_owner (owner_id, created_at),
    KEY idx_audit_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Verification ───────────────────────────────────────────────────────
--   SHOW COLUMNS FROM file_metadata LIKE 'scan_status';
--   SHOW TABLES LIKE 'file_versions';
--   SHOW TABLES LIKE 'audit_logs';
