/**
 * Authentication & user identity types (auth-service / user-service).
 */

export type UserRole = 'USER' | 'ADMIN';

/** Account lifecycle status returned by the auth-service. */
export type AccountStatus = 'PENDING_VERIFICATION' | 'ACTIVE' | 'LOCKED';

/** OTP purpose carried through the verification flow. */
export type OtpPurpose = 'registration' | 'login' | 'password-reset';

/** Purpose of a code-verification page visit (incl. the 2FA step). */
export type VerifyOtpPurpose = 'registration' | 'login' | '2fa';

/**
 * The signed-in user's profile as returned by the user-service
 * (`GET /api/users/me`). `displayName` may be missing for accounts created
 * before display names were supported.
 */
export interface User {
  id: number | string;
  username: string;
  email: string;
  displayName: string | null;
  avatarUrl?: string | null;
  bio?: string | null;
  phone?: string | null;
  role: UserRole;
  enabled?: boolean;
  createdAt: string;
  updatedAt?: string;
  /** Optional — only present once the backend tracks login timestamps. */
  lastLogin?: string | null;
}

export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
  /** When true, the device is remembered as trusted (skips OTP later). */
  rememberDevice?: boolean;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface VerifyOtpRequest {
  /** Short-lived challenge JWT (login / password-reset flows). */
  challengeToken?: string;
  /** Email (registration flow). */
  email?: string;
  code: string;
}

export interface ResendOtpRequest {
  email?: string;
  challengeToken?: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  resetToken: string;
  newPassword: string;
}

/**
 * Registration response: the account is pending verification and an OTP was
 * emailed. `devOtp` is only present when the backend runs without email
 * delivery (development mode).
 */
export interface RegisterResponse {
  userId: number;
  email: string;
  message: string;
  devOtp?: string;
  resendAfterSeconds?: number;
  otpExpiryMinutes?: number;
}

/**
 * Unified login response. When `requiresOtp` is true only `challengeToken`
 * (plus identity) is populated — complete via `verifyLogin`. When
 * `requires2fa` is true complete via `verifyTwoFactorLogin` with an
 * authenticator / backup code.
 */
export interface LoginResponse {
  requiresOtp: boolean;
  /** When true, the caller must complete the TOTP / backup-code step. */
  requires2fa?: boolean;
  challengeToken?: string;
  token?: string;
  refreshToken?: string;
  userId?: number;
  username?: string;
  email?: string;
  role?: string;
  devOtp?: string;
  resendAfterSeconds?: number;
  otpExpiryMinutes?: number;
  trustedDevice?: boolean;
}

/** OTP dispatch response (forgot-password / resend). */
export interface OtpDispatchResponse {
  sent: boolean;
  challengeToken?: string;
  devOtp?: string;
  resendAfterSeconds?: number;
  otpExpiryMinutes?: number;
}

export interface ResetTokenResponse {
  resetToken: string;
}

export interface AuthResponse {
  token: string;
  refreshToken?: string;
  userId?: number;
  username?: string;
  email?: string;
  role?: string;
  requiresOtp?: boolean;
}

/** An active sign-in session shown on the Security page. */
export interface SessionInfo {
  sessionId: string;
  deviceId: string;
  deviceName: string;
  browser: string;
  os: string;
  deviceType: 'DESKTOP' | 'TABLET' | 'MOBILE' | 'OTHER';
  ipAddress: string | null;
  location: string;
  current: boolean;
  trusted: boolean;
  loginTime: string;
  lastActive: string;
}

export interface TrustedDeviceInfo {
  id: number;
  deviceId: string;
  deviceName: string;
  browser: string;
  os: string;
  ipAddress: string | null;
  lastUsedAt: string;
  createdAt: string;
}

export interface LoginHistoryEntry {
  id: number;
  /** Owner of the attempt (present on admin views). */
  userId?: number;
  success: boolean;
  ipAddress: string | null;
  browser: string;
  os: string;
  deviceType: string;
  deviceName: string;
  location: string;
  failureReason: string | null;
  loginTime: string;
}

export interface SecurityLogEntry {
  id: number;
  /** Owner of the event (present on admin views). */
  userId?: number;
  action: string;
  details: string | null;
  ipAddress: string | null;
  browser: string;
  os: string;
  location: string;
  createdAt: string;
}

/** Aggregated security posture for the Security page. */
export interface SecurityOverview {
  securityScore: number;
  accountStatus: AccountStatus;
  emailVerified: boolean;
  twoFactorEnabled: boolean;
  passwordChangedAt: string | null;
  lastLoginAt: string | null;
  activeSessionCount: number;
  trustedDeviceCount: number;
  failedLoginsLast7Days: number;
  totalLogins: number;
}

/** React Hook Form values for the login form. */
export interface LoginFormValues {
  email: string;
  password: string;
  rememberMe?: boolean;
}

/** React Hook Form values for the registration form. */
export interface RegisterFormValues {
  fullName: string;
  email: string;
  password: string;
  confirmPassword: string;
}

/** Navigation state carried to the OTP verification page. */
export interface VerifyOtpState {
  purpose: VerifyOtpPurpose;
  email?: string;
  challengeToken?: string;
  rememberDevice?: boolean;
  devOtp?: string;
  from?: string;
  resendAfterSeconds?: number;
  otpExpiryMinutes?: number;
}

// ── Phase 6: two-factor authentication (TOTP) ──────────────────────────────

/** Current 2FA state shown on the Security page. */
export interface TwoFactorStatus {
  enabled: boolean;
  backupCodesRemaining: number;
}

/** Secret + otpauth URI returned by the setup step (QR payload). */
export interface TwoFactorSetup {
  secret: string;
  otpauthUri: string;
  accountName: string;
  issuer: string;
  digits: number;
  periodSeconds: number;
}

export interface EnableTwoFactorRequest {
  code: string;
}

/** Enabling 2FA returns the backup codes exactly once. */
export interface EnableTwoFactorResponse {
  enabled: boolean;
  backupCodes: string[];
}

export interface DisableTwoFactorRequest {
  /** TOTP code, unused backup code or account password. */
  verification: string;
}

export interface RegenerateBackupCodesResponse {
  backupCodes: string[];
}

export interface TwoFactorLoginRequest {
  challengeToken: string;
  code: string;
}

// ── Phase 6: passkeys (WebAuthn) ───────────────────────────────────────────

/** A registered passkey shown on the Security page. */
export interface PasskeyCredentialInfo {
  id: string;
  nickname: string | null;
  transports: string[];
  createdAt: string;
  lastUsedAt: string | null;
}

/** Start of a registration ceremony: creation options for the browser. */
export interface PasskeyRegistrationStart {
  optionsJson: string;
}

export interface PasskeyRegistrationFinishRequest {
  optionsJson: string;
  responseJson: string;
  nickname?: string;
}

/** Start of a passkey sign-in: assertion request echoed back on finish. */
export interface PasskeyAuthenticationStart {
  requestJson: string;
  credentialsGetJson: string;
}

export interface PasskeyAuthenticationFinishRequest {
  requestJson: string;
  responseJson: string;
}
