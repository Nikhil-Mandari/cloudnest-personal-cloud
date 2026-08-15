import { useAuthStore } from '@/store/authStore';

/**
 * Convenience hook around the persisted auth store.
 */
export function useAuth() {
  const token = useAuthStore((state) => state.token);
  const user = useAuthStore((state) => state.user);
  const status = useAuthStore((state) => state.status);
  const setToken = useAuthStore((state) => state.setToken);
  const setUser = useAuthStore((state) => state.setUser);
  const setStatus = useAuthStore((state) => state.setStatus);
  const setAuth = useAuthStore((state) => state.setAuth);
  const logout = useAuthStore((state) => state.logout);

  return {
    token,
    user,
    status,
    isAuthenticated: Boolean(token),
    setToken,
    setUser,
    setStatus,
    setAuth,
    logout,
  };
}
