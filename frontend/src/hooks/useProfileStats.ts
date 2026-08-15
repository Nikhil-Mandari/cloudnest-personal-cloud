import { useMemo } from 'react';

import { STORAGE_QUOTA_BYTES } from '@/constants/files';
import { useFilesQuery } from './useFiles';
import { useFoldersQuery } from './useFolders';

export const RECENT_ACTIVITY_LIMIT = 6;

/**
 * Derives the profile statistics (files count, folders count, storage used,
 * recent activity) from the existing file/folder queries. The backend does
 * not expose aggregates, so these are computed client-side from the same
 * cached data the rest of the app already loads.
 */
export function useProfileStats() {
  const filesQuery = useFilesQuery();
  const foldersQuery = useFoldersQuery();

  const files = useMemo(() => filesQuery.data ?? [], [filesQuery.data]);
  const folders = useMemo(() => foldersQuery.data ?? [], [foldersQuery.data]);

  const storageUsed = useMemo(
    () => files.reduce((sum, file) => sum + file.fileSize, 0),
    [files],
  );

  const storagePercent = useMemo(() => {
    if (STORAGE_QUOTA_BYTES <= 0) {
      return 0;
    }
    return Math.min(100, Math.round((storageUsed / STORAGE_QUOTA_BYTES) * 100));
  }, [storageUsed]);

  const recentFiles = useMemo(
    () =>
      [...files]
        .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
        .slice(0, RECENT_ACTIVITY_LIMIT),
    [files],
  );

  return {
    filesCount: files.length,
    foldersCount: folders.length,
    storageUsed,
    storageQuota: STORAGE_QUOTA_BYTES,
    storagePercent,
    recentFiles,
    isLoading: filesQuery.isLoading || foldersQuery.isLoading,
  };
}
