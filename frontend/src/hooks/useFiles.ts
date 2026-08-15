import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';

import { fileService } from '@/services/file.service';
import type { FileItem } from '@/types';
import { downloadFileItem } from '@/utils/download';
import { getErrorMessage } from '@/utils/error';

export const FILES_QUERY_KEY = ['files'] as const;

/** Minimum query length before the server-side search fires. */
export const MIN_SEARCH_QUERY_LENGTH = 2;

/**
 * Lists files at the current explorer location.
 *
 * - {@code undefined} — every active file (dashboard / global views)
 * - {@code null}      — root-level files only
 * - a UUID            — files inside that folder
 *
 * The cache is scoped per location; invalidating `FILES_QUERY_KEY` (partial
 * match) refreshes every scope at once.
 */
export function useFilesQuery(folderId?: string | null) {
  const scope = folderId === undefined ? 'all' : folderId ?? 'root';
  return useQuery({
    queryKey: [...FILES_QUERY_KEY, scope],
    queryFn: async () => {
      const { data } = await fileService.getFiles(folderId);
      return data.data;
    },
  });
}

/**
 * Server-side file search (file-service `/files/search`). Only fires once the
 * query is at least {@link MIN_SEARCH_QUERY_LENGTH} characters.
 */
export function useFileSearchQuery(query: string) {
  return useQuery({
    queryKey: [...FILES_QUERY_KEY, 'search', query.trim().toLowerCase()],
    queryFn: async () => {
      const { data } = await fileService.searchFiles(query);
      return data.data;
    },
    enabled: query.trim().length >= MIN_SEARCH_QUERY_LENGTH,
    staleTime: 30_000,
  });
}

export interface RenameFileVariables {
  id: number;
  originalFileName: string;
}

export interface MoveFileVariables {
  id: number;
  folderId: string | null;
}

/**
 * File mutations (rename, delete, move, favorite) plus the download action.
 * Successes invalidate the files query and surface toast notifications.
 */
export function useFileMutations() {
  const queryClient = useQueryClient();

  const invalidateFiles = () => queryClient.invalidateQueries({ queryKey: FILES_QUERY_KEY });

  const renameFile = useMutation({
    mutationFn: ({ id, originalFileName }: RenameFileVariables) =>
      fileService.renameFile(id, originalFileName),
    onSuccess: () => {
      toast.success('File renamed');
      void invalidateFiles();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to rename the file.')),
  });

  const deleteFile = useMutation({
    mutationFn: (id: number) => fileService.deleteFile(id),
    onSuccess: () => {
      toast.success('File moved to trash');
      void invalidateFiles();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to move the file to trash.')),
  });

  const moveFile = useMutation({
    mutationFn: ({ id, folderId }: MoveFileVariables) => fileService.moveFile(id, folderId),
    onSuccess: () => {
      toast.success('File moved');
      void invalidateFiles();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to move the file.')),
  });

  const toggleFavorite = useMutation({
    mutationFn: ({ id, favorite }: { id: number; favorite: boolean }) =>
      fileService.setFavorite(id, favorite),
    onSuccess: (response, variables) => {
      // Update every folder-scoped cache in place so the star flips instantly,
      // then reconcile. `setQueriesData` partial-matches all `['files', ...]` keys.
      queryClient.setQueriesData<FileItem[]>(
        { queryKey: FILES_QUERY_KEY },
        (current) =>
          current?.map((item) =>
            item.id === variables.id
              ? { ...item, isFavorite: response.data.data.isFavorite }
              : item,
          ),
      );
      toast.success(variables.favorite ? 'Added to favorites' : 'Removed from favorites');
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to update favorite.')),
  });

  /** Streams the file through the API and saves it locally. */
  const downloadFile = async (file: FileItem) => {
    try {
      await downloadFileItem(file);
    } catch (error) {
      toast.error(getErrorMessage(error, 'Failed to download the file.'));
    }
  };

  return { renameFile, deleteFile, moveFile, toggleFavorite, downloadFile, invalidateFiles };
}
