import { Navigate, Outlet } from 'react-router-dom';

import { Spinner } from '@/components/ui/Spinner';
import { APP_ROUTES } from '@/constants/routes';
import { selectIsAuthenticated, useAuthStore } from '@/store/authStore';
import { isAdminRole } from '@/utils/role';

/**
 * Guards the admin dashboard: redirects to the login page when signed out and
 * to the dashboard when the signed-in user is not an administrator.
 *
 * Right after a sign-in the profile hydrates asynchronously (`user` is briefly
 * null), so instead of bouncing an admin to the dashboard we wait for the
 * profile before deciding.
 */
export function AdminRoute() {
  const isAuthenticated = useAuthStore(selectIsAuthenticated);
  const user = useAuthStore((state) => state.user);

  if (!isAuthenticated) {
    return <Navigate to={APP_ROUTES.login} replace />;
  }

  // Profile still hydrating after login — wait before deciding the role.
  if (!user) {
    return (
      <div className="grid min-h-screen place-items-center bg-gray-50 dark:bg-gray-950">
        <Spinner size="lg" className="text-brand-500" />
      </div>
    );
  }

  if (!isAdminRole(user.role)) {
    return <Navigate to={APP_ROUTES.dashboard} replace />;
  }

  return <Outlet />;
}
