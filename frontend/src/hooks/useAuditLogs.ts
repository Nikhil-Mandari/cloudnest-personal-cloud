import { useQuery } from '@tanstack/react-query';

import { fileService } from '@/services/file.service';

/** React Query key for a page of the audit trail. */
export const AUDIT_LOGS_QUERY_KEY = (page: number, size: number, action?: string) =>
  ['files', 'audit-logs', { page, size, action: action ?? 'all' }] as const;

/** Fetches a page of the user's audit-trail entries, newest first. */
export function useAuditLogsQuery(page: number, size: number, action?: string) {
  return useQuery({
    queryKey: AUDIT_LOGS_QUERY_KEY(page, size, action),
    queryFn: async () => {
      const { data } = await fileService.getAuditLogs({ page, size, action: action || undefined });
      return data.data;
    },
  });
}
