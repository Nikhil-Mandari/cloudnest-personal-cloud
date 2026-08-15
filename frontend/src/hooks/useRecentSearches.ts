import { useCallback, useEffect, useState } from 'react';

import { STORAGE_KEYS } from '@/constants/storage';

const MAX_RECENT_SEARCHES = 8;

function readRecents(): string[] {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEYS.recentSearches);
    if (!raw) return [];
    const parsed: unknown = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed
      .filter((item): item is string => typeof item === 'string')
      .map((item) => item.trim())
      .filter(Boolean)
      .slice(0, MAX_RECENT_SEARCHES);
  } catch {
    return [];
  }
}

/**
 * Persisted list of the user's recent global-search queries (most recent
 * first). Best-effort: if localStorage is unavailable (private mode, quota),
 * the list simply lives in memory for the session.
 */
export function useRecentSearches() {
  const [recents, setRecents] = useState<string[]>(readRecents);

  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_KEYS.recentSearches, JSON.stringify(recents));
    } catch {
      // Storage unavailable — keep the in-memory list only.
    }
  }, [recents]);

  const add = useCallback((query: string) => {
    const trimmed = query.trim();
    if (!trimmed) return;
    setRecents((current) =>
      [trimmed, ...current.filter((item) => item.toLowerCase() !== trimmed.toLowerCase())].slice(
        0,
        MAX_RECENT_SEARCHES,
      ),
    );
  }, []);

  const remove = useCallback((query: string) => {
    setRecents((current) => current.filter((item) => item !== query));
  }, []);

  const clear = useCallback(() => setRecents([]), []);

  return { recents, add, remove, clear };
}
