import axios, { type AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import { toast } from 'react-toastify';

import { API_BASE_URL, REQUEST_TIMEOUT_MS } from '@/constants/app';
import { APP_ROUTES } from '@/constants/routes';
import { useAuthStore } from '@/store/authStore';
import type { ApiErrorResponse } from '@/types';
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
  }
}

/**
 * Shared Axios client pointing at the CloudNest API Gateway.
 *
 * - Request interceptor: attaches the JWT (`Authorization: Bearer …`).
 * - Response interceptor: global handling of 401 / 403 / 500 and network
 *   errors, with toast notifications. Errors are still re-thrown so callers
 *   can handle them locally too.
 */
export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: REQUEST_TIMEOUT_MS,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    const { response, config } = error;
    const skipAuthRedirect = config?.skipAuthRedirect ?? false;
    const silent = config?.silent ?? false;
    const message = getErrorMessage(error);

    if (response) {
      switch (response.status) {
        case 401:
          if (!skipAuthRedirect) {
            useAuthStore.getState().logout();
            if (window.location.pathname !== APP_ROUTES.login) {
              if (!silent) {
                toast.error(message || 'Your session has expired. Please sign in again.');
              }
              window.location.href = APP_ROUTES.login;
            }
          }
          break;

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
    } else if (!silent) {
      toast.error('Unable to reach the server. Please check your connection.');
    }

    return Promise.reject(error);
  },
);
