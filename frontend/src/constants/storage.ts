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
  folders: 'cloudnest-folders',
  recentSearches: 'cloudnest-recent-searches',
  /** Stable per-browser device id used for OTP / trusted-device flows. */
  deviceId: 'cloudnest-device-id',
} as const;
