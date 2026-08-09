import type { AxiosRequestConfig } from 'axios';

import { apiClient } from '@/api/axios';
import { API_ENDPOINTS } from '@/constants/apiEndpoints';
import type {
  ApiResponse,
  AuthResponse,
  ChangePasswordRequest,
  ForgotPasswordRequest,
  LoginHistoryEntry,
  LoginRequest,
  LoginResponse,
  OtpDispatchResponse,
  PaginatedResponse,
  RegisterRequest,
  RegisterResponse,
  ResetPasswordRequest,
  ResetTokenResponse,
  ResendOtpRequest,
  DisableTwoFactorRequest,
  EnableTwoFactorRequest,
  EnableTwoFactorResponse,
  PasskeyAuthenticationFinishRequest,
  PasskeyAuthenticationStart,
  PasskeyCredentialInfo,
  PasskeyRegistrationFinishRequest,
  PasskeyRegistrationStart,
  RegenerateBackupCodesResponse,
  SecurityLogEntry,
  SecurityOverview,
  SessionInfo,
  TrustedDeviceInfo,
  TwoFactorLoginRequest,
  TwoFactorSetup,
  TwoFactorStatus,
  UserProfile,
  VerifyOtpRequest,
} from '@/types';

/**
 * Auth service (auth-service).
 *
 * `skipAuthRedirect` + `silent` keep the global interceptor out of the way:
 * a 401 on bad credentials is expected, and the auth pages surface errors
 * themselves (via the login/register mutations).
 */
export const authService = {
  // ── Registration (email OTP activation) ────────────────────────────────
  register: (payload: RegisterRequest) =>
    apiClient.post<ApiResponse<RegisterResponse>>(API_ENDPOINTS.auth.register, payload, {
      skipAuthRedirect: true,
      silent: true,
    }),

  verifyRegistration: (payload: VerifyOtpRequest, rememberDevice = false) =>
    apiClient.post<ApiResponse<AuthResponse>>(
      API_ENDPOINTS.auth.verifyRegistration,
      payload,
      { skipAuthRedirect: true, silent: true, params: { rememberDevice } },
    ),

  // ── Login ───────────────────────────────────────────────────────────────
  login: (payload: LoginRequest) =>
    apiClient.post<ApiResponse<LoginResponse>>(API_ENDPOINTS.auth.login, payload, {
      skipAuthRedirect: true,
      silent: true,
    }),

  verifyLogin: (payload: VerifyOtpRequest, rememberDevice = false) =>
    apiClient.post<ApiResponse<AuthResponse>>(
      API_ENDPOINTS.auth.verifyLogin,
      payload,
      { skipAuthRedirect: true, silent: true, params: { rememberDevice } },
    ),

  /** Completes a sign-in blocked on the 2FA step (TOTP or backup code). */
  verifyTwoFactorLogin: (payload: TwoFactorLoginRequest, rememberDevice = false) =>
    apiClient.post<ApiResponse<AuthResponse>>(
      API_ENDPOINTS.auth.verifyTwoFactorLogin,
      payload,
      { skipAuthRedirect: true, silent: true, params: { rememberDevice } },
    ),

  resendOtp: (payload: ResendOtpRequest) =>
    apiClient.post<ApiResponse<OtpDispatchResponse>>(API_ENDPOINTS.auth.resendOtp, payload, {
      skipAuthRedirect: true,
      silent: true,
    }),

  // ── Forgot password ────────────────────────────────────────────────────
  forgotPassword: (payload: ForgotPasswordRequest) =>
    apiClient.post<ApiResponse<OtpDispatchResponse>>(API_ENDPOINTS.auth.forgotPassword, payload, {
      skipAuthRedirect: true,
      silent: true,
    }),

  verifyForgotPassword: (payload: VerifyOtpRequest) =>
    apiClient.post<ApiResponse<ResetTokenResponse>>(
      API_ENDPOINTS.auth.verifyForgotPassword,
      payload,
      { skipAuthRedirect: true, silent: true },
    ),

  resetPassword: (payload: ResetPasswordRequest) =>
    apiClient.post<ApiResponse<null>>(API_ENDPOINTS.auth.resetPassword, payload, {
      skipAuthRedirect: true,
      silent: true,
    }),

  // ── Session / device management ────────────────────────────────────────
  logout: (refreshToken?: string) =>
    apiClient.post<ApiResponse<null>>(
      API_ENDPOINTS.auth.logout,
      refreshToken ? { refreshToken } : undefined,
    ),

  logoutAll: () => apiClient.post<ApiResponse<null>>(API_ENDPOINTS.auth.logoutAll),

  getSessions: () => apiClient.get<ApiResponse<SessionInfo[]>>(API_ENDPOINTS.auth.sessions),

  endSession: (sessionId: string) =>
    apiClient.delete<ApiResponse<null>>(API_ENDPOINTS.auth.session(sessionId)),

  getTrustedDevices: () =>
    apiClient.get<ApiResponse<TrustedDeviceInfo[]>>(API_ENDPOINTS.auth.trustedDevices),

  removeTrustedDevice: (id: number) =>
    apiClient.delete<ApiResponse<null>>(API_ENDPOINTS.auth.trustedDevice(id)),

  getLoginHistory: (page = 0, size = 20) =>
    apiClient.get<ApiResponse<PaginatedResponse<LoginHistoryEntry>>>(
      API_ENDPOINTS.auth.loginHistory,
      { params: { page, size } },
    ),

  getSecurityLogs: (page = 0, size = 20) =>
    apiClient.get<ApiResponse<PaginatedResponse<SecurityLogEntry>>>(
      API_ENDPOINTS.auth.securityLogs,
      { params: { page, size } },
    ),

  getSecurityOverview: () =>
    apiClient.get<ApiResponse<SecurityOverview>>(API_ENDPOINTS.auth.securityOverview),

  // ── Phase 6: two-factor authentication (TOTP) ────────────────────────────
  getTwoFactorStatus: () =>
    apiClient.get<ApiResponse<TwoFactorStatus>>(API_ENDPOINTS.auth.twoFactor.status),

  twoFactorSetup: () =>
    apiClient.post<ApiResponse<TwoFactorSetup>>(API_ENDPOINTS.auth.twoFactor.setup),

  enableTwoFactor: (payload: EnableTwoFactorRequest) =>
    apiClient.post<ApiResponse<EnableTwoFactorResponse>>(
      API_ENDPOINTS.auth.twoFactor.enable,
      payload,
    ),

  disableTwoFactor: (payload: DisableTwoFactorRequest) =>
    apiClient.post<ApiResponse<null>>(API_ENDPOINTS.auth.twoFactor.disable, payload),

  regenerateBackupCodes: () =>
    apiClient.post<ApiResponse<RegenerateBackupCodesResponse>>(
      API_ENDPOINTS.auth.twoFactor.regenerateBackupCodes,
    ),

  // ── Phase 6: passkeys (WebAuthn) ────────────────────────────────────────
  listPasskeys: () =>
    apiClient.get<ApiResponse<PasskeyCredentialInfo[]>>(API_ENDPOINTS.auth.passkeys.list),

  passkeyRegisterStart: () =>
    apiClient.post<ApiResponse<PasskeyRegistrationStart>>(
      API_ENDPOINTS.auth.passkeys.registerStart,
    ),

  passkeyRegisterFinish: (payload: PasskeyRegistrationFinishRequest) =>
    apiClient.post<ApiResponse<PasskeyCredentialInfo>>(
      API_ENDPOINTS.auth.passkeys.registerFinish,
      payload,
    ),

  removePasskey: (id: string) =>
    apiClient.delete<ApiResponse<null>>(API_ENDPOINTS.auth.passkeys.remove(id)),

  passkeyAuthenticateStart: () =>
    apiClient.post<ApiResponse<PasskeyAuthenticationStart>>(
      API_ENDPOINTS.auth.passkeys.authenticateStart,
      undefined,
      { skipAuthRedirect: true, silent: true },
    ),

  passkeyAuthenticateFinish: (payload: PasskeyAuthenticationFinishRequest) =>
    apiClient.post<ApiResponse<AuthResponse>>(
      API_ENDPOINTS.auth.passkeys.authenticateFinish,
      payload,
      { skipAuthRedirect: true, silent: true },
    ),

  // ── Legacy / profile ───────────────────────────────────────────────────

  /**
   * Hydrates the signed-in user's profile from the user-service
   * (`GET /api/users/me`). The auth-service has no `/me` endpoint.
   */
  getMe: (config?: AxiosRequestConfig) =>
    apiClient.get<ApiResponse<UserProfile>>(API_ENDPOINTS.users.profile, config),

  /** Changes the authenticated user's password (auth-service). */
  changePassword: (payload: ChangePasswordRequest) =>
    apiClient.put<ApiResponse<null>>(API_ENDPOINTS.auth.changePassword, payload),
};
