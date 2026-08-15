import { useCallback, useSyncExternalStore } from 'react';

/**
 * Tracks a CSS media query (e.g. `(min-width: 1024px)`) and re-renders when it
 * changes. Built on `useSyncExternalStore` for correctness.
 */
export function useMediaQuery(query: string): boolean {
  const subscribe = useCallback(
    (onStoreChange: () => void) => {
      const mql = window.matchMedia(query);
      mql.addEventListener('change', onStoreChange);
      return () => mql.removeEventListener('change', onStoreChange);
    },
    [query],
  );

  const getSnapshot = useCallback(() => window.matchMedia(query).matches, [query]);

  return useSyncExternalStore(subscribe, getSnapshot, getSnapshot);
}
