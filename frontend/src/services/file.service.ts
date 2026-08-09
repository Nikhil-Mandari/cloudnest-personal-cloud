import { apiClient } from '@/api/axios';
import { API_ENDPOINTS } from '@/constants/apiEndpoints';
import type {
  ApiResponse,
  DuplicateAction,
  FileDetail,
  FileItem,
  UploadResult,
} from '@/types';

export interface UploadFileOptions {
  /** Destination folder UUID (omitted = root). */
  folderId?: string | null;
  /** Upload progress callback (0–100). */
  onProgress?: (percent: number) => void;
  /** AbortSignal for cancelling the upload. */
  signal?: AbortSignal;
  /**
   * How the server should resolve a duplicate-content upload:
   * ASK (default), KEEP_BOTH, SKIP or REPLACE.
   */
  onDuplicate?: DuplicateAction;
}

/**
 * File service (file-service).
 *
 * NOTE: minimal reconstruction — only the methods backed by endpoints present
 * on this branch's API map. The trash / version-history / ZIP / analytics /
 * audit / scan methods are intentionally deferred and restored when their
 * slices land (they require `apiEndpoints.files` keys not yet available).
 *
 * Every mutation endpoint is keyed by the internal numeric `id` (the list
 * endpoints expose it as `FileItem.id`); `fileId` is the public UUID used for
 * external references. Search uses the `query` query parameter.
 */
export const fileService = {
  /**
   * Lists the user's active files.
   *
   * @param folderId `undefined` = every active file (dashboard view);
   *                 `null` = root-level files only; a UUID = files inside that
   *                 folder (folder navigation).
   */
  getFiles: (folderId?: string | null) =>
    apiClient.get<ApiResponse<FileItem[]>>(API_ENDPOINTS.files.list, {
      params: folderId ? { folderId } : folderId === null ? { folderId: '' } : undefined,
    }),

  getFavoriteFiles: () => apiClient.get<ApiResponse<FileItem[]>>(API_ENDPOINTS.files.favorites),

  searchFiles: (query: string) =>
    apiClient.get<ApiResponse<FileItem[]>>(API_ENDPOINTS.files.search, { params: { query } }),

  uploadFile: (file: File, options: UploadFileOptions = {}) => {
    const formData = new FormData();
    formData.append('file', file);
    if (options.folderId) {
      formData.append('folderId', options.folderId);
    }

    return apiClient.post<ApiResponse<UploadResult>>(API_ENDPOINTS.files.upload, formData, {
      // Do NOT set a manual `Content-Type: multipart/form-data` — the browser
      // must generate the boundary. Forcing it without a boundary breaks
      // multipart parsing (upload fails with 400/500).
      headers: { 'Content-Type': null },
      params: options.onDuplicate ? { onDuplicate: options.onDuplicate } : undefined,
      signal: options.signal,
      onUploadProgress: (event) => {
        if (event.total) {
          options.onProgress?.(Math.round((event.loaded / event.total) * 100));
        }
      },
    });
  },

  downloadFile: (id: number) =>
    apiClient.get<Blob>(API_ENDPOINTS.files.download(id), { responseType: 'blob' }),

  previewFile: (id: number) =>
    apiClient.get<Blob>(API_ENDPOINTS.files.preview(id), { responseType: 'blob' }),

  renameFile: (id: number, originalFileName: string) =>
    apiClient.put<ApiResponse<FileDetail>>(API_ENDPOINTS.files.rename(id), { originalFileName }),

  moveFile: (id: number, folderId: string | null) =>
    apiClient.patch<ApiResponse<FileDetail>>(API_ENDPOINTS.files.move(id), undefined, {
      params: folderId ? { folderId } : undefined,
    }),

  setFavorite: (id: number, favorite: boolean) =>
    apiClient.patch<ApiResponse<FileDetail>>(API_ENDPOINTS.files.favorite(id), undefined, {
      params: { favorite },
    }),

  deleteFile: (id: number) => apiClient.delete<ApiResponse<null>>(API_ENDPOINTS.files.remove(id)),

  restoreFile: (id: number) =>
    apiClient.patch<ApiResponse<FileDetail>>(API_ENDPOINTS.files.restore(id)),
};
