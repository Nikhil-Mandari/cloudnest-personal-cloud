/**
 * Share types (share-service).
 *
 * The share-service resolves a recipient (by user ID or email) and returns a
 * `ShareResponse` containing a public `shareToken`. Public access is available
 * at `GET /shares/public/{shareToken}` without authentication.
 */

export type SharePermission = 'VIEW' | 'EDIT';

export type ShareResourceType = 'FILE' | 'FOLDER';

export interface ShareRecord {
  id: number;
  resourceId: string;
  resourceType: ShareResourceType;
  ownerId: number;
  sharedWithUserId: number | null;
  permission: SharePermission;
  shareToken: string;
  isPublic: boolean;
  expiryDate: string | null;
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
}
