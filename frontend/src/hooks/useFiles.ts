import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';

import { fileService } from '@/services/file.service';
import type { FileItem } from '@/types';
import { downloadFileItem } from '@/utils/download';
import { getErrorMessage } from '@/utils/error';

export const FILES_QUERY_KEY = ['files'] as const;

/** Lists the authenticated user's active files. */
export function useFilesQuery() {
  return useQuery({
    queryKey: FILES_QUERY_KEY,
    queryFn: async () => {
      const { data } = await fileService.getFiles();
      return data.data;
    },
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
      toast.success('File deleted');
      void invalidateFiles();
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to delete the file.')),
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
      // Update the cache in place so the star flips instantly, then reconcile.
      queryClient.setQueryData<FileItem[]>(FILES_QUERY_KEY, (current) =>
        current?.map((item) =>
          item.id === variables.id ? { ...item, isFavorite: response.data.data.isFavorite } : item,
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
