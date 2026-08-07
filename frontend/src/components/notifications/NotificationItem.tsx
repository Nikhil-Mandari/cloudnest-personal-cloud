import { motion } from 'framer-motion';
import { MailOpen, Trash2 } from 'lucide-react';

import type { AppNotification } from '@/types';
import { cn } from '@/utils/cn';
import { formatFileDate } from '@/utils/file';
import { formatRelativeTime } from '@/utils/format';
import { NotificationIcon } from './NotificationIcon';

export interface NotificationItemProps {
  notification: AppNotification;
  /** Marks a single notification as read (no-op when already read). */
  onMarkRead: (id: number) => void;
  onDelete: (id: number) => void;
  /** Per-action busy states to disable double clicks. */
  isMutating?: boolean;
}

/** A single notification row with hover actions. */
export function NotificationItem({
  notification,
  onMarkRead,
  onDelete,
  isMutating = false,
}: NotificationItemProps) {
  const unread = !notification.isRead;

  return (
    <motion.li
      layout
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, height: 0 }}
      transition={{ duration: 0.16, ease: 'easeOut' }}
      className={cn(
        'group flex items-start gap-3.5 border-b border-gray-100 px-5 py-4 transition-colors last:border-0 dark:border-gray-800',
        unread ? 'bg-brand-500/[0.04] dark:bg-brand-400/[0.04]' : 'hover:bg-gray-50 dark:hover:bg-gray-800/40',
      )}
    >
      <NotificationIcon type={notification.type} />

      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <p
            className={cn(
              'truncate text-sm',
              unread
                ? 'font-semibold text-gray-900 dark:text-white'
                : 'font-medium text-gray-600 dark:text-gray-300',
            )}
          >
            {notification.title}
          </p>
          {unread && (
            <span
              aria-label="Unread"
              className="bg-brand-500 h-2 w-2 shrink-0 rounded-full"
              title="Unread"
            />
          )}
        </div>
        {notification.message && (
          <p className="mt-0.5 line-clamp-2 text-sm text-gray-500 dark:text-gray-400">
            {notification.message}
          </p>
        )}
        <p
          className="mt-1 text-xs text-gray-400 dark:text-gray-500"
          title={formatFileDate(notification.createdAt)}
        >
          {formatRelativeTime(notification.createdAt)}
        </p>
      </div>

      <div className="flex shrink-0 items-center gap-1 opacity-0 transition-opacity group-hover:opacity-100 group-focus-within:opacity-100">
        {unread && (
          <button
            type="button"
            onClick={() => onMarkRead(notification.id)}
            disabled={isMutating}
            aria-label="Mark as read"
            title="Mark as read"
            className="grid h-8 w-8 place-items-center rounded-lg text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-700 disabled:opacity-50 dark:text-gray-500 dark:hover:bg-gray-800 dark:hover:text-gray-200"
          >
            <MailOpen className="h-4 w-4" />
          </button>
        )}
        <button
          type="button"
          onClick={() => onDelete(notification.id)}
          disabled={isMutating}
          aria-label="Delete notification"
          title="Delete"
          className="grid h-8 w-8 place-items-center rounded-lg text-gray-400 transition-colors hover:bg-rose-50 hover:text-rose-600 disabled:opacity-50 dark:text-gray-500 dark:hover:bg-rose-500/10 dark:hover:text-rose-400"
        >
          <Trash2 className="h-4 w-4" />
        </button>
      </div>
    </motion.li>
  );
}
