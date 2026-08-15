import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';

import { folderService } from '@/services/folder.service';
import type { CreateFolderRequest } from '@/types';
import { getErrorMessage } from '@/utils/error';

export const FOLDERS_QUERY_KEY = ['folders'] as const;

/** Lists the authenticated user's folders (flat list — move dialog etc.). */
export function useFoldersQuery() {
  return useQuery({
    queryKey: FOLDERS_QUERY_KEY,
    queryFn: async () => {
      const { data } = await folderService.getFolders();
      return data.data;
    },
  });
}

/**
 * Folders visible at the current explorer location: root-level folders when
 * {@code folderId} is null, or the immediate children of the given folder.
 * The cache is scoped per location, so navigating around never mixes levels.
 */
export function useFolderContentsQuery(folderId: string | null) {
  return useQuery({
    queryKey: [...FOLDERS_QUERY_KEY, 'contents', folderId ?? 'root'],
    queryFn: async () => {
      const { data } = folderId
        ? await folderService.getFolderChildren(folderId)
        : await folderService.getRootFolders();
      return data.data;
    },
  });
}

/**
 * Folder mutations (create, rename, delete). Successes invalidate the folders
 * query and surface toast notifications.
 */
export function useFolderMutations() {
  const queryClient = useQueryClient();

  const invalidateFolders = () =>
    queryClient.invalidateQueries({ queryKey: FOLDERS_QUERY_KEY });

  const createFolder = useMutation({
    mutationFn: (payload: CreateFolderRequest) => folderService.createFolder(payload),
    onSuccess: () => {
      toast.success('Folder created');
      void invalidateFolders();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to create the folder.')),
  });

  const renameFolder = useMutation({
    mutationFn: ({ id, name }: { id: string; name: string }) =>
      folderService.renameFolder(id, name),
    onSuccess: () => {
      toast.success('Folder renamed');
      void invalidateFolders();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to rename the folder.')),
  });

  const deleteFolder = useMutation({
    mutationFn: (id: string) => folderService.deleteFolder(id),
    onSuccess: () => {
      toast.success('Folder deleted');
      void invalidateFolders();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to delete the folder.')),
  });

  return { createFolder, renameFolder, deleteFolder, invalidateFolders };
}
