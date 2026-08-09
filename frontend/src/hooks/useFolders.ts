import { useQuery } from '@tanstack/react-query';

import { folderService } from '@/services/folder.service';

export const FOLDERS_QUERY_KEY = ['folders'] as const;

/**
 * Lists the authenticated user's folders (flat list — move dialog etc.).
 *
 * NOTE: minimal reconstruction — only the query GlobalSearch needs. The
 * folder-contents query and folder mutations belong to the deferred
 * FoldersPage slice and are restored with it.
 */
export function useFoldersQuery() {
  return useQuery({
    queryKey: FOLDERS_QUERY_KEY,
    queryFn: async () => {
      const { data } = await folderService.getFolders();
      return data.data;
    },
  });
}
