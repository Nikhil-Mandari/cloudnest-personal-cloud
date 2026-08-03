/**
 * Centralised route paths. Import `APP_ROUTES` instead of hardcoding strings
 * so navigation and the router stay in sync.
 */
export const APP_ROUTES = {
  home: '/',
  login: '/login',
  register: '/register',
  dashboard: '/dashboard',
  files: '/files',
  folders: '/folders',
  shared: '/shared',
  trash: '/trash',
  profile: '/profile',
  settings: '/settings',
  notifications: '/notifications',
} as const;

export type AppRoute = (typeof APP_ROUTES)[keyof typeof APP_ROUTES];
