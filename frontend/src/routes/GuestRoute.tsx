import { Navigate, Outlet } from 'react-router-dom';

import { APP_ROUTES } from '@/constants/routes';
import { useAuthStore, selectIsAuthenticated } from '@/store/authStore';

export function GuestRoute() {
  const isAuthenticated = useAuthStore(selectIsAuthenticated);

  if (isAuthenticated) {
    return <Navigate to={APP_ROUTES.dashboard} replace />;
  }

  return <Outlet />;
}
