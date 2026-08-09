import { useQuery } from '@tanstack/react-query';

import { fetchGatewayHealth } from '@/services/system.service';

export const SYSTEM_STATUS_QUERY_KEY = ['system-status'] as const;

/**
 * Polls the API gateway actuator health endpoint so the Settings page can show
 * live API / database / MinIO / microservice status. Refetches every minute
 * and never blocks rendering (returns `unknown` state while offline).
 */
export function useSystemStatus() {
  return useQuery({
    queryKey: SYSTEM_STATUS_QUERY_KEY,
    queryFn: fetchGatewayHealth,
    staleTime: 30_000,
    refetchInterval: 60_000,
    retry: 1,
  });
}
