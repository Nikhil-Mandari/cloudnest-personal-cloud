import { apiClient } from '@/api/axios';
import { API_ENDPOINTS } from '@/constants/apiEndpoints';
import type {
  ApiResponse,
  ChangePasswordRequest,
  UpdateProfileRequest,
  UserProfile,
} from '@/types';

/** User service (user-service). */
export const userService = {
  getProfile: () => apiClient.get<ApiResponse<UserProfile>>(API_ENDPOINTS.users.profile),

  updateProfile: (payload: UpdateProfileRequest) =>
    apiClient.put<ApiResponse<UserProfile>>(API_ENDPOINTS.users.profile, payload),

  changePassword: (payload: ChangePasswordRequest) =>
    apiClient.put<ApiResponse<null>>(API_ENDPOINTS.users.changePassword, payload),
};
