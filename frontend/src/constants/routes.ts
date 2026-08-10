/**
 * Centralised route paths. Import `APP_ROUTES` instead of hardcoding strings
 * so navigation and the router stay in sync.
 */
export const APP_ROUTES = {
  home: '/',
  login: '/login',
  register: '/register',
  verifyOtp: '/verify-otp',
  forgotPassword: '/forgot-password',
  /** Landing page for the Google/GitHub social-login redirect. */
  oauthCallback: '/oauth/callback',
  dashboard: '/dashboard',
  files: '/files',
  folders: '/folders',
  shared: '/shared',
  myShares: '/my-shares',
  trash: '/trash',
  analytics: '/analytics',
  auditLogs: '/audit-logs',
  profile: '/profile',
  settings: '/settings',
  security: '/security',
  about: '/about',
  notifications: '/notifications',
  /** Admin dashboard (ROLE_ADMIN only). */
  admin: '/admin',
  /** Public, unauthenticated share-link browse page. */
  publicShare: (token: string) => `/s/${token}`,
} as const;

export type AppRoute = (typeof APP_ROUTES)[keyof typeof APP_ROUTES];
