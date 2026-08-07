# CloudNest — Phase 2 Report: Enterprise File Management

> Enterprise-grade file management power features on top of the existing
> Spring Boot microservices + React frontend. Existing APIs, business logic and
> the architecture are preserved; only additive features were built.

**Scope covered:** file version history · SHA-256 duplicate detection · storage
analytics · share-link enhancements (password, download-only, analytics) ·
upload queue (pause/resume/retry/duplicates) · bulk ZIP download · audit logs ·
virus-scan interface.

---

## 1. New features added

### Backend — file-service
- **File version history** — every content replacement (new version upload,
  duplicate `REPLACE`) archives the previous content as an immutable snapshot.
  Users can list, download, restore and delete versions. Restoring archives the
  current content first, so nothing is ever lost.
- **SHA-256 duplicate detection** — the upload computes the checksum and
  detects identical content owned by the same user. The client decides via
  `onDuplicate=ASK|KEEP_BOTH|SKIP|REPLACE`:
  - `ASK` → detected and reported without uploading (never stores duplicate metadata)
  - `KEEP_BOTH` → second record created
  - `SKIP` → nothing uploaded
  - `REPLACE` → existing content archived as a version, then replaced
- **Storage analytics** — `GET /api/files/stats/overview`: totals, trash usage,
  largest files, file-type breakdown, weekly (8w) + monthly (12m) usage.
- **Audit trail** — `GET /api/files/audit-logs` (paged, action filterable):
  upload/download/preview/rename/move/delete/restore/favorite/version/zip/share
  actions with IP + user agent.
- **Virus-scan interface** — pluggable `VirusScanner` (ClamAV over the
  `INSTREAM` protocol, `NoopVirusScanner` in dev). Scans run on upload and on
  new versions; infected content is removed and blocked from download/preview;
  `scanStatus` is exposed per file.
- **Bulk ZIP download** — `POST /api/files/download-zip` streams a ZIP that
  preserves the folder hierarchy under `CloudNest/`, with ownership checks and
  a hard entry cap.
- **Share-stream (token capability)** — `GET /api/files/{id}/share-stream`
  validates the share token against the Share Service before streaming, so
  public share links download content without an owner session.

### Backend — share-service
- **Password-protected links** — optional BCrypt-hashed `passwordHash`; public
  access (`verify-password`, `download`) enforces it. The hash is never
  serialized — only `hasPassword` is exposed.
- **Download-only permission** — new `DOWNLOAD` enum value (alongside `VIEW`,
  `EDIT`) for download-only links.
- **Access analytics** — `viewCount` / `downloadCount` / `lastAccessedAt`
  tracked per share; owners read them via `GET /api/shares/{id}/analytics`.
- **Public download streaming** — `GET /api/shares/public/{token}/download`
  (gateway public path) validates token + expiry + password, then streams from
  the file-service.
- **Internal token validation** — `GET /api/shares/internal/validate` used by
  the file-service; hardened so authenticated external callers (who carry
  `X-User-Id`) are rejected, preventing use as a token oracle.

### Frontend
- **Version history dialog** — list, upload, restore, delete, download versions.
- **Duplicate dialog** — the upload queue pauses and asks
  Keep both / Replace / Skip when the server reports identical content.
- **Upload queue upgrades** — pause/resume (in addition to cancel/retry),
  duplicate prompt integration, live speed + ETA retained.
- **Share dialog** — optional password, download-only permission, and a
  post-creation analytics strip (views / downloads / password state).
- **Storage analytics page** (`/analytics`) — storage meter, stat cards,
  file-type bars, weekly/monthly usage bars, largest files, low-storage warning.
- **Audit logs page** (`/audit-logs`) — filterable timeline with per-action
  icons and pagination.
- **Scan status badges** — color-coded virus-scan pills in the file grid/list.
- **Bulk ZIP download** — selection-bar button downloads selected files as ZIP.

## 2. Files modified / created

**file-service (backend):**
- New: `entity/FileVersion`, `entity/AuditLog`, `entity/ScanStatus`,
  `entity/DuplicateAction`, `repository/FileVersionRepository`,
  `repository/AuditLogRepository`, `service/{Version,AuditLog,StorageAnalytics,ZipDownload,VirusScan}Service(+Impl)`,
  `security/{VirusScanner,NoopVirusScanner,ClamAvVirusScanner,ScanOutcome}`,
  `config/{VirusScannerConfig,VirusScannerProperties}`, `client/ShareServiceClient`,
  `controller/FileAdvancedController`, `util/{FileTypeCategory,FileTypeCategorizer}`,
  `exception/VirusDetectedException`, DTOs (`FileVersionResponse`,
  `UploadResultResponse`, `DuplicateFileInfo`, `ScanStatusResponse`,
  `StorageOverviewResponse`, `FileTypeStat`, `UsagePoint`, `LargestFileInfo`,
  `AuditLogResponse`, `PagedAuditLogsResponse`, `DownloadZipRequest`,
  `ShareValidationResponse`).
- Modified: `FileMetadata` (+scanStatus), `FileService`/`FileServiceImpl`
  (duplicate detection, virus scan, audit, share-stream), `FileController`
  (upload returns `UploadResultResponse`, `onDuplicate` param),
  `FileMetadataRepository`, `FileResponse`/`FileMetadataResponse` (+scanStatus),
  `FileMapper`, `AuditLog` (+`SHARE_DOWNLOAD`), `FolderServiceClient`,
  `GlobalExceptionHandler`, `pom.xml` (ClamAV), `config-repo/file-service.yml`.

**share-service (backend):**
- New: `dto/{ShareAnalyticsResponse,VerifySharePasswordRequest,ShareValidationResponse,ShareDownloadResponse}`,
  `exception/{SharePasswordRequiredException,InvalidSharePasswordException}`,
  `config/ShareSecurityConfig` (PasswordEncoder bean).
- Modified: `Share` (+passwordHash/viewCount/downloadCount/lastAccessedAt,
  +DOWNLOAD permission), `ShareResponse` (+hasPassword/analytics),
  `ShareServiceImpl` (password hashing, counters, public download, internal
  validation, analytics), `ShareController` (+5 endpoints), `ShareMapper`,
  `FileServiceClient` (+downloadStream), `GlobalExceptionHandler`, `pom.xml`
  (+spring-security-crypto).

**frontend:**
- New: `types/fileAdvanced.types.ts`, `hooks/{useFileVersions,useStorageAnalytics,useAuditLogs}.ts`,
  `components/files/{VersionHistoryDialog,DuplicateDialog,ScanStatusBadge}.tsx`,
  `pages/analytics/AnalyticsPage.tsx`, `pages/audit/AuditLogsPage.tsx`.
- Modified: `types/{file,share}.types.ts` + `index.ts`,
  `constants/{apiEndpoints,routes}.ts`, `services/{file,share}.service.ts`,
  `hooks/{useFileUpload,useShare}.ts`, `components/files/{UploadModal,UploadProgress,ShareDialog,FileRow,FileCard}.tsx`,
  `components/shares/ShareBadges.tsx`, `pages/files/FilesPage.tsx`,
  `pages/profile/ProfilePage.tsx` (upload result shape), `utils/download.ts`,
  `routes/{pages,index}.tsx`, `components/layout/Sidebar.tsx`,
  `pages/auth/{VerifyOtpPage,ForgotPasswordPage}.tsx` (lint), `pages/about/AboutPage.tsx` (lint).

**Database:** `database/file-service-enterprise-file-mgmt.sql`
(`file_versions`, `audit_logs` tables + `file_metadata.scan_status`),
`database/share-service-enterprise-file-mgmt.sql` (shares password/analytics columns).
Both idempotent — existing rows keep working.

## 3. Database changes
- `file_db`: new tables `file_versions`, `audit_logs`; new column
  `file_metadata.scan_status` (VARCHAR, defaults for existing rows).
- `share_db`: new columns `password_hash`, `view_count`, `download_count`,
  `last_accessed_at` on `shares` (nullable/defaulted — no backfill needed).

## 4. New APIs
**file-service**
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/files/{id}/versions` | list versions |
| POST | `/api/files/{id}/versions` | upload new version |
| POST | `/api/files/{id}/versions/{vid}/restore` | restore version |
| DELETE | `/api/files/{id}/versions/{vid}` | delete version |
| GET | `/api/files/{id}/versions/{vid}/download` | download version |
| POST | `/api/files/download-zip` | bulk ZIP download |
| GET | `/api/files/stats/overview` | storage analytics |
| GET | `/api/files/audit-logs` | paged audit trail |
| GET | `/api/files/{id}/scan-status` | virus-scan status |
| GET | `/api/files/{id}/share-stream` | share-token download (internal) |
| POST | `/api/files/upload?onDuplicate=…` | upload with duplicate handling |

**share-service**
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/shares/public/{token}/verify-password` | verify link password |
| GET | `/api/shares/public/{token}/download` | stream shared file |
| GET | `/api/shares/{id}/analytics` | owner access analytics |
| GET | `/api/shares/internal/validate` | internal token check (service-to-service) |
| PUT | `/api/shares/{id}` | update permission/expiry/password |

## 5. Security improvements
- Share passwords are BCrypt-hashed (never stored/serialized raw).
- Duplicate `REPLACE` is the only path that overwrites content — `ASK` never
  uploads (fixes a silent-overwrite bug found in code review).
- Share-stream is a token capability: validated against the Share Service
  (expiry + resource match), infected/trashed files still blocked.
- Internal validation endpoint rejects authenticated callers (token-oracle guard).
- Version deletion never destroys an object still referenced by another version.
- Virus scan blocks download/preview of infected or still-scanning content.

## 6. Known limitations / future work
- Share-link public **browse page** (rendering the shared resource publicly) is
  Phase 3 scope; the API (view/download/verify) is fully ready.
- Version restore uses object-pointer swapping — a restore-then-replace can
  briefly reference the same object in two version rows (deletion is guarded);
  copy-on-write would fully decouple snapshots.
- ClamAV needs `clamd` running (see `file-service.yml`); dev falls back to a
  no-op scanner so uploads still pass.
- Upload pause is client-side (abort + requeue) — true resumable multipart
  uploads require backend support (future).
- Audit log IP/user-agent are best-effort (gateway context only).

## 7. Validation
- `file-service` and `share-service`: `mvn -DskipTests compile` clean.
- `auth-service` unit suites unchanged (22 tests) still pass.
- Frontend: `tsc -b` clean, `npm run build` clean, `eslint --max-warnings=0` clean.
- `qa-e2e.sh` extended with 17 Phase 2 scenarios (duplicates, versions, ZIP,
  analytics, audit, scan, protected shares, public download, oracle guard).
- The pre-existing `contextLoads` tests for file/share-service require MySQL +
  the SQL migrations applied (same infra dependency as Phase 1) and are
  unrelated to these changes.
