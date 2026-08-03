import { lazy } from 'react';

/**
 * Lazily-loaded page components (route-based code splitting).
 * Kept in their own module so fast-refresh only sees component exports.
 */
export const LoginPage = lazy(() =>
  import('@/pages/auth/LoginPage').then((m) => ({ default: m.LoginPage })),
);
export const RegisterPage = lazy(() =>
  import('@/pages/auth/RegisterPage').then((m) => ({ default: m.RegisterPage })),
);
export const DashboardPage = lazy(() =>
  import('@/pages/dashboard/DashboardPage').then((m) => ({ default: m.DashboardPage })),
);
export const FilesPage = lazy(() =>
  import('@/pages/files/FilesPage').then((m) => ({ default: m.FilesPage })),
);
export const FoldersPage = lazy(() =>
  import('@/pages/folders/FoldersPage').then((m) => ({ default: m.FoldersPage })),
);
export const SharedPage = lazy(() =>
  import('@/pages/share/SharedPage').then((m) => ({ default: m.SharedPage })),
);
export const TrashPage = lazy(() =>
  import('@/pages/trash/TrashPage').then((m) => ({ default: m.TrashPage })),
);
export const ProfilePage = lazy(() =>
  import('@/pages/profile/ProfilePage').then((m) => ({ default: m.ProfilePage })),
);
export const SettingsPage = lazy(() =>
  import('@/pages/settings/SettingsPage').then((m) => ({ default: m.SettingsPage })),
);
export const NotificationsPage = lazy(() =>
  import('@/pages/notifications/NotificationsPage').then((m) => ({ default: m.NotificationsPage })),
);
