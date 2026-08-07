# Phase 6 — Two-Factor Authentication (TOTP) & WebAuthn Passkeys

Status: **Implemented & validated** (auth-service + notification-service compile; frontend `tsc` + `eslint` clean; two code-review passes applied).

---

## What was added

### 1. TOTP two-factor authentication

| Flow | Endpoint | Notes |
| --- | --- | --- |
| Status | `GET /api/auth/2fa/status` | `enabled` + unused backup codes |
| Setup | `POST /api/auth/2fa/setup` | Idempotent; returns base32 secret + `otpauth://` URI (QR payload) |
| Enable | `POST /api/auth/2fa/enable` | Requires a **live authenticator code** before flipping the flag; returns 10 single-use backup codes (plaintext, exactly once) |
| Disable | `POST /api/auth/2fa/disable` | Proof required: TOTP code, unused backup code **or** account password |
| Regenerate | `POST /api/auth/2fa/backup-codes/regenerate` | Invalidates old codes, issues a fresh set |

- **Works with Google Authenticator, Microsoft Authenticator, Authy** — RFC 6238, HMAC-SHA1, 30 s steps, 6 digits (`auth.two-factor.*` configurable).
- **Replay protection**: every accepted TOTP code records its time-step counter (`two_factor_settings.last_used_counter`); a code from an equal-or-earlier step is rejected, so a captured code cannot be replayed inside its validity window.
- **Backup codes at rest**: only SHA-256 hashes are stored; plaintext is shown once at generation (`XXXXX-XXXXX`, unambiguous alphabet). Each is consumed on first use.
- **Login integration**: when 2FA is enabled, `POST /api/auth/login` returns `requires2fa: true` + a `2FA_LOGIN` challenge token (before any OTP / trusted-device shortcut). `POST /api/auth/login/2fa` completes the sign-in.
- **Security events**: enable/disable/verify/regenerate write security-log entries, send transactional emails (dev-mode console logging) and create in-app notifications (`TWO_FACTOR_ENABLED`, `TWO_FACTOR_DISABLED`, `BACKUP_CODES_REGENERATED`).
- **Security score**: enabling 2FA adds +20 to the scorecard (previously impossible to reach 100).

### 2. WebAuthn passkeys (Face ID / Touch ID / Windows Hello / security keys)

| Flow | Endpoint | Auth |
| --- | --- | --- |
| Register start | `POST /api/auth/passkeys/register/start` | JWT |
| Register finish | `POST /api/auth/passkeys/register/finish` | JWT |
| List | `GET /api/auth/passkeys` | JWT |
| Remove | `DELETE /api/auth/passkeys/{id}` | JWT |
| Sign-in start | `POST /api/auth/passkeys/authenticate/start` | Public |
| Sign-in finish | `POST /api/auth/passkeys/authenticate/finish` | Public |

- Powered by **Yubico `webauthn-server-core` 2.5.2** (fully offline, no external WebAuthn service).
- **Discoverable (resident) credentials** — sign-in needs no username: the browser offers every passkey for the relying party, one tap verifies both factors.
- **Stateless ceremonies**: the serialized creation options / assertion request are echoed back from the browser on finish; no server-side session state.
- The user handle is the UTF-8 encoding of the CloudNest user id; `PasskeyCredentialRepository` implements the WebAuthn `CredentialRepository` contract (`lookup` / `lookupAll` / `getUsernameForUserHandle`).
- **Signature counters** are persisted after each assertion (the relying party rejects counter regression → cloned-authenticator detection).
- A successful passkey assertion **satisfies both factors** (possession + biometric), so passkey sign-ins skip the emailed OTP and TOTP steps.
- Passkey register/remove raise security-log entries + in-app notifications (`PASSKEY_REGISTERED`, `PASSKEY_REMOVED`).

### 3. Frontend

- **Login page**: "Sign in with a passkey" button (browser WebAuthn support-gated) — `navigator.credentials.get` → finish → JWT session.
- **Verify OTP page**: new *2FA* mode (authenticator code or backup code; no resend/countdown; backup-code hint).
- **Security page**: new **"2FA & passkeys"** tab:
  - `TwoFactorPanel` — status card, QR-code setup (`qrcode.react`), secret copy, enable → backup-codes modal (shown exactly once), regenerate, disable with verification.
  - `PasskeysPanel` — list with transport chips (platform/USB/NFC/hybrid), register via `navigator.credentials.create`, remove.
  - Overview banner now reflects the real `twoFactorEnabled` state (score pills, protection rows).
- **Notification center**: icons + "Security" filter category for the 5 new types.

## Database changes

`database/auth-service-2fa-passkeys.sql` (run against `auth_db` **before** starting auth-service — `ddl-auto: validate`):

- `two_factor_settings` — user, secret, enabled, enabled_at, `last_used_counter` (replay protection).
- `backup_codes` — user, SHA-256 `code_hash`, used flag/timestamp.
- `passkey_credentials` — credential id (base64url), user handle, COSE public key, signature counter, transports, nickname.
- Includes an idempotent `ALTER TABLE` backfill for databases that applied an earlier revision without `last_used_counter`.

## New/changed files (backend)

- `auth-service` — `TwoFactorService`, `PasskeyCredentialService`, `TwoFactorController`, `PasskeyController`, `TotpUtils` (RFC 6238 + base32, counter-finding for replay checks), `WebAuthnConfig` (RelyingParty bean), `ClientInfoFactory`, entities (`TwoFactorSetting`, `BackupCode`, `PasskeyCredential`), repositories, 15 DTOs, `AuthService` (+`verifyTwoFactorLogin`, `completePasskeyLogin`), `AuthServiceImpl` (2FA-first login branch, passkey login, overview/score wiring), `AuthController` (`POST /login/2fa`), `SecurityConstants` (new public paths), `NotificationServiceClient` (+5 types), `EmailService` (2FA enable/disable emails), `pom.xml` (+`webauthn-server-core`).
- `notification-service` — `Notification.NotificationType` (+5 values).
- `config-repo/auth-service.yml` — `auth.two-factor.*`, `auth.webauthn.*`.

## New/changed files (frontend)

`types/auth.types.ts`, `types/notification.types.ts`, `types/index.ts`, `constants/apiEndpoints.ts`, `services/auth.service.ts`, `hooks/useAuthMutations.ts`, `hooks/useSecurity.ts`, `pages/auth/LoginPage.tsx`, `pages/auth/VerifyOtpPage.tsx`, `pages/security/SecurityPage.tsx`, `components/security/MfaPanels.tsx` (new), `components/notifications/NotificationIcon.tsx`, `pages/notifications/NotificationsPage.tsx`, `utils/passkeys.ts` (new), `package.json` (+`qrcode.react`).

## Security improvements

- Replay-protected TOTP verification (time-step counter monotonicity).
- Backup codes hashed at rest, single-use, shown exactly once.
- 2FA disable requires proof (code / backup / password).
- WebAuthn signature-counter regression rejection (clone detection).
- 2FA precedes every password sign-in (trusted-device OTP skip never bypasses it).
- All 2FA/passkey events produce security-log entries + email + in-app notifications.
- Challenge tokens are purpose-bound (`2FA_LOGIN`) and short-lived.

## Validation

- `mvn compile` — auth-service ✅, notification-service ✅.
- `npx tsc -b` + `npx eslint src --max-warnings=0` ✅.
- `qa-e2e.sh` — 14 new Phase 6 scenarios (setup → enable with live TOTP via python3 → `requires2fa` login → 2FA login → wrong-code rejection → notifications → logs → disable; passkey ceremony starts + list).
- Code review (deepseek-flash) — findings all addressed (regenerate-codes modal bug, DTO doc mismatch, DDL migration backfill, shared `isPasskeySupported` util, security-log labels).

## Notes / limitations

- Full WebAuthn register/finish + authenticate/finish ceremonies require a real authenticator and are exercised through the browser UI (curl cannot complete a ceremony).
- Passkey sign-in currently runs discovery-less (username field accepted for API forward-compatibility, not yet used for scoping).
- Repeat 2FA login failures are not yet lockout-bounded (TOTP window + challenge expiry limit abuse).
