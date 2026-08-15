import { Navigate, Outlet, useLocation } from 'react-router-dom';

import { APP_ROUTES } from '@/constants/routes';
import { useAuthStore, selectIsAuthenticated } from '@/store/authStore';

export function ProtectedRoute() {
  const isAuthenticated = useAuthStore(selectIsAuthenticated);
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to={APP_ROUTES.login} replace state={{ from: location.pathname }} />;
  }

  return <Outlet />;
}
