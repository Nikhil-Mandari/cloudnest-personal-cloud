import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { toast } from 'react-toastify';

import { APP_ROUTES } from '@/constants/routes';
import { useAuthStore } from '@/store/authStore';

/**
 * Landing page for the social-login redirect.
 * <p>
 * The auth-service redirects the browser here after a successful Google /
 * GitHub sign-in with {@code #token=...&refreshToken=...} in the URL fragment
 * (fragments never reach the server, so the session tokens stay out of
 * access logs and browser history is the only trace). This page stores the
 * session and forwards to the dashboard. Failed attempts arrive as
 * {@code ?oauth=error} (query string) and are sent back to the login page.
 */
export function OAuthCallbackPage() {
  const navigate = useNavigate();

  useEffect(() => {
    // Tokens travel in the fragment (#token=...&refreshToken=...); errors in
    // the query string (?oauth=error). Check both.
    const hashParams = new URLSearchParams(window.location.hash.replace(/^#/, ''));
    const queryParams = new URLSearchParams(window.location.search);
    const token = hashParams.get('token');
    const refreshToken = hashParams.get('refreshToken');

    if (queryParams.get('oauth') === 'error' || !token) {
      toast.error('Social sign-in could not be completed. Please try again or use email and password.');
      navigate(APP_ROUTES.login, { replace: true });
      return;
    }

    // Persist the session (profile hydrates via the dashboard bootstrap).
    useAuthStore.getState().setAuthSession(token, refreshToken, null);
    navigate(APP_ROUTES.dashboard, { replace: true });
  }, [navigate]);

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-white text-gray-600 dark:bg-gray-950 dark:text-gray-400">
      <Loader2 className="text-brand-600 h-8 w-8 animate-spin dark:text-brand-400" />
      <p className="text-sm">Completing sign-in…</p>
    </div>
  );
}
