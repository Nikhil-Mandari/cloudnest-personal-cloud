-- ============================================================================
-- CloudNest — Share Service: Enterprise File Management migration (Phase 2)
-- ----------------------------------------------------------------------------
-- Adds password-protected share links and access analytics (view / download
-- counters) to the existing `shares` table.
--
-- Idempotent: safe to run multiple times. Existing rows keep working — all new
-- columns either have defaults or are nullable.
-- ============================================================================

USE share_db;

-- Password hash for password-protected share links (BCrypt, nullable = open link)
ALTER TABLE shares
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(100) NULL AFTER expiry_date;

-- Lifetime access analytics
ALTER TABLE shares
    ADD COLUMN IF NOT EXISTS view_count BIGINT NOT NULL DEFAULT 0 AFTER password_hash;

ALTER TABLE shares
    ADD COLUMN IF NOT EXISTS download_count BIGINT NOT NULL DEFAULT 0 AFTER view_count;

ALTER TABLE shares
    ADD COLUMN IF NOT EXISTS last_accessed_at DATETIME(6) NULL AFTER download_count;

-- ============================================================================
-- Note on the Permission enum: the Java enum now also supports DOWNLOAD
-- (download-only links). MySQL stores the enum as a VARCHAR so no schema
-- change is required for new permission values.
-- ============================================================================
