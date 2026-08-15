import { createBrowserRouter, Navigate } from 'react-router-dom';

import { APP_ROUTES } from '@/constants/routes';
import { AuthLayout } from '@/layouts/AuthLayout';
import { DashboardLayout } from '@/layouts/DashboardLayout';
import { NotFoundPage } from '@/pages/NotFoundPage';
import { GuestRoute } from './GuestRoute';
import {
  AboutPage,
  AnalyticsPage,
  DashboardPage,
  FilesPage,
  FoldersPage,
  ForgotPasswordPage,
  LoginPage,
  MySharesPage,
  NotificationsPage,
  OAuthCallbackPage,
  ProfilePage,
  PublicSharePage,
  RegisterPage,
  SecurityPage,
  SettingsPage,
  SharedPage,
  StoragePlansPage,
  TrashPage,
  VerifyOtpPage,
} from './pages';
import { ProtectedRoute } from './ProtectedRoute';

export const router = createBrowserRouter([
  { path: APP_ROUTES.home, element: <Navigate to={APP_ROUTES.dashboard} replace /> },

  {
    // Public share-link browse page — open to everyone, no auth.
    path: APP_ROUTES.publicShare(':token'),
    element: <PublicSharePage />,
  },

  {
    // Social-login landing — receives the session tokens after Google/GitHub
    // sign-in, then forwards to the dashboard (open to everyone, no auth).
    path: APP_ROUTES.oauthCallback,
    element: <OAuthCallbackPage />,
  },

  {
    // Public auth pages — redirect to /dashboard when already signed in.
    element: <GuestRoute />,
    children: [
      {
        element: <AuthLayout />,
        children: [
          { path: APP_ROUTES.login, element: <LoginPage /> },
          { path: APP_ROUTES.register, element: <RegisterPage /> },
          { path: APP_ROUTES.verifyOtp, element: <VerifyOtpPage /> },
          { path: APP_ROUTES.forgotPassword, element: <ForgotPasswordPage /> },
        ],
      },
    ],
  },

  {
    // Private app shell.
    element: <ProtectedRoute />,
    children: [
      {
        element: <DashboardLayout />,
        children: [
          { path: APP_ROUTES.dashboard, element: <DashboardPage /> },
          { path: APP_ROUTES.files, element: <FilesPage /> },
          { path: APP_ROUTES.folders, element: <FoldersPage /> },
          { path: APP_ROUTES.shared, element: <SharedPage /> },
          { path: APP_ROUTES.myShares, element: <MySharesPage /> },
          { path: APP_ROUTES.trash, element: <TrashPage /> },
          { path: APP_ROUTES.profile, element: <ProfilePage /> },
          { path: APP_ROUTES.settings, element: <SettingsPage /> },
          { path: APP_ROUTES.notifications, element: <NotificationsPage /> },
          { path: APP_ROUTES.analytics, element: <AnalyticsPage /> },
          { path: APP_ROUTES.plans, element: <StoragePlansPage /> },
          { path: APP_ROUTES.security, element: <SecurityPage /> },
          { path: APP_ROUTES.about, element: <AboutPage /> },
        ],
      },
    ],
  },

  { path: '*', element: <NotFoundPage /> },
]);
