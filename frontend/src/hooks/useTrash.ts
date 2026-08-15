import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';

import { fileService } from '@/services/file.service';
import { folderService } from '@/services/folder.service';
import type { TrashData } from '@/utils/trash';
import { getErrorMessage } from '@/utils/error';
import { FILES_QUERY_KEY } from './useFiles';
import { FOLDERS_QUERY_KEY } from './useFolders';

export const TRASH_QUERY_KEY = ['trash'] as const;

/** Loads trashed files and folders in parallel. */
export function useTrashQuery() {
  return useQuery({
    queryKey: TRASH_QUERY_KEY,
    queryFn: async (): Promise<TrashData> => {
      const [filesRes, foldersRes] = await Promise.all([
        fileService.getTrashFiles(),
        folderService.getTrashFolders(),
      ]);
      return { files: filesRes.data.data, folders: foldersRes.data.data };
    },
  });
}

/**
 * Trash mutations (restore / permanent delete / empty). Every success
 * refreshes the trash plus the active files and folders lists.
 */
export function useTrashMutations() {
  const queryClient = useQueryClient();

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: TRASH_QUERY_KEY });
    void queryClient.invalidateQueries({ queryKey: FILES_QUERY_KEY });
    void queryClient.invalidateQueries({ queryKey: FOLDERS_QUERY_KEY });
  };

  /**
   * Individual restore / permanent-delete mutations.
   *
   * Batch operations (restore all, delete selection) pass `quiet: true` and
   * show a single summary toast themselves, so restoring 50 items does not
   * queue 50 identical toasts.
   */
  const restoreFile = useMutation({
    mutationFn: ({ id }: { id: number; quiet?: boolean }) => fileService.restoreFile(id),
    onSuccess: (_data, variables) => {
      if (!variables.quiet) {
        toast.success('File restored');
      }
      invalidate();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to restore the file.')),
  });

  const permanentDeleteFile = useMutation({
    mutationFn: ({ id }: { id: number; quiet?: boolean }) =>
      fileService.permanentlyDeleteFile(id),
    onSuccess: (_data, variables) => {
      if (!variables.quiet) {
        toast.success('File permanently deleted');
      }
      invalidate();
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, 'Failed to permanently delete the file.')),
  });

  const restoreFolder = useMutation({
    mutationFn: ({ id }: { id: string; quiet?: boolean }) => folderService.restoreFolder(id),
    onSuccess: (_data, variables) => {
      if (!variables.quiet) {
        toast.success('Folder restored');
      }
      invalidate();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to restore the folder.')),
  });

  const permanentDeleteFolder = useMutation({
    mutationFn: ({ id }: { id: string; quiet?: boolean }) =>
      folderService.permanentlyDeleteFolder(id),
    onSuccess: (_data, variables) => {
      if (!variables.quiet) {
        toast.success('Folder permanently deleted');
      }
      invalidate();
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, 'Failed to permanently delete the folder.')),
  });

  const emptyTrash = useMutation({
    mutationFn: async () => {
      await Promise.allSettled([fileService.emptyTrash(), folderService.emptyTrash()]);
    },
    onSuccess: () => {
      toast.success('Trash emptied');
      invalidate();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to empty the trash.')),
  });

  return { restoreFile, permanentDeleteFile, restoreFolder, permanentDeleteFolder, emptyTrash };
}
