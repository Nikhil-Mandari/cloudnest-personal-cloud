import { create } from 'zustand';
import { persist } from 'zustand/middleware';

import { STORAGE_KEYS } from '@/constants/storage';

export type Theme = 'light' | 'dark';

interface ThemeState {
  theme: Theme;
  setTheme: (theme: Theme) => void;
  toggleTheme: () => void;
  /** Toggles the `dark` class on <html> for class-based Tailwind dark mode. */
  applyTheme: (theme: Theme) => void;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      theme: 'light',
      setTheme: (theme) => set({ theme }),
      toggleTheme: () => set((state) => ({ theme: state.theme === 'dark' ? 'light' : 'dark' })),
      applyTheme: (theme) => {
        document.documentElement.classList.toggle('dark', theme === 'dark');
      },
    }),
    {
      name: STORAGE_KEYS.theme,
      partialize: (state) => ({ theme: state.theme }),
    },
  ),
);
