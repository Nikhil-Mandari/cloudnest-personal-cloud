# Phase 5 — Security Email Notifications & Notification Center

**Scope:** Transactional security emails (new login, unknown device, password
changed/reset, account locked) plus in-app notifications that mirror those
events, and an upgraded notification center UI.

---

## What was already in place (verified, not rebuilt)

The email layer shipped with the earlier auth work and needed no rework:

| Event | Method | Trigger |
|---|---|---|
| New sign-in | `EmailService.sendNewLoginAlert` | `completeLogin` via `SecurityEventService.sendLoginAlert` |
| Unknown-device sign-in | `EmailService.sendUnknownDeviceAlert` | same path, stricter tone |
| Password changed | `EmailService.sendPasswordChangedAlert` | `changePassword` |
| Password reset | `EmailService.sendPasswordResetConfirmation` | `resetPassword` |
| Account locked | `EmailService.sendAccountLockedAlert` | 5 failed attempts (lock service) |
| Welcome / OTP | `sendWelcome` / `sendOtp` | registration flows |

Emails are best-effort (never block auth), support HTML+plain rendering, and
fall back to console logging in dev (`mail.enabled=false`, see
`backend/config-repo/auth-service.yml`).

## New in this phase

### Backend — notification-service

- **5 new `NotificationType` values** (column `length=30`, all fit):
  `LOGIN_ALERT`, `UNKNOWN_DEVICE_LOGIN`, `PASSWORD_CHANGED`,
  `PASSWORD_RESET`, `ACCOUNT_LOCKED`.
- **`DELETE /api/notifications/read-all`** — bulk "clear read" for a user
  (`NotificationRepository.deleteReadNotifications`, service + controller).
  Coexists with `PUT /read-all` (mark all read); Spring resolves the literal
  path over `DELETE /{id}`.

### Backend — auth-service

- **`NotificationServiceClient`** (OpenFeign → `notification-service`)
  with **`NotificationServiceClientConfig`** interceptor (service identity
  headers) **and 1.5 s connect / 3 s read timeouts** so an unresponsive
  notification-service can never stall login or password changes.
- **`NotificationCreateRequest`** DTO (`type` is the wire enum name).
- **`SecurityEventService`**:
  - best-effort `notify(...)` helper — failures logged, flow never blocked;
  - `sendLoginAlert(user, info, knownDevice)` — now receives the
    known/unknown verdict **computed before the session is recorded**;
  - `describeClient(...)` renders `device · browser on OS · location` for
    message bodies (null-safe);
  - `sendAccountLockedAlert` also pushes an `ACCOUNT_LOCKED` notification.
- **`AuthServiceImpl`**:
  - `completeLogin` computes `isKnownDevice` **before** creating the session
    (fixes a latent bug where the just-created session made every device look
    "known", silently disabling unknown-device detection);
  - `changePassword` / `resetPassword` push `PASSWORD_CHANGED` /
    `PASSWORD_RESET` notifications.

### Frontend — notification center

- **Type union extended** with the five security types
  (`src/types/notification.types.ts`).
- **`NotificationIcon`**: `ShieldCheck`, `ShieldAlert`, `KeyRound`,
  `RotateCcw`, `Lock` chips for the new types.
- **`NotificationsPage`**: category filters **All / Unread / Shares /
  Security / System** with live per-filter counts, plus a **Clear read**
  ghost button (shown only when read items exist) next to *Mark all as read*.
- **`notification.service` / `useNotifications`**: `clearRead` mutation
  (`DELETE /notifications/read-all`).
- Empty-state copy updated for security alerts; filter-empty copy generic.

## Files changed

**Backend — notification-service**
- `entity/Notification.java` (enum values)
- `repository/NotificationRepository.java` (delete-read query)
- `service/NotificationService.java`, `service/impl/NotificationServiceImpl.java`
- `controller/NotificationController.java`

**Backend — auth-service**
- `client/NotificationServiceClient.java` *(new)*
- `config/NotificationServiceClientConfig.java` *(new, incl. timeouts)*
- `dto/NotificationCreateRequest.java` *(new)*
- `service/SecurityEventService.java`
- `service/impl/AuthServiceImpl.java`

**Frontend**
- `types/notification.types.ts`, `constants/apiEndpoints.ts`
- `services/notification.service.ts`, `hooks/useNotifications.ts`
- `components/notifications/NotificationIcon.tsx`, `NotificationEmptyState.tsx`
- `pages/notifications/NotificationsPage.tsx`

**Tooling / docs**
- `qa-e2e.sh` (+6 scenarios: in-app password/login alerts, unknown-device
  detection from a fresh device, locked-account notification, clear-read)
- `docs/12_SECURITY_NOTIFICATIONS.md` (this report)

## New/changed APIs

| Method | Path | Notes |
|---|---|---|
| `DELETE` | `/api/notifications/read-all` | clears read notifications (X-User-Id) |
| `POST` | `/api/notifications` (internal Feign) | unchanged, now consumed by auth-service |

## Notes & future enhancements

- Share-event emails/notifications (`SHARE_RECEIVED` etc.) still lack a
  producer in share-service — a natural next step is a share→notification
  Feign client so the center shows real sharing events.
- Storage-warning emails (`STORAGE_WARNING`) can be added to file-service
  with the same notify pattern.
- 2FA (TOTP) enable/disable and passkey events can push new types without
  schema changes (enum is `VARCHAR`).
