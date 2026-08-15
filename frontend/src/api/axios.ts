import axios, { type AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import { toast } from 'react-toastify';

import { API_BASE_URL, REQUEST_TIMEOUT_MS } from '@/constants/app';
import { APP_ROUTES } from '@/constants/routes';
import { useAuthStore } from '@/store/authStore';
import type { ApiErrorResponse, ApiResponse, AuthResponse } from '@/types';
import { getErrorMessage } from '@/utils/error';

declare module 'axios' {
  export interface AxiosRequestConfig {
    /**
     * Set to `true` for requests where a 401 is expected (e.g. login with bad
     * credentials) so the interceptor does not force a logout + redirect.
     */
    skipAuthRedirect?: boolean;
    /**
     * Set to `true` to suppress the global error toasts (e.g. when the caller
     * renders its own inline error). The error is still rejected to the caller.
     */
    silent?: boolean;
    /**
     * Internal: set on a request that was already replayed after a token
     * refresh, so a persistent 401 cannot loop the refresh logic forever.
     */
    _retried?: boolean;
  }
}

/**
 * Shared Axios client pointing at the CloudNest API Gateway.
 *
 * - Request interceptor: attaches the JWT (`Authorization: Bearer …`) and the
 *   stable device id (`X-Device-Id`) used by the OTP / trusted-device flows.
 * - Response interceptor: on 401 it silently rotates the refresh token
 *   (single-flight) and replays the original request; only when refresh fails
 *   does it force a logout + redirect to the login page.
 */
export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: REQUEST_TIMEOUT_MS,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const { token, deviceId } = useAuthStore.getState();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  config.headers['X-Device-Id'] = deviceId;
  return config;
});

// ── Refresh-token rotation (single-flight, with request replay) ────────────
let refreshInFlight: Promise<string | null> | null = null;

/**
 * Rotates the refresh token exactly once, even when several requests 401 at
 * the same time. Returns the new access token, or null when the session can
 * no longer be recovered.
 */
async function rotateRefreshToken(): Promise<string | null> {
  const refreshToken = useAuthStore.getState().refreshToken;
  if (!refreshToken) {
    return null;
  }

  try {
    const { data } = await axios.post<ApiResponse<AuthResponse>>(
      `${API_BASE_URL}/auth/refresh`,
      { refreshToken },
      { headers: { 'Content-Type': 'application/json' } },
    );
    const auth = data.data;
    useAuthStore.getState().setTokenPair(auth.token, auth.refreshToken ?? null);
    return auth.token;
  } catch {
    useAuthStore.getState().logout();
    return null;
  }
}

function forceLogout(message: string) {
  useAuthStore.getState().logout();
  if (window.location.pathname !== APP_ROUTES.login) {
    toast.error(message);
    window.location.href = APP_ROUTES.login;
  }
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorResponse>) => {
    const { response, config } = error;
    const skipAuthRedirect = config?.skipAuthRedirect ?? false;
    const silent = config?.silent ?? false;
    const message = getErrorMessage(error);

    if (!response) {
      if (!silent) {
        toast.error('Unable to reach the server. Please check your connection.');
      }
      return Promise.reject(error);
    }

    // ── 401 → try a silent refresh, then replay the original request ──────
    if (response.status === 401 && !skipAuthRedirect && config) {
      // Never try to refresh a refresh call, and never retry more than once.
      const isRefreshCall = String(config.url ?? '').includes('/auth/refresh');
      const alreadyRetried = config._retried === true;
      if (!isRefreshCall && !alreadyRetried) {
        refreshInFlight ??= rotateRefreshToken();
        const newToken = await refreshInFlight;
        refreshInFlight = null;

        if (newToken) {
          // Replay the failed request with the fresh token (single attempt).
          return apiClient({
            ...config,
            _retried: true,
            headers: {
              ...config.headers,
              Authorization: `Bearer ${newToken}`,
            },
          });
        }
      }
      forceLogout(message || 'Your session has expired. Please sign in again.');
      return Promise.reject(error);
    }

    switch (response.status) {
      case 403:
        if (!silent) {
          toast.error(message || 'You do not have permission to perform this action.');
        }
        break;

      case 500:
        if (!silent) {
          toast.error(message || 'Something went wrong on our end. Please try again.');
        }
        break;
    }

    return Promise.reject(error);
  },
);
