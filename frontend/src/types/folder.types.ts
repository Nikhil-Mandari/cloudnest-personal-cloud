/**
 * Folder types (folder-service).
 */

export interface Folder {
  id: string;
  name: string;
  parentId?: string | null;
  ownerId: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFolderRequest {
  name: string;
  parentId?: string;
}
