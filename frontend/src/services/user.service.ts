import { apiClient } from '@/api/axios';
import { API_ENDPOINTS } from '@/constants/apiEndpoints';
import type { ApiResponse, UpdateProfileRequest, UserProfile } from '@/types';

/** User service (user-service). */
export const userService = {
  getProfile: () => apiClient.get<ApiResponse<UserProfile>>(API_ENDPOINTS.users.profile),

  updateProfile: (payload: UpdateProfileRequest) =>
    apiClient.put<ApiResponse<UserProfile>>(API_ENDPOINTS.users.profile, payload),

  /** Permanently deletes the authenticated user's account. */
  deleteAccount: () => apiClient.delete<ApiResponse<null>>(API_ENDPOINTS.users.profile),
};
