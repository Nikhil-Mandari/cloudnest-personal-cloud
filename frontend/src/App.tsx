import { useEffect } from 'react';

import { AppProviders } from '@/context/AppProviders';
import { DARK_QUERY, useThemeStore } from '@/store/themeStore';

export default function App() {
  const theme = useThemeStore((state) => state.theme);

  // Keep the <html> class in sync with the persisted theme (also handled
  // pre-hydration by the inline script in index.html to avoid a flash).
  useEffect(() => {
    useThemeStore.getState().applyTheme(theme);
  }, [theme]);

  // While in `system` mode, follow OS theme changes live.
  useEffect(() => {
    if (!window.matchMedia) {
      return;
    }
    const mql = window.matchMedia(DARK_QUERY);
    const onChange = () => {
      if (useThemeStore.getState().theme === 'system') {
        useThemeStore.getState().applyTheme('system');
      }
    };
    mql.addEventListener('change', onChange);
    return () => mql.removeEventListener('change', onChange);
  }, []);

  return <AppProviders />;
}
