import { apiClient } from '@/api/axios';
import { API_ENDPOINTS } from '@/constants/apiEndpoints';
import type { ApiResponse, Folder } from '@/types';

/**
 * Folder service (folder-service).
 *
 * NOTE: minimal reconstruction — only the methods backed by endpoints present
 * on this branch's API map. The root / detail / children / trash / restore /
 * permanent-delete methods are intentionally deferred and restored when the
 * FoldersPage slice lands (they require `apiEndpoints.folders` keys not yet
 * available).
 */
export const folderService = {
  getFolders: () => apiClient.get<ApiResponse<Folder[]>>(API_ENDPOINTS.folders.list),
};
