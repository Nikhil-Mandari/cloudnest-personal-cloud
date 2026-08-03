import type { AxiosRequestConfig } from 'axios';

import { apiClient } from '@/api/axios';
import { API_ENDPOINTS } from '@/constants/apiEndpoints';
import type { ApiResponse, AuthResponse, LoginRequest, RegisterRequest, User } from '@/types';

/**
 * Auth service (auth-service).
 *
 * `skipAuthRedirect` + `silent` keep the global interceptor out of the way:
 * a 401 on bad credentials is expected, and the auth pages surface errors
 * themselves (via the login/register mutations).
 */
export const authService = {
  login: (payload: LoginRequest) =>
    apiClient.post<ApiResponse<AuthResponse>>(API_ENDPOINTS.auth.login, payload, {
      skipAuthRedirect: true,
      silent: true,
    }),

  register: (payload: RegisterRequest) =>
    apiClient.post<ApiResponse<null>>(API_ENDPOINTS.auth.register, payload, {
      skipAuthRedirect: true,
      silent: true,
    }),

  getMe: (config?: AxiosRequestConfig) =>
    apiClient.get<ApiResponse<User>>(API_ENDPOINTS.auth.me, config),
};
