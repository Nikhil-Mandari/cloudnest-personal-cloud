# 🔐 CloudNest — Phase 1: Enterprise Authentication & Security

> **Status:** Implemented ✅ · **Scope:** Email-OTP flows, JWT refresh/rotation, sessions, devices, login history, security logs, account lock, email alerts, Security & About pages
> **Backend:** auth-service · **Frontend:** React · **Database:** auth_db migration

---

## 1. New features added

| Feature | Details |
|---|---|
| **Email OTP registration** | Account created `PENDING_VERIFICATION` → 6-digit OTP emailed → verify → activate + auto sign-in. Resend with 60s cooldown, 5 min expiry, 5 max attempts. |
| **OTP login** | Password verified first → OTP emailed → `POST /login/verify` completes sign-in. `rememberDevice` marks the device trusted. |
| **Trusted devices** | Trusted devices skip the OTP step (`auth.security.skip-otp-on-trusted-device`, default on). Manage/remove from the Security page. |
| **Forgot password** | `email → OTP → verify → reset`. Generic response hides account existence. Reset ends all sessions and revokes all refresh tokens. |
| **Account lock** | 5 consecutive failed password attempts (login **or** change-password) → locked for 15 min (HTTP 423), email alert sent. |
| **JWT refresh / rotation / revocation** | 15-min access tokens + 30-day rotating refresh tokens (SHA-256 hashed in DB). Logout / logout-all / password change / reset revoke immediately. |
| **Active sessions** | Per-device sessions; list, "end session", "log out all devices". |
| **Login history & security logs** | Every success/failure recorded with IP, browser, OS, device, location; paginated endpoints. |
| **Unknown-device detection** | New-device sign-ins trigger a security log entry + alert email. |
| **Email notifications** | Welcome, OTP, password-reset confirmation, password-changed, new-login, unknown-device, account-locked (Gmail SMTP via env vars; console fallback in dev). |
| **Security page** | Scorecard (0–100), account protection checklist, active sessions, trusted devices, login history, security log. |
| **About page** | Versions, tech stack, GitHub, license, live gateway/microservice status probe. |
| **Swagger** | Added springdoc OpenAPI to the auth-service (JWT bearer scheme). |

## 2. New APIs (auth-service, via gateway)

**Public**
- `POST /api/auth/register` → account + OTP (no token)
- `POST /api/auth/register/verify` → activate + token pair
- `POST /api/auth/login` → `{ requiresOtp, challengeToken }` or full pair
- `POST /api/auth/login/verify` → complete OTP login
- `POST /api/auth/otp/resend`
- `POST /api/auth/forgot-password` · `POST /api/auth/forgot-password/verify` · `POST /api/auth/forgot-password/reset`
- `POST /api/auth/refresh` → rotated pair

**Protected**
- `POST /api/auth/logout` · `POST /api/auth/logout-all`
- `GET /api/auth/sessions` · `DELETE /api/auth/sessions/{sessionId}`
- `GET /api/auth/trusted-devices` · `DELETE /api/auth/trusted-devices/{id}`
- `GET /api/auth/login-history` · `GET /api/auth/security-logs` · `GET /api/auth/security-overview`

## 3. Database changes (`database/auth-service-enterprise-auth.sql`, idempotent)

New tables: `otp_verifications`, `refresh_tokens`, `trusted_devices`, `user_sessions`, `login_history`, `security_logs`.
New `user_credentials` columns: `status` (PENDING_VERIFICATION/ACTIVE/LOCKED), `failed_attempts`, `locked_until`, `email_verified_at`, `last_login_at`, `password_changed_at`. Existing rows default to `ACTIVE`.

> ⚠️ Run once against `auth_db`: `mysql -u root -p auth_db < database/auth-service-enterprise-auth.sql`

## 4. Files changed / added

- **auth-service (new):** entities ×6, repositories ×6, `OtpService`, `RefreshTokenService`, `SessionService`, `TrustedDeviceService`, `AccountLockService`, `SecurityEventService`, `EmailService`, `MailConfig`, `AuthProperties`, `MailProperties`, `OpenApiConfig`, `DeviceInfoParser`, `DeviceInfo`, `ClientInfo`, `IpUtils`, `LocationResolver`, `Hashing`, DTOs ×15, exceptions ×9, tests ×4.
- **auth-service (modified):** `AuthController`, `AuthService` + `AuthServiceImpl` (rewritten), `JwtProvider` (refresh/challenge tokens + jti/sid), `UserCredential`, `SecurityConstants`, `GlobalExceptionHandler`, `pom.xml` (+mail, +springdoc), `AuthServiceApplication` (`@ConfigurationPropertiesScan`).
- **gateway:** `AuthenticationFilter` public paths + `AuthenticationFilterTest`.
- **config-repo:** `auth-service.yml` (auth.* + mail.*).
- **frontend:** `api/axios.ts` (silent refresh-on-401 + retry-once), `authStore` (refresh token + device id), `services/auth.service.ts`, `useAuthMutations`, new `useSecurity`, `utils/device`, pages `VerifyOtpPage`, `ForgotPasswordPage`, `SecurityPage`, `AboutPage`, updated `LoginPage`/`RegisterPage`, routes, sidebar, types.
- **qa-e2e.sh:** rewritten auth section for the OTP flow + account-lock scenario.

## 5. Security improvements

- OTP codes stored as HMAC-SHA256 (peppered), never plaintext; constant-time comparison.
- Refresh tokens stored hashed; atomic CAS revocation prevents concurrent-reuse double rotation.
- Unknown users / locked accounts get no info leakage; forgot-password hides account existence.
- Email delivery never blocks auth flows (fail-open with logging).
- New brute-force protection on change-password (previously an un-throttled oracle).
- Account lock re-checked at OTP completion to prevent lock bypass.

## 6. Setup for real email (Gmail)

```bash
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=you@gmail.com        # full Gmail address
MAIL_PASSWORD=<16-char app password>
MAIL_FROM="CloudNest <you@gmail.com>"
AUTH_OTP_PEPPER=<random long string>   # override the dev default
```

## 7. Known limitations / future work

- Emails are sent inside the request transaction (acceptable dev tradeoff; an outbox / `AFTER_COMMIT` listener is the production upgrade).
- No per-email daily OTP cap beyond the 60s cooldown; no refresh-token-reuse alert email yet.
- Concurrency counters (OTP attempts, lock counter) rely on row-level updates; `@Version` is the further hardening.
- TOTP 2FA, WebAuthn passkeys, social login, admin dashboard → Phase 3.
- File versioning, duplicate detection, analytics, upload queue, ZIP bulk download, audit logs → Phase 2.
