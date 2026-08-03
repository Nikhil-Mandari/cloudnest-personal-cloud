import { useEffect } from 'react';

import { AppProviders } from '@/context/AppProviders';
import { useThemeStore } from '@/store/themeStore';

export default function App() {
  const theme = useThemeStore((state) => state.theme);

  // Keep the <html> class in sync with the persisted theme (also handled
  // pre-hydration by the inline script in index.html to avoid a flash).
  useEffect(() => {
    useThemeStore.getState().applyTheme(theme);
  }, [theme]);

  return <AppProviders />;
}
