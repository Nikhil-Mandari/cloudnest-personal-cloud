/**
 * localStorage keys used by persisted zustand stores.
 *
 * NOTE: `theme` is also read by the inline script in index.html (pre-hydration
 * theme application) — keep the two in sync.
 */
export const STORAGE_KEYS = {
  auth: 'cloudnest-auth',
  theme: 'cloudnest-theme',
  ui: 'cloudnest-ui',
  files: 'cloudnest-files',
} as const;
