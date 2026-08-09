/**
 * Folder types (folder-service).
 *
 * Mirrors the backend `FolderResponse` DTO — note the parent is exposed as
 * `parentFolderId` on the wire.
 */

export interface Folder {
  id: string;
  name: string;
  /** ID of the parent folder (null = root-level folder). */
  parentFolderId?: string | null;
  ownerId: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFolderRequest {
  name: string;
  /** ID of the parent folder (omitted = root-level folder). */
  parentFolderId?: string;
}

/** Sort keys supported by the folder explorer. */
export type FolderSortKey = 'name' | 'date';
