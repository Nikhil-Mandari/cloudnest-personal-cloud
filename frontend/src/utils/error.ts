import axios from 'axios';

import type { ApiErrorResponse } from '@/types';

/**
 * Extracts a human-readable message from an unknown error, preferring the
 * backend's `message`/`errors` fields (CloudNest standard error envelope).
 */
export function getErrorMessage(
  error: unknown,
  fallback = 'Something went wrong. Please try again.',
): string {
  if (axios.isAxiosError(error)) {
    if (error.code === 'ERR_NETWORK') {
      return 'Unable to reach the server. Please check your connection.';
    }

    const data = error.response?.data as ApiErrorResponse | undefined;

    if (data?.message) {
      return data.message;
    }
    if (Array.isArray(data?.errors) && data.errors.length > 0) {
      return data.errors.join(', ');
    }
    if (error.message) {
      return error.message;
    }
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallback;
}
