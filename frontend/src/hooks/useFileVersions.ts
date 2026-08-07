import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';

import { fileService } from '@/services/file.service';
import { FILES_QUERY_KEY } from './useFiles';
import { getErrorMessage } from '@/utils/error';

/** React Query key prefix for a file's version list. */
export const VERSIONS_QUERY_KEY = (fileId: number) => ['files', fileId, 'versions'] as const;

/** Lists the archived versions of a file, newest first. */
export function useFileVersionsQuery(fileId: number | null, enabled = false) {
  return useQuery({
    queryKey: VERSIONS_QUERY_KEY(fileId ?? 0),
    queryFn: async () => {
      const { data } = await fileService.getVersions(fileId as number);
      return data.data;
    },
    enabled: enabled && fileId !== null,
  });
}

/** Uploads a new version (current content is archived automatically). */
export function useUploadVersionMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, file }: { id: number; file: File }) =>
      fileService.uploadNewVersion(id, file),
    onSuccess: (_data, variables) => {
      toast.success('New version uploaded');
      void queryClient.invalidateQueries({ queryKey: VERSIONS_QUERY_KEY(variables.id) });
      void queryClient.invalidateQueries({ queryKey: FILES_QUERY_KEY });
    },
    onError: (error, variables) =>
      toast.error(getErrorMessage(error, `Failed to upload a new version of "${variables.file.name}"`)),
  });
}

/** Restores an archived version as the file's current content. */
export function useRestoreVersionMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, versionId }: { id: number; versionId: number }) =>
      fileService.restoreVersion(id, versionId),
    onSuccess: (_data, variables) => {
      toast.success('Version restored');
      void queryClient.invalidateQueries({ queryKey: VERSIONS_QUERY_KEY(variables.id) });
      void queryClient.invalidateQueries({ queryKey: FILES_QUERY_KEY });
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to restore this version')),
  });
}

/** Deletes an archived version and its content. */
export function useDeleteVersionMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, versionId }: { id: number; versionId: number }) =>
      fileService.deleteVersion(id, versionId),
    onSuccess: (_data, variables) => {
      toast.success('Version deleted');
      void queryClient.invalidateQueries({ queryKey: VERSIONS_QUERY_KEY(variables.id) });
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to delete this version')),
  });
}

/** Downloads an archived version. */
export function useDownloadVersionMutation() {
  return useMutation({
    mutationFn: ({ id, versionId, fileName }: { id: number; versionId: number; fileName: string }) =>
      fileService.downloadVersion(id, versionId).then(({ data }) => ({ data, fileName })),
    onSuccess: async ({ data, fileName }) => {
      const { blobDownload } = await import('@/utils/download');
      blobDownload(data, fileName);
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to download this version')),
  });
}
