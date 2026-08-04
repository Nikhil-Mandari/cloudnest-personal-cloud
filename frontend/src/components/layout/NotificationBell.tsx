import { Link } from 'react-router-dom';
import { Bell } from 'lucide-react';

import { APP_ROUTES } from '@/constants/routes';

export function NotificationBell() {
  return (
    <Link
      to={APP_ROUTES.notifications}
      aria-label="Notifications"
      className="relative grid h-10 w-10 place-items-center rounded-lg text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-900 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-white"
    >
      <Bell className="h-5 w-5" />
      {/* Unread indicator — will reflect real notification count once wired up */}
      <span className="absolute top-2.5 right-2.5 h-2 w-2 rounded-full bg-rose-500 ring-2 ring-white dark:ring-gray-950" />
    </Link>
  );
}
