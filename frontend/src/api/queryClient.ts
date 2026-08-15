import { QueryClient } from '@tanstack/react-query';

/**
 * Global TanStack Query client. Tune defaults here as the app grows
 * (e.g. per-query `staleTime`, upload retries, etc.).
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});
