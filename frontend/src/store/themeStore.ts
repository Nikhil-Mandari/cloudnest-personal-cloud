import { create } from 'zustand';
import { persist } from 'zustand/middleware';

import { STORAGE_KEYS } from '@/constants/storage';

export type Theme = 'light' | 'dark' | 'system';

export const DARK_QUERY = '(prefers-color-scheme: dark)';

/** Resolves `system` to the OS preference (defaults to light). */
export function resolveTheme(theme: Theme): 'light' | 'dark' {
  if (theme === 'system') {
    return typeof window !== 'undefined' && window.matchMedia(DARK_QUERY).matches
      ? 'dark'
      : 'light';
  }
  return theme;
}

interface ThemeState {
  theme: Theme;
  setTheme: (theme: Theme) => void;
  toggleTheme: () => void;
  /** Applies the resolved theme as the `dark` class on <html>. */
  applyTheme: (theme: Theme) => void;
}

const CYCLE_ORDER: readonly Theme[] = ['light', 'dark', 'system'];

export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      theme: 'light',
      setTheme: (theme) => set({ theme }),
      toggleTheme: () =>
        set((state) => {
          const index = CYCLE_ORDER.indexOf(state.theme);
          return { theme: CYCLE_ORDER[(index + 1) % CYCLE_ORDER.length] };
        }),
      applyTheme: (theme) => {
        document.documentElement.classList.toggle('dark', resolveTheme(theme) === 'dark');
      },
    }),
    {
      name: STORAGE_KEYS.theme,
      partialize: (state) => ({ theme: state.theme }),
    },
  ),
);
