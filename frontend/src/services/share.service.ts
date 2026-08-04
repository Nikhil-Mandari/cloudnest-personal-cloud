import { apiClient } from '@/api/axios';
import { API_ENDPOINTS } from '@/constants/apiEndpoints';
import type { ApiResponse, CreateShareRequest, ShareRecord } from '@/types';

/**
 * Share service (share-service).
 *
 * Sharing a file requires a recipient (user ID or email) and a permission.
 * A successful share returns a `ShareRecord` with a public `shareToken` —
 * the public access URL is `{API_BASE_URL}/shares/public/{shareToken}`.
 */
export const shareService = {
  getMyShares: () => apiClient.get<ApiResponse<ShareRecord[]>>(API_ENDPOINTS.share.myShares),

  getSharedWithMe: () =>
    apiClient.get<ApiResponse<ShareRecord[]>>(API_ENDPOINTS.share.sharedWithMe),

  shareFile: ({ fileId, ...body }: CreateShareRequest) =>
    apiClient.post<ApiResponse<ShareRecord>>(API_ENDPOINTS.share.create(fileId), body),

  revokeShare: (id: number) => apiClient.delete<ApiResponse<null>>(API_ENDPOINTS.share.remove(id)),
};
