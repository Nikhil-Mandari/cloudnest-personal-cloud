import { apiClient } from '@/api/axios';
import { API_ENDPOINTS } from '@/constants/apiEndpoints';
import type { ApiResponse, FileDetail, FileItem } from '@/types';

export interface UploadFileOptions {
  /** Destination folder UUID (omitted = root). */
  folderId?: string | null;
  /** Upload progress callback (0–100). */
  onProgress?: (percent: number) => void;
  /** AbortSignal for cancelling the upload. */
  signal?: AbortSignal;
}

/**
 * File service (file-service).
 *
 * Every mutation endpoint is keyed by the internal numeric `id` (the list
 * endpoints expose it as `FileItem.id`); `fileId` is the public UUID used for
 * external references. Search uses the `query` query parameter.
 */
export const fileService = {
  getFiles: () => apiClient.get<ApiResponse<FileItem[]>>(API_ENDPOINTS.files.list),

  getFavoriteFiles: () => apiClient.get<ApiResponse<FileItem[]>>(API_ENDPOINTS.files.favorites),

  searchFiles: (query: string) =>
    apiClient.get<ApiResponse<FileItem[]>>(API_ENDPOINTS.files.search, { params: { query } }),

  uploadFile: (file: File, options: UploadFileOptions = {}) => {
    const formData = new FormData();
    formData.append('file', file);
    if (options.folderId) {
      formData.append('folderId', options.folderId);
    }

    return apiClient.post<ApiResponse<FileDetail>>(API_ENDPOINTS.files.upload, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
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
