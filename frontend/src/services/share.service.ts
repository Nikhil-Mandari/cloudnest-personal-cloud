import { apiClient } from '@/api/axios';
import { API_ENDPOINTS } from '@/constants/apiEndpoints';
import type {
  ApiResponse,
  CreateShareRequest,
  ShareAnalytics,
  ShareRecord,
  UpdateShareRequest,
  VerifySharePasswordRequest,
} from '@/types';

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

  /** Updates permission / expiry / password of an existing share. */
  updateShare: (id: number, body: UpdateShareRequest) =>
    apiClient.put<ApiResponse<ShareRecord>>(API_ENDPOINTS.share.update(id), body),

  /** Owner-only analytics for a share link. */
  getShareAnalytics: (id: number) =>
    apiClient.get<ApiResponse<ShareAnalytics>>(API_ENDPOINTS.share.analytics(id)),

  /** Verifies the password of a password-protected public share. */
  verifySharePassword: (token: string, body: VerifySharePasswordRequest) =>
    apiClient.post<ApiResponse<ShareRecord>>(API_ENDPOINTS.share.verifyPassword(token), body, {
      // A wrong share password is a 401 — never treat it as an expired session.
      silent: true,
      skipAuthRedirect: true,
    }),

  // ── Public share-link browsing (no auth required) ─────────────────────────

  /**
   * Loads share metadata for the public browse page. Silent: 404/410 render
   * as inline "link unavailable" states instead of global toasts.
   */
  getPublicShare: (token: string) =>
    apiClient.get<ApiResponse<ShareRecord>>(API_ENDPOINTS.share.public(token), {
      silent: true,
      skipAuthRedirect: true,
    }),

  /** Streams the shared file's bytes through the public download endpoint. */
  downloadPublicShare: (token: string, password?: string) =>
    apiClient.get<Blob>(API_ENDPOINTS.share.download(token), {
      responseType: 'blob',
      headers: password ? { 'X-Share-Password': password } : undefined,
      // Same as above: a wrong/absent share password surfaces as a 401.
      silent: true,
      skipAuthRedirect: true,
    }),

  /**
   * Streams the shared file's bytes for in-browser preview. Does not count as
   * a download in the owner's analytics.
   */
  previewPublicShare: (token: string, password?: string) =>
    apiClient.get<Blob>(API_ENDPOINTS.share.preview(token), {
      responseType: 'blob',
      headers: password ? { 'X-Share-Password': password } : undefined,
      silent: true,
      skipAuthRedirect: true,
    }),
};
