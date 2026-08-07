import {
  Ban,
  Bell,
  FileUp,
  FolderUp,
  Inbox,
  KeyRound,
  Lock,
  RefreshCw,
  RotateCcw,
  ShieldAlert,
  ShieldCheck,
  type LucideIcon,
} from 'lucide-react';

import type { NotificationType } from '@/types';
import { cn } from '@/utils/cn';

const TYPE_STYLES: Record<NotificationType, { icon: LucideIcon; className: string }> = {
  SHARE_RECEIVED: {
    icon: Inbox,
    className: 'bg-sky-500/10 text-sky-600 dark:text-sky-400',
  },
  SHARE_UPDATED: {
    icon: RefreshCw,
    className: 'bg-amber-500/10 text-amber-600 dark:text-amber-400',
  },
  SHARE_REVOKED: {
    icon: Ban,
    className: 'bg-rose-500/10 text-rose-600 dark:text-rose-400',
  },
  FILE_SHARED: {
    icon: FileUp,
    className: 'bg-violet-500/10 text-violet-600 dark:text-violet-400',
  },
  FOLDER_SHARED: {
    icon: FolderUp,
    className: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400',
  },
  SYSTEM: {
    icon: Bell,
    className: 'bg-gray-500/10 text-gray-600 dark:text-gray-400',
  },
  // ── Phase 5: security events ─────────────────────────────────────────────
  LOGIN_ALERT: {
    icon: ShieldCheck,
    className: 'bg-indigo-500/10 text-indigo-600 dark:text-indigo-400',
  },
  UNKNOWN_DEVICE_LOGIN: {
    icon: ShieldAlert,
    className: 'bg-amber-500/10 text-amber-600 dark:text-amber-400',
  },
  PASSWORD_CHANGED: {
    icon: KeyRound,
    className: 'bg-violet-500/10 text-violet-600 dark:text-violet-400',
  },
  PASSWORD_RESET: {
    icon: RotateCcw,
    className: 'bg-teal-500/10 text-teal-600 dark:text-teal-400',
  },
  ACCOUNT_LOCKED: {
    icon: Lock,
    className: 'bg-rose-500/10 text-rose-600 dark:text-rose-400',
  },
};

export interface NotificationIconProps {
  type: NotificationType;
  className?: string;
}

/** Colored, type-specific icon chip for a notification. */
export function NotificationIcon({ type, className }: NotificationIconProps) {
  const { icon: Icon, className: chipClassName } = TYPE_STYLES[type] ?? TYPE_STYLES.SYSTEM;

  return (
    <span
      className={cn(
        'grid h-10 w-10 shrink-0 place-items-center rounded-xl',
        chipClassName,
        className,
      )}
    >
      <Icon className="h-5 w-5" />
    </span>
  );
}
