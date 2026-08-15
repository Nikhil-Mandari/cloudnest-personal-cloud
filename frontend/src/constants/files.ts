/**
 * File explorer constants — upload limits, filter definitions, storage quota, etc.
 *
 * The 100 MB cap mirrors the file-service configuration (see
 * config-repo/file-service.yml → `spring.servlet.multipart.max-file-size`).
 */

export const MAX_FILE_SIZE_BYTES = 100 * 1024 * 1024; // 100 MB
export const MAX_FILE_SIZE_MB = MAX_FILE_SIZE_BYTES / (1024 * 1024);

/**
 * Displayed free-tier storage quota (30 GB). Mirrors the FREE plan seeded by
 * the billing-service; the live value always comes from the billing
 * subscription API (this is only the offline fallback).
 */
export const STORAGE_QUOTA_BYTES = 30 * 1024 ** 3; // 30 GB

/** Upper bound on how many files can be queued in a single upload batch. */
export const MAX_FILES_PER_BATCH = 100;

/** How many uploads run concurrently. */
export const UPLOAD_CONCURRENCY = 3;

/** Default expiry presets for the share dialog (in days, or null = never). */
export const SHARE_EXPIRY_OPTIONS: ReadonlyArray<{ label: string; value: string | null }> = [
  { label: 'Never expires', value: null },
  { label: '1 day', value: '1' },
  { label: '7 days', value: '7' },
  { label: '30 days', value: '30' },
] as const;
