import { Link } from 'react-router-dom';
import { Bell } from 'lucide-react';

import { APP_ROUTES } from '@/constants/routes';
import { useUnreadCountQuery } from '@/hooks/useNotifications';

export function NotificationBell() {
  const { data: unreadCount } = useUnreadCountQuery();
  const count = unreadCount ?? 0;

  return (
    <Link
      to={APP_ROUTES.notifications}
      aria-label={count > 0 ? `${count} unread notifications` : 'Notifications'}
      className="relative grid h-10 w-10 place-items-center rounded-lg text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-900 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-white"
    >
      <Bell className="h-5 w-5" />
      {count > 0 && (
        <span className="absolute -top-0.5 -right-0.5 grid h-[1.125rem] min-w-[1.125rem] place-items-center rounded-full bg-rose-500 px-1 text-[10px] font-bold text-white ring-2 ring-white dark:ring-gray-950">
          {count > 99 ? '99+' : count}
        </span>
      )}
    </Link>
  );
}
