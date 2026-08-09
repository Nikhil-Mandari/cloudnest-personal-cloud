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
export const VerifyOtpPage = lazy(() =>
  import('@/pages/auth/VerifyOtpPage').then((m) => ({ default: m.VerifyOtpPage })),
);
export const ForgotPasswordPage = lazy(() =>
  import('@/pages/auth/ForgotPasswordPage').then((m) => ({ default: m.ForgotPasswordPage })),
);
export const PublicSharePage = lazy(() =>
  import('@/pages/share/PublicSharePage').then((m) => ({ default: m.PublicSharePage })),
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
export const MySharesPage = lazy(() =>
  import('@/pages/share/MySharesPage').then((m) => ({ default: m.MySharesPage })),
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
export const AnalyticsPage = lazy(() =>
  import('@/pages/analytics/AnalyticsPage').then((m) => ({ default: m.AnalyticsPage })),
);
export const AuditLogsPage = lazy(() =>
  import('@/pages/audit/AuditLogsPage').then((m) => ({ default: m.AuditLogsPage })),
);
export const SecurityPage = lazy(() =>
  import('@/pages/security/SecurityPage').then((m) => ({ default: m.SecurityPage })),
);
export const AboutPage = lazy(() =>
  import('@/pages/about/AboutPage').then((m) => ({ default: m.AboutPage })),
);
export const NotificationsPage = lazy(() =>
  import('@/pages/notifications/NotificationsPage').then((m) => ({ default: m.NotificationsPage })),
);
