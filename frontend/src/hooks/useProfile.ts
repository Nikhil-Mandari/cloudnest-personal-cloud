import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';

import { authService } from '@/services/auth.service';
import { userService } from '@/services/user.service';
import { useAuthStore } from '@/store/authStore';
import type { ChangePasswordRequest, UpdateProfileRequest } from '@/types';
import { getErrorMessage } from '@/utils/error';

export const PROFILE_QUERY_KEY = ['profile'] as const;

/** Fetches the authenticated user's full profile (`GET /users/me`). */
export function useProfileQuery() {
  return useQuery({
    queryKey: PROFILE_QUERY_KEY,
    queryFn: async () => {
      const { data } = await userService.getProfile();
      return data.data;
    },
  });
}

/** Profile mutation — updates via `PUT /users/me` and hydrates the auth store. */
export function useProfileMutations() {
  const queryClient = useQueryClient();
  const setUser = useAuthStore((state) => state.setUser);

  const updateProfile = useMutation({
    mutationFn: (payload: UpdateProfileRequest) => userService.updateProfile(payload),
    onSuccess: (response) => {
      const profile = response.data.data;
      // Keep the navbar/user-menu profile in sync.
      setUser(profile);
      queryClient.setQueryData(PROFILE_QUERY_KEY, profile);
      toast.success('Profile updated');
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to update your profile.')),
  });

  return { updateProfile };
}

/** Password mutation — verifies the current password via the auth-service. */
export function useChangePasswordMutation() {
  return useMutation({
    mutationFn: (payload: ChangePasswordRequest) => authService.changePassword(payload),
    onSuccess: () => {
      toast.success('Password updated');
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, 'Failed to change your password.')),
  });
}
