/**
 * User profile types (user-service).
 */

import type { User } from './auth.types';

export interface UserProfile extends User {
  avatarUrl?: string | null;
  bio?: string | null;
  storageLimitBytes?: number;
  storageUsedBytes?: number;
}

export interface UpdateProfileRequest {
  fullName?: string;
  bio?: string;
  avatarUrl?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
