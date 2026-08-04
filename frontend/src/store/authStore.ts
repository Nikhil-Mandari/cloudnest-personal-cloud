import { create } from 'zustand';
import { persist } from 'zustand/middleware';

import { STORAGE_KEYS } from '@/constants/storage';
import type { User } from '@/types';

export type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'unauthenticated';

interface AuthState {
  token: string | null;
  user: User | null;
  status: AuthStatus;

  /** Stores the JWT right after login so the Axios interceptor can use it. */
  setToken: (token: string) => void;
  /** Hydrates (or replaces) the signed-in user's profile. */
  setUser: (user: User) => void;
  setStatus: (status: AuthStatus) => void;
  /** Complete-session setter (token + profile). */
  setAuth: (token: string, user: User | null) => void;
  logout: () => void;
}

/**
 * JWT auth store, persisted to localStorage (`cloudnest-auth`).
 * The token is read directly by the Axios request interceptor.
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      status: 'idle',

      setToken: (token) => set({ token }),
      setUser: (user) => set({ user }),
      setStatus: (status) => set({ status }),
      setAuth: (token, user) => set({ token, user, status: 'authenticated' }),
      logout: () => set({ token: null, user: null, status: 'unauthenticated' }),
    }),
    {
      name: STORAGE_KEYS.auth,
      partialize: (state) => ({ token: state.token, user: state.user }),
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
