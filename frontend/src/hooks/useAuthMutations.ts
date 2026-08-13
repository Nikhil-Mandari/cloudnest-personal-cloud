import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';

import { APP_ROUTES } from '@/constants/routes';
import { authService } from '@/services/auth.service';
import { useAuthStore } from '@/store/authStore';
import type { LoginFormValues, RegisterFormValues, VerifyOtpRequest, VerifyOtpState } from '@/types';
import { getErrorMessage } from '@/utils/error';

export interface LoginMutationVariables extends LoginFormValues {
  /** Optional redirect target (e.g. the page the user was trying to reach). */
  from?: string;
}

export interface VerifyOtpMutationVariables {
  purpose: VerifyOtpState['purpose'];
  code: string;
  email?: string;
  challengeToken?: string;
  rememberDevice?: boolean;
  from?: string;
  devOtp?: string;
  resendAfterSeconds?: number;
  otpExpiryMinutes?: number;
}

/**
 * Auth mutations for the login / register / OTP-verify pages.
 *
 * - Login: exchanges credentials for a token pair or an OTP challenge; when
 *   the backend requires an OTP the user is routed to the verify page.
 * - Register: creates the (pending) account and routes to the verify page.
 * - Verify: completes registration or login with the emailed code.
 */
export function useAuthMutations() {
  const navigate = useNavigate();
  const setAuthSession = useAuthStore((state) => state.setAuthSession);
  const setStatus = useAuthStore((state) => state.setStatus);

  const login = useMutation({
    mutationFn: async ({ email, password, rememberMe }: LoginMutationVariables) => {
      const payload = { usernameOrEmail: email, password, rememberDevice: rememberMe ?? false };
      const { data } = await authService.login(payload);
      return { response: data.data, rememberMe: rememberMe ?? false };
    },
    onSuccess: async ({ response, rememberMe }, variables) => {
      if (response.requires2fa) {
        toast.info('Password verified — enter your authenticator code.');
        navigate(APP_ROUTES.verifyOtp, {
          state: {
            purpose: '2fa',
            challengeToken: response.challengeToken,
            email: response.email,
            rememberDevice: rememberMe,
            from: variables.from,
          } satisfies VerifyOtpState,
        });
        return;
      }

      if (response.requiresOtp) {
        toast.info('Password verified — check your email for the sign-in code.');
        navigate(APP_ROUTES.verifyOtp, {
          state: {
            purpose: 'login',
            challengeToken: response.challengeToken,
            email: response.email,
            rememberDevice: rememberMe,
            devOtp: response.devOtp,
            resendAfterSeconds: response.resendAfterSeconds,
            otpExpiryMinutes: response.otpExpiryMinutes,
            from: variables.from,
          } satisfies VerifyOtpState,
        });
        return;
      }

      if (response.token) {
        applySession(response.token, response.refreshToken, variables.from);
      }
    },
    onError: (error) => {
      toast.error(getErrorMessage(error, 'Sign in failed. Please try again.'));
    },
  });

  const register = useMutation({
    mutationFn: async ({ fullName, email, password }: RegisterFormValues) => {
      const payload = { username: fullName, email, password };
      const { data } = await authService.register(payload);
      return data.data;
    },
    onSuccess: (response) => {
      toast.success('Account created! Check your email for the activation code.');
      navigate(APP_ROUTES.verifyOtp, {
        state: {
          purpose: 'registration',
          email: response.email,
          devOtp: response.devOtp,
          resendAfterSeconds: response.resendAfterSeconds,
          otpExpiryMinutes: response.otpExpiryMinutes,
        } satisfies VerifyOtpState,
      });
    },
    onError: (error) => {
      toast.error(getErrorMessage(error, 'Registration failed. Please try again.'));
    },
  });

  const verifyOtp = useMutation({
    mutationFn: async ({ purpose, code, email, challengeToken, rememberDevice }: VerifyOtpMutationVariables) => {
      const payload: VerifyOtpRequest = { email, challengeToken, code };
      if (purpose === '2fa') {
        const { data } = await authService.verifyTwoFactorLogin(
          { challengeToken: challengeToken ?? '', code },
          rememberDevice ?? false,
        );
        return data.data;
      }
      if (purpose === 'registration') {
        const { data } = await authService.verifyRegistration(payload, rememberDevice ?? false);
        return data.data;
      }
      const { data } = await authService.verifyLogin(payload, rememberDevice ?? false);
      return data.data;
    },
    onSuccess: (auth, variables) => {
      toast.success('Welcome to CloudNest!');
      applySession(auth.token, auth.refreshToken, variables.from);
    },
    onError: (error) => {
      toast.error(getErrorMessage(error, 'That code didn\'t work. Please try again.'));
    },
  });

  const resendOtp = useMutation({
    mutationFn: ({ email, challengeToken }: Pick<VerifyOtpMutationVariables, 'email' | 'challengeToken'>) =>
      authService.resendOtp({ email, challengeToken }),
  });

  function applySession(token: string, refreshToken: string | undefined, from?: string) {
    setAuthSession(token, refreshToken ?? null, null);
    setStatus('authenticated');

    // Redirect immediately for a snappy UX; hydrate the profile in the
    // background (the UI already handles `user === null`).
    navigate(from ?? APP_ROUTES.dashboard, { replace: true });

    void authService
      .getMe({ silent: true, skipAuthRedirect: true })
      .then(({ data }) => {
        // Guard against a logout racing the profile fetch.
        if (useAuthStore.getState().token === token) {
          useAuthStore.getState().setUser(data.data);
        }
      })
      .catch(() => {
        // Best-effort: the profile will hydrate on the next app load.
      });
  }

  return {
    loginMutation: login,
    registerMutation: register,
    verifyOtpMutation: verifyOtp,
    resendOtpMutation: resendOtp,
  };
}
