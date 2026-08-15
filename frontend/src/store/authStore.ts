import { create } from 'zustand';
import { persist } from 'zustand/middleware';

import { STORAGE_KEYS } from '@/constants/storage';
import { getDeviceId } from '@/utils/device';
import type { User } from '@/types';

export type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'unauthenticated';

interface AuthState {
  token: string | null;
  /** Rotating refresh token used by the Axios interceptor to recover 401s. */
  refreshToken: string | null;
  /** Stable device id sent as `X-Device-Id` for OTP / trusted-device flows. */
  deviceId: string;
  user: User | null;
  status: AuthStatus;

  /** Stores the JWT right after login so the Axios interceptor can use it. */
  setToken: (token: string) => void;
  /** Hydrates (or replaces) the signed-in user's profile. */
  setUser: (user: User) => void;
  setStatus: (status: AuthStatus) => void;
  /** Complete-session setter (token + profile) — backward compatible. */
  setAuth: (token: string, user: User | null) => void;
  /** Full auth session setter (access + refresh + profile). */
  setAuthSession: (token: string, refreshToken: string | null, user: User | null) => void;
  /** Refreshes the stored token pair after a rotation. */
  setTokenPair: (token: string, refreshToken: string | null) => void;
  logout: () => void;
}

/**
 * JWT auth store, persisted to localStorage (`cloudnest-auth`).
 * The access token is read directly by the Axios request interceptor, and
 * the refresh token by the 401-recovery interceptor.
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      refreshToken: null,
      deviceId: getDeviceId(),
      user: null,
      status: 'idle',

      setToken: (token) => set({ token }),
      setUser: (user) => set({ user }),
      setStatus: (status) => set({ status }),
      setAuth: (token, user) => set({ token, user, status: 'authenticated' }),
      setAuthSession: (token, refreshToken, user) =>
        set({ token, refreshToken, user, status: 'authenticated' }),
      setTokenPair: (token, refreshToken) => set({ token, refreshToken }),
      logout: () => set({ token: null, refreshToken: null, user: null, status: 'unauthenticated' }),
    }),
    {
      name: STORAGE_KEYS.auth,
      partialize: (state) => ({
        token: state.token,
        refreshToken: state.refreshToken,
        user: state.user,
      }),
      // After a refresh, reflect the persisted session in `status`.
      onRehydrateStorage: () => (state) => {
        if (state?.token) {
          state.setStatus('authenticated');
        }
      },
    },
  ),
);

/**
 * Selector: convenient for `useAuthStore(selectIsAuthenticated)`.
 * The JWT is the source of truth — the profile may hydrate asynchronously.
 */
export const selectIsAuthenticated = (state: AuthState): boolean => Boolean(state.token);
