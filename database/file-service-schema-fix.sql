-- ═══════════════════════════════════════════════════════════════════════
-- CloudNest — File Service schema repair (one-off migration)
-- ═══════════════════════════════════════════════════════════════════════
-- Issue:  GET /api/files (and /api/files?folderId=<uuid>) returned 500
--         "An unexpected error occurred" on every listing.
--
-- Root cause:
--   The `file_metadata` table predates the current FileMetadata entity.
--   Hibernate `ddl-auto: update` could not migrate it automatically:
--   adding `uploaded_at datetime(6) NOT NULL` to a table that already
--   contains rows fails under MySQL strict mode ("Incorrect datetime
--   value: '0000-00-00 00:00:00'"), so the column was never created.
--   Every SELECT generated from the entity references `uploaded_at`,
--   producing "Unknown column" → SQLException → HTTP 500.
--
-- Fix (run once against the file-service database):
--   1. Add the column as nullable (allows the ALTER on a non-empty table)
--   2. Backfill existing rows from `created_at` (created_at is NOT NULL)
--   3. Tighten the column back to NOT NULL to match the entity mapping
--
-- Target database: file_db  (run as: mysql -u root -p < this file)
-- ═══════════════════════════════════════════════════════════════════════

ALTER TABLE file_metadata ADD COLUMN uploaded_at datetime(6) NULL AFTER updated_at;

UPDATE file_metadata SET uploaded_at = created_at WHERE uploaded_at IS NULL;

ALTER TABLE file_metadata MODIFY COLUMN uploaded_at datetime(6) NOT NULL;

-- Verification:
--   SHOW COLUMNS FROM file_metadata;   -- uploaded_at must be present, NOT NULL
--   SELECT id, uploaded_at FROM file_metadata;
