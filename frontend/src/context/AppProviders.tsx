import { QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import { queryClient } from '@/api/queryClient';
import { ErrorBoundary } from '@/components/common/ErrorBoundary';
import { router } from '@/routes';
import { useThemeStore } from '@/store/themeStore';

/**
 * Composition root: wires together the global error boundary, React Query,
 * the router and the toast container.
 */
export function AppProviders() {
  const theme = useThemeStore((state) => state.theme);

  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
        <ToastContainer
          position="top-right"
          autoClose={4000}
          hideProgressBar={false}
          newestOnTop
          closeOnClick
          pauseOnHover
          theme={theme}
        />
      </QueryClientProvider>
    </ErrorBoundary>
  );
}
