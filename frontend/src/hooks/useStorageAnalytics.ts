import { useQuery } from '@tanstack/react-query';

import { fileService } from '@/services/file.service';
import { STORAGE_QUOTA_BYTES } from '@/constants/files';

/** React Query key for the storage analytics overview. */
export const STORAGE_OVERVIEW_QUERY_KEY = ['files', 'stats', 'overview'] as const;

/** Computed storage summary derived from the analytics overview. */
export interface StorageSummary {
  used: number;
  quota: number;
  remaining: number;
  /** Percentage 0–100 of the quota used. */
  percentUsed: number;
}

/** Fetches the storage analytics overview for the authenticated user. */
export function useStorageOverviewQuery() {
  return useQuery({
    queryKey: STORAGE_OVERVIEW_QUERY_KEY,
    queryFn: async () => {
      const { data } = await fileService.getStorageOverview();
      return data.data;
    },
  });
}

/** Derives a quota summary from the overview (free tier = 30 GB). */
export function deriveStorageSummary(used: number): StorageSummary {
  const quota = STORAGE_QUOTA_BYTES;
  const clamped = Math.max(0, used);
  return {
    used: clamped,
    quota,
    remaining: Math.max(0, quota - clamped),
    percentUsed: Math.min(100, (clamped / quota) * 100),
  };
}
