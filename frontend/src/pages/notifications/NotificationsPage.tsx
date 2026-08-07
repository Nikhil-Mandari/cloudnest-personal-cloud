import { useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { Bell, CheckCheck } from 'lucide-react';

import { ErrorState } from '@/components/common/ErrorState';
import { PageHeader } from '@/components/common/PageHeader';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { NotificationEmptyState } from '@/components/notifications/NotificationEmptyState';
import { NotificationItem } from '@/components/notifications/NotificationItem';
import { NotificationSkeletons } from '@/components/notifications/NotificationSkeletons';
import { useNotificationMutations, useNotificationsQuery } from '@/hooks/useNotifications';
import type { AppNotification } from '@/types';
import { cn } from '@/utils/cn';

type NotificationFilter = 'all' | 'unread';

interface NotificationGroup {
  label: string;
  items: AppNotification[];
}

const DAY_MS = 86_400_000;

/** Buckets a (newest-first) list into Today / Yesterday / Earlier groups. */
function groupByDay(items: AppNotification[]): NotificationGroup[] {
  const now = new Date();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const startOfYesterday = startOfToday - DAY_MS;

  const groups: NotificationGroup[] = [];
  let current: NotificationGroup | null = null;

  for (const notification of items) {
    const time = new Date(notification.createdAt).getTime();
    const label = Number.isNaN(time)
      ? 'Earlier'
      : time >= startOfToday
        ? 'Today'
        : time >= startOfYesterday
          ? 'Yesterday'
          : 'Earlier';

    if (!current || current.label !== label) {
      current = { label, items: [] };
      groups.push(current);
    }
    current.items.push(notification);
  }

  return groups;
}

const FILTERS: ReadonlyArray<{ value: NotificationFilter; label: string }> = [
  { value: 'all', label: 'All' },
  { value: 'unread', label: 'Unread' },
];

export function NotificationsPage() {
  const [filter, setFilter] = useState<NotificationFilter>('all');

  const { data: notifications, isLoading, isError, refetch } = useNotificationsQuery();
  const { markAsRead, markAllAsRead, deleteNotification } = useNotificationMutations();

  const unreadCount = notifications?.filter((n) => !n.isRead).length ?? 0;
  const visible =
    notifications?.filter((n) => (filter === 'unread' ? !n.isRead : true)) ?? [];

  const isMutating = markAsRead.isPending || deleteNotification.isPending;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Notifications"
        description="Shares, alerts and account activity, all in one place."
      />

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-gray-100 px-5 py-3.5 dark:border-gray-800">
          <div
            role="group"
            aria-label="Filter notifications"
            className="flex overflow-hidden rounded-lg border border-gray-300 bg-white shadow-sm dark:border-gray-700 dark:bg-gray-900"
          >
            {FILTERS.map(({ value, label }) => (
              <button
                key={value}
                type="button"
                onClick={() => setFilter(value)}
                aria-pressed={filter === value}
                className={cn(
                  'h-9 px-4 text-sm font-medium transition-colors',
                  filter === value
                    ? 'bg-brand-500/10 text-brand-600 dark:text-brand-400'
                    : 'text-gray-500 hover:bg-gray-50 hover:text-gray-700 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-gray-200',
                )}
              >
                {label}
                {value === 'unread' && unreadCount > 0 && (
                  <span className="bg-brand-500 ml-1.5 inline-grid h-4 min-w-4 place-items-center rounded-full px-1 text-[10px] font-bold text-white">
                    {unreadCount}
                  </span>
                )}
              </button>
            ))}
          </div>

          <Button
            variant="outline"
            size="sm"
            className="ml-auto"
            leftIcon={<CheckCheck className="h-3.5 w-3.5" />}
            disabled={unreadCount === 0 || markAllAsRead.isPending}
            isLoading={markAllAsRead.isPending}
            onClick={() => markAllAsRead.mutate()}
          >
            Mark all as read
          </Button>
        </div>

        {isLoading ? (
          <NotificationSkeletons />
        ) : isError ? (
          <div className="p-5">
            <ErrorState
              title="Couldn't load notifications"
              message="Your notifications couldn't be fetched right now."
              onRetry={() => void refetch()}
            />
          </div>
        ) : notifications && notifications.length === 0 ? (
          <div className="p-5">
            <NotificationEmptyState />
          </div>
        ) : visible.length === 0 ? (
          <div className="p-5">
            <NotificationEmptyState filtered />
          </div>
        ) : (
          <div className="max-h-[32rem] overflow-y-auto">
            {groupByDay(visible).map((group) => (
              <section key={group.label} aria-label={group.label}>
                <p className="sticky top-0 border-b border-gray-100 bg-white/95 px-5 py-2 text-xs font-semibold tracking-wide text-gray-400 uppercase backdrop-blur-sm dark:border-gray-800 dark:bg-gray-900/95 dark:text-gray-500">
                  {group.label}
                </p>
                <AnimatePresence initial={false}>
                  {group.items.map((notification) => (
                    <NotificationItem
                      key={notification.id}
                      notification={notification}
                      isMutating={isMutating}
                      onMarkRead={(id) => markAsRead.mutate(id)}
                      onDelete={(id) => deleteNotification.mutate(id)}
                    />
                  ))}
                </AnimatePresence>
              </section>
            ))}
          </div>
        )}
      </Card>

      <motion.p
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="flex items-center gap-1.5 text-xs text-gray-400 dark:text-gray-500"
      >
        <Bell className="h-3.5 w-3.5" />
        Notifications refresh automatically every 30 seconds.
      </motion.p>
    </div>
  );
}
