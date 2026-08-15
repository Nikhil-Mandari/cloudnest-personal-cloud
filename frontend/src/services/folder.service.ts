import { apiClient } from '@/api/axios';
import { API_ENDPOINTS } from '@/constants/apiEndpoints';
import type { ApiResponse, CreateFolderRequest, Folder } from '@/types';

/** Folder service (folder-service). */
export const folderService = {
  /** All active folders (flat list — used by the move dialog). */
  getFolders: () => apiClient.get<ApiResponse<Folder[]>>(API_ENDPOINTS.folders.list),

  /** Root-level folders (no parent). */
  getRootFolders: () => apiClient.get<ApiResponse<Folder[]>>(API_ENDPOINTS.folders.root),

  /** Single folder by UUID. */
  getFolder: (id: string) =>
    apiClient.get<ApiResponse<Folder>>(API_ENDPOINTS.folders.detail(id)),

  /** Immediate children of a folder. */
  getFolderChildren: (id: string) =>
    apiClient.get<ApiResponse<Folder[]>>(API_ENDPOINTS.folders.children(id)),

  createFolder: (payload: CreateFolderRequest) =>
    apiClient.post<ApiResponse<Folder>>(API_ENDPOINTS.folders.create, payload),

  renameFolder: (id: string, name: string) =>
    apiClient.put<ApiResponse<Folder>>(API_ENDPOINTS.folders.rename(id), { name }),

  deleteFolder: (id: string) =>
    apiClient.delete<ApiResponse<null>>(API_ENDPOINTS.folders.remove(id)),

  getTrashFolders: () =>
    apiClient.get<ApiResponse<Folder[]>>(API_ENDPOINTS.folders.trash),

  restoreFolder: (id: string) =>
    apiClient.patch<ApiResponse<Folder>>(API_ENDPOINTS.folders.restore(id)),

  permanentlyDeleteFolder: (id: string) =>
    apiClient.delete<ApiResponse<null>>(API_ENDPOINTS.folders.permanentRemove(id)),

  emptyTrash: () => apiClient.delete<ApiResponse<null>>(API_ENDPOINTS.folders.trash),
};
