/**
 * User profile types (user-service).
 */

import type { User } from './auth.types';

/**
 * Full profile returned by the user-service. Alias of the auth `User` shape —
 * kept as a distinct name so the profile domain can grow independently.
 */
export type UserProfile = User;

/**
 * Partial profile update payload — every field is optional; only provided
 * fields are updated (mirrors the backend `UpdateProfileRequest`).
 */
export interface UpdateProfileRequest {
  displayName?: string;
  email?: string;
  avatarUrl?: string;
  bio?: string;
  phone?: string;
}



/**
 * Change-password payload. NOTE: the backend does not expose this endpoint
 * yet — the type is kept for when it lands.
 */
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
