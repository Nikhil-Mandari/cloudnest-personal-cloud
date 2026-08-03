import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';

import { APP_ROUTES } from '@/constants/routes';
import { authService } from '@/services/auth.service';
import { useAuthStore } from '@/store/authStore';
import type { LoginFormValues, RegisterFormValues } from '@/types';
import { getErrorMessage } from '@/utils/error';

export interface LoginMutationVariables extends LoginFormValues {
  /** Optional redirect target (e.g. the page the user was trying to reach). */
  from?: string;
}

/**
 * Auth mutations for the login / register pages.
 *
 * - Login: exchanges credentials for a JWT, stores it immediately (so the
 *   Axios interceptor and route guards work), then best-effort hydrates the
 *   user profile from `/auth/me` and redirects.
 * - Register: creates the account and redirects to the login page.
 */
export function useAuthMutations() {
  const navigate = useNavigate();
  const setToken = useAuthStore((state) => state.setToken);
  const setUser = useAuthStore((state) => state.setUser);
  const setStatus = useAuthStore((state) => state.setStatus);

  const login = useMutation({
    mutationFn: async ({ email, password }: LoginMutationVariables) => {
      const { data } = await authService.login({ email, password });
      return data.data.token;
    },
    onSuccess: async (token, variables) => {
      setToken(token);
      setStatus('authenticated');
      toast.success('Welcome back!');

      // Redirect immediately for a snappy UX; hydrate the profile in the
      // background (the UI already handles `user === null`). Silent + no
      // auth-redirect: the fetch is best-effort and the token is fresh.
      navigate(variables.from ?? APP_ROUTES.dashboard, { replace: true });

      try {
        const { data } = await authService.getMe({ silent: true, skipAuthRedirect: true });
        // Guard against a logout racing the profile fetch.
        if (useAuthStore.getState().token === token) {
          setUser(data.data);
        }
      } catch {
        // Best-effort: the profile will hydrate on the next app load.
      }
    },
    onError: (error) => {
      toast.error(getErrorMessage(error, 'Sign in failed. Please try again.'));
    },
  });

  const register = useMutation({
    mutationFn: async ({ fullName, email, password }: RegisterFormValues) => {
      await authService.register({ fullName, email, password });
    },
    onSuccess: () => {
      toast.success('Account created! Please sign in.');
      navigate(APP_ROUTES.login);
    },
    onError: (error) => {
      toast.error(getErrorMessage(error, 'Registration failed. Please try again.'));
    },
  });

  return { loginMutation: login, registerMutation: register };
}
