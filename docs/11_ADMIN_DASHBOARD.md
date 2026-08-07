# Phase 4 — Admin Dashboard (Users · Storage · Analytics · Security · Audit · System Health)

Platform-wide administration built on top of the existing microservices without
changing their architecture, database-per-service rule, or public APIs.

---

## What was built

### 1. Trusted admin identity (critical security fix)

**API Gateway — header spoofing eliminated.** Previously any client could send
`X-User-Id` / `X-User-Role` headers and downstream services trusted them. The
`AuthenticationFilter` now **strips** caller-supplied values and **re-sets**
them from the validated JWT on every protected route. All admin checks below
are therefore trustworthy.

- `AuthenticationFilter` — replace (not append) identity headers from JWT claims.
- Gateway `AdminController` (`GET /api/admin/system/health`) — aggregates every
  Eureka-discovered service, probes each `/actuator/health` with a 3 s timeout,
  and returns per-service status, instance count, endpoint and healthy/total
  counts. Requires `ROLE_ADMIN` (checked against the JWT-derived header).

### 2. Auth Service

- **Profile provisioning (Feign).** New `UserServiceClient` (+ request
  interceptor that injects `X-User-Role: ROLE_ADMIN` for internal calls) with
  `POST /api/users`. Profiles are synced best-effort at registration, retried
  at email verification, and **re-synced on every successful sign-in** — this
  self-heals accounts created before provisioning existed.
- **Admin bootstrap runner.** On startup, `auth.admin.email` (default
  `admin@cloudnest.test` / `Admin@123456`) is created or promoted to
  `ROLE_ADMIN` and its profile synced to the user-service. Fully idempotent,
  disable by blanking `ADMIN_EMAIL`.
- **Disabled-account login block.** `login` now rejects accounts with
  `enabled = false` (403) before the password check, recorded in the login
  history.
- **Admin endpoints** (all `AdminGuard`-protected):
  - `GET /api/auth/admin/security-overview` — account mix, logins, failed
    logins (7 d), active sessions, trusted devices.
  - `GET /api/auth/admin/login-history` / `GET /api/auth/admin/security-logs` —
    paginated, cross-user, each entry now carries `userId`.
  - `PATCH /api/auth/admin/users/{id}/enabled?enabled=` — disables → ends all
    sessions + revokes all refresh tokens; self-change blocked.
  - `PATCH /api/auth/admin/users/{id}/role?role=ROLE_ADMIN|ROLE_USER` —
    self-change blocked; security-logged.
- `AdminGuard`, `AdminAccessDeniedException`, `AccountDisabledException`, and
  403 handlers added.

### 3. User Service

- `POST /api/users` — idempotent profile provisioning (email → username match),
  admin-guarded (the Auth Service Feign client carries the admin role).
- `GET /api/users/admin/summary` — totals, active/disabled, admins, new (7 d).
- `GET /api/users/admin?page&size&query` — paged, filterable user list.
- Repository: paginated `searchPage`, `countByEnabledFalse`, `countByRole`,
  `countByCreatedAtAfter`; the pre-existing `search` query was hardened with
  `COALESCE` for null `displayName`.

### 4. File Service

- `GET /api/files/admin/storage-overview` — platform totals (distinct owners,
  files, bytes, trash), largest files, file-type breakdown, weekly/monthly
  usage across all users.
- `GET /api/files/admin/audit-logs?page&size&action&userId` — cross-user audit
  trail; `AuditLogResponse` now includes `ownerId`.
- `GET /api/files/admin/minio-status` — live MinIO probe (reachable, bucket
  exists, endpoint, bucket). Never throws.
- `MinioService.status()`, `AdminGuard`, 403 handler.

### 5. Frontend — `/admin` dashboard

- `AdminRoute` — waits for profile hydration after sign-in (no false bounce),
  redirects non-admins to the dashboard.
- **Sidebar** — an *Administration* section appears only for admins.
- **AdminPage** — six animated tabs:
  - **Overview** — user / security / storage stat cards plus services-health.
  - **Users** — search + paginated table, promote/demote admin, enable/disable.
  - **Storage** — platform totals, file-type bars, weekly/monthly uploads,
    largest files.
  - **Audit** — cross-user audit with action chips + user-id filter.
  - **Security** — login history + security log across all users.
  - **System** — per-microservice health (UP/DOWN/UNKNOWN, instances, endpoint)
    and MinIO status card.
- `utils/role.ts` — `isAdminRole` / `normalizeRole` accept both `ROLE_ADMIN`
  and bare `ADMIN` (ProfilePage's admin badge now uses it too).
- New: `types/admin.types.ts`, `services/admin.service.ts`,
  `hooks/useAdmin.ts`, `constants/apiEndpoints.ts` admin section.

---

## New endpoints

| Method | Path | Service | Notes |
|---|---|---|---|
| GET | `/api/admin/system/health` | gateway | Eureka + actuator aggregation |
| GET | `/api/users/admin/summary` | user | admin |
| GET | `/api/users/admin` | user | paged, admin |
| POST | `/api/users` | user | provisioning, admin/internal |
| GET | `/api/files/admin/storage-overview` | file | admin |
| GET | `/api/files/admin/audit-logs` | file | admin |
| GET | `/api/files/admin/minio-status` | file | admin |
| GET | `/api/auth/admin/security-overview` | auth | admin |
| GET | `/api/auth/admin/login-history` | auth | admin |
| GET | `/api/auth/admin/security-logs` | auth | admin |
| PATCH | `/api/auth/admin/users/{id}/enabled` | auth | admin |
| PATCH | `/api/auth/admin/users/{id}/role` | auth | admin |

## Database changes

None — all aggregates are computed with repository queries over existing tables
(`users`, `file_metadata`, `audit_logs`, `login_history`, `security_logs`,
`user_sessions`, `trusted_devices`).

## Security improvements

- Gateway strips + re-derives `X-User-Id` / `X-User-Role` (spoofing eliminated).
- Every admin endpoint is role-guarded server-side; the role header is now
  trustworthy.
- Disabled accounts are blocked at login and signed out everywhere.
- Admins cannot disable or demote themselves.
- Provisioning never blocks auth (best-effort, self-healing on sign-in).

## Validation

- `mvn compile` clean on gateway, auth, user, file services.
- Gateway test suite: **31 tests, 0 failures** — including a new spoofing
  regression test asserting JWT-derived headers replace caller-supplied ones.
- Frontend `tsc -b`, production build, and ESLint all clean.
- `qa-e2e.sh` +11 admin scenarios (admin OTP login, profile sync, all admin
  views, non-admin 403s, header spoofing, disabled-account block, self-demote
  guard, promote/demote round-trip).

## Future enhancements

- Storage quota enforcement per user; per-user storage cards in the Users tab.
- Admin user search by email/username on the backend page directly.
- Account-lock sweep job (auto-unlock after 15 min) surfaced on the dashboard.
- Export admin views (CSV) and role-change audit filtering.
