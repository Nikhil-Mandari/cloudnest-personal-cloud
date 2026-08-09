import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';

import { shareService } from '@/services/share.service';
import type { CreateShareRequest, ShareAnalytics, UpdateShareRequest } from '@/types';
import { getErrorMessage } from '@/utils/error';

export const SHARES_SHARED_WITH_ME_QUERY_KEY = ['shares', 'shared-with-me'] as const;
export const SHARES_MY_SHARES_QUERY_KEY = ['shares', 'my-shares'] as const;

/** Lists the resources other users have shared with the authenticated user. */
export function useSharedWithMeQuery() {
  return useQuery({
    queryKey: SHARES_SHARED_WITH_ME_QUERY_KEY,
    queryFn: async () => {
      const { data } = await shareService.getSharedWithMe();
      return data.data;
    },
  });
}

/** Lists the share links the authenticated user has created. */
export function useMySharesQuery() {
  return useQuery({
    queryKey: SHARES_MY_SHARES_QUERY_KEY,
    queryFn: async () => {
      const { data } = await shareService.getMyShares();
      return data.data;
    },
  });
}

/** Creates a share for a file (recipient + permission + optional expiry/password). */
export function useShareFileMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateShareRequest) => shareService.shareFile(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: SHARES_MY_SHARES_QUERY_KEY });
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to create the share link.')),
  });
}

/** Updates permission / expiry / password of an existing share. */
export function useUpdateShareMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: UpdateShareRequest }) =>
      shareService.updateShare(id, body),
    onSuccess: (_data, variables) => {
      toast.success('Share updated');
      void queryClient.invalidateQueries({ queryKey: SHARES_MY_SHARES_QUERY_KEY });
      void queryClient.invalidateQueries({ queryKey: ['shares', 'analytics', variables.id] });
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to update the share.')),
  });
}

/** Owner-only access analytics for a share link. */
export function useShareAnalyticsQuery(shareId: number | null) {
  return useQuery({
    queryKey: ['shares', 'analytics', shareId ?? 0],
    queryFn: async () => {
      const { data } = await shareService.getShareAnalytics(shareId as number);
      return data.data as ShareAnalytics;
    },
    enabled: shareId !== null,
  });
}

/**
 * Loads a public share for the unauthenticated browse page.
 *
 * Silent + no retry: 404 (removed link) and 410 (expired link) render as
 * inline states instead of retrying or firing global toasts. Background
 * refetches are disabled because every fetch records a view server-side.
 */
export function usePublicShareQuery(token: string | undefined) {
  return useQuery({
    queryKey: ['shares', 'public', token ?? ''],
    queryFn: async () => {
      const { data } = await shareService.getPublicShare(token as string);
      return data.data;
    },
    enabled: Boolean(token),
    retry: false,
    refetchOnWindowFocus: false,
  });
}

/** Revokes (deletes) one of the user's share links. */
export function useRevokeShareMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => shareService.revokeShare(id),
    onSuccess: () => {
      toast.success('Share link revoked');
      void queryClient.invalidateQueries({ queryKey: SHARES_MY_SHARES_QUERY_KEY });
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to revoke the share link.')),
  });
}
