import { motion } from 'framer-motion';
import { Monitor, Moon, Sun } from 'lucide-react';

import { useThemeStore, type Theme } from '@/store/themeStore';

const THEME_ICONS: Record<Theme, typeof Sun> = {
  light: Sun,
  dark: Moon,
  system: Monitor,
};

const NEXT_MODE: Record<Theme, string> = {
  light: 'dark',
  dark: 'system',
  system: 'light',
};

export function ThemeToggle() {
  const theme = useThemeStore((state) => state.theme);
  const toggleTheme = useThemeStore((state) => state.toggleTheme);
  const Icon = THEME_ICONS[theme];

  return (
    <button
      type="button"
      onClick={toggleTheme}
      aria-label={`Theme: ${theme}. Switch to ${NEXT_MODE[theme]} mode.`}
      title={`Switch to ${NEXT_MODE[theme]} mode`}
      className="grid h-10 w-10 place-items-center rounded-lg text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-900 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-white"
    >
      <motion.span
        key={theme}
        initial={{ rotate: -90, opacity: 0, scale: 0.6 }}
        animate={{ rotate: 0, opacity: 1, scale: 1 }}
        transition={{ duration: 0.25, ease: 'easeOut' }}
        className="inline-flex"
      >
        <Icon className="h-5 w-5" />
      </motion.span>
    </button>
  );
}
