/**
 * Share types (share-service).
 *
 * The share-service resolves a recipient (by user ID or email) and returns a
 * `ShareResponse` containing a public `shareToken`. Public access is available
 * at `GET /shares/public/{shareToken}` without authentication.
 */

export type SharePermission = 'VIEW' | 'DOWNLOAD' | 'EDIT';

export type ShareResourceType = 'FILE' | 'FOLDER';

/** `all` = everything, otherwise only files or only folders. */
export type ShareTypeFilter = 'all' | ShareResourceType;

/** Sort keys supported by the shared-with-me explorer. */
export type ShareSortKey = 'date' | 'type';

export interface ShareRecord {
  id: number;
  resourceId: string;
  resourceType: ShareResourceType;
  /** Display name of the shared resource (omitted when unresolvable). */
  resourceName?: string | null;
  ownerId: number;
  sharedWithUserId: number | null;
  permission: SharePermission;
  shareToken: string;
  isPublic: boolean;
  expiryDate: string | null;
  /** Whether the share link is protected by a password. */
  hasPassword?: boolean;
  /** Lifetime view counter (owner analytics). */
  viewCount?: number;
  /** Lifetime download counter (owner analytics). */
  downloadCount?: number;
  /** Most recent access timestamp. */
  lastAccessedAt?: string | null;
  createdAt: string;
}

export interface CreateShareRequest {
  /** Internal file ID (Long) as used by the share-service. */
  fileId: number;
  /** Recipient email (optional if `sharedWithUserId` is provided). */
  sharedWithEmail?: string;
  /** Recipient user ID (optional if `sharedWithEmail` is provided). */
  sharedWithUserId?: number;
  permission: SharePermission;
  /** ISO date-time; omitted = never expires. */
  expiryDate?: string;
  /** Optional password protecting the share link. */
  password?: string;
}

export interface UpdateShareRequest {
  permission: SharePermission;
  /** ISO date-time; omitted = keep the current expiry. */
  expiryDate?: string | null;
  /** True removes the expiry so the link never expires. */
  clearExpiry?: boolean;
  /** New password for the link; omitted = keep the current one. */
  password?: string;
  /** True removes password protection. */
  clearPassword?: boolean;
}

/** Sort keys supported by the My Shares management view. */
export type MySharesSortKey = 'date' | 'name' | 'views' | 'downloads';

/** Owner-only access analytics for a share link. */
export interface ShareAnalytics {
  shareId: number;
  shareToken: string;
  resourceId: string;
  resourceType: ShareResourceType;
  resourceName?: string | null;
  permission: SharePermission;
  hasPassword?: boolean;
  isPublic?: boolean;
  expiryDate?: string | null;
  createdAt?: string;
  viewCount: number;
  downloadCount: number;
  lastAccessedAt?: string | null;
}

/** Password payload for verifying password-protected public shares. */
export interface VerifySharePasswordRequest {
  password: string;
}
