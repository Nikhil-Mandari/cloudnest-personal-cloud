import { apiClient } from '@/api/axios';
import { API_ENDPOINTS } from '@/constants/apiEndpoints';
import type { ApiResponse, CreateFolderRequest, Folder } from '@/types';

/** Folder service (folder-service). */
export const folderService = {
  getFolders: () => apiClient.get<ApiResponse<Folder[]>>(API_ENDPOINTS.folders.list),

  createFolder: (payload: CreateFolderRequest) =>
    apiClient.post<ApiResponse<Folder>>(API_ENDPOINTS.folders.create, payload),

  renameFolder: (id: string, name: string) =>
    apiClient.put<ApiResponse<Folder>>(API_ENDPOINTS.folders.rename(id), { name }),

  deleteFolder: (id: string) =>
    apiClient.delete<ApiResponse<null>>(API_ENDPOINTS.folders.remove(id)),
};
