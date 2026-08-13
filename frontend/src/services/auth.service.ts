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
  SecurityLogEntry,
  SecurityOverview,
  SessionInfo,
  TwoFactorLoginRequest,
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

  // ── Profile ────────────────────────────────────────────────────────────

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
