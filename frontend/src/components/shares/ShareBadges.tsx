import { FileText, FolderOpen } from 'lucide-react';

import type { SharePermission, ShareRecord, ShareResourceType } from '@/types';
import { cn } from '@/utils/cn';
import { isShareExpired } from '@/utils/share';

/** Calendar-style date for an expiry ("Aug 12" / "Jan 5, 2027"). */
function formatExpiryDate(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return '—';
  }
  const options: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric' };
  if (date.getFullYear() !== new Date().getFullYear()) {
    options.year = 'numeric';
  }
  return new Intl.DateTimeFormat(undefined, options).format(date);
}

const PERMISSION_LABELS: Record<SharePermission, string> = {
  VIEW: 'Can view',
  DOWNLOAD: 'Download only',
  EDIT: 'Can edit',
};

const PERMISSION_CLASSES: Record<SharePermission, string> = {
  VIEW: 'bg-sky-500/10 text-sky-600 dark:text-sky-400',
  DOWNLOAD: 'bg-violet-500/10 text-violet-600 dark:text-violet-400',
  EDIT: 'bg-amber-500/10 text-amber-600 dark:text-amber-400',
};

/** Badge showing the permission granted on a share. */
export function PermissionBadge({ permission }: { permission: SharePermission }) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
        PERMISSION_CLASSES[permission],
      )}
    >
      {PERMISSION_LABELS[permission]}
    </span>
  );
}

/** Badge showing whether a share has expired. */
export function ExpiryBadge({ share }: { share: ShareRecord }) {
  if (!share.expiryDate) {
    return <span className="text-xs text-gray-400 dark:text-gray-500">Never expires</span>;
  }
  if (isShareExpired(share)) {
    return (
      <span className="inline-flex items-center rounded-full bg-rose-500/10 px-2.5 py-0.5 text-xs font-medium text-rose-600 dark:text-rose-400">
        Expired
      </span>
    );
  }
  return (
    <span className="text-xs text-gray-500 dark:text-gray-400">
      Expires {formatExpiryDate(share.expiryDate)}
    </span>
  );
}

const TYPE_LABELS: Record<ShareResourceType, string> = {
  FILE: 'Shared file',
  FOLDER: 'Shared folder',
};

/**
 * Icon + label for a share's resource. Shows the real file/folder name when
 * the backend resolved one, falling back to a generic type label, with the
 * resource ID as a muted subtitle.
 */
export function ResourceBadge({ share }: { share: ShareRecord }) {
  const Icon = share.resourceType === 'FILE' ? FileText : FolderOpen;
  const label = share.resourceName || TYPE_LABELS[share.resourceType];
  return (
    <span className="flex min-w-0 items-center gap-2.5">
      <span
        className={cn(
          'grid h-8 w-8 shrink-0 place-items-center rounded-lg',
          share.resourceType === 'FILE'
            ? 'bg-brand-500/10 text-brand-600 dark:text-brand-400'
            : 'bg-amber-500/10 text-amber-600 dark:text-amber-400',
        )}
      >
        <Icon className="h-4 w-4" />
      </span>
      <span className="min-w-0">
        <span
          title={label}
          className="block truncate text-sm font-medium text-gray-900 dark:text-white"
        >
          {label}
        </span>
        <span
          title={share.resourceId}
          className="block truncate font-mono text-[11px] text-gray-400 dark:text-gray-500"
        >
          {share.resourceId}
        </span>
      </span>
    </span>
  );
}
