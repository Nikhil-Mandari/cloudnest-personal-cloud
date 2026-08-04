import { useQuery } from '@tanstack/react-query';

import { folderService } from '@/services/folder.service';

export const FOLDERS_QUERY_KEY = ['folders'] as const;

/** Lists the authenticated user's folders (used by the move dialog). */
export function useFoldersQuery() {
  return useQuery({
    queryKey: FOLDERS_QUERY_KEY,
    queryFn: async () => {
      const { data } = await folderService.getFolders();
      return data.data;
    },
  });
}
