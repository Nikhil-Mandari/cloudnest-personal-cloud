import { Link2, Lock, MousePointerClick, Settings2, Trash2 } from 'lucide-react';

import type { ShareRecord } from '@/types';
import { cn } from '@/utils/cn';
import { formatFileDate } from '@/utils/file';
import { isShareExpired } from '@/utils/share';
import { ExpiryBadge, PermissionBadge, ResourceBadge } from './ShareBadges';

export interface MySharesRowProps {
  share: ShareRecord;
  onCopyLink: (share: ShareRecord) => void;
  onOpenSettings: (share: ShareRecord) => void;
  onRevoke: (share: ShareRecord) => void;
}

/** Row for the My Shares management table (owner view with analytics). */
export function MySharesRow({ share, onCopyLink, onOpenSettings, onRevoke }: MySharesRowProps) {
  const expired = isShareExpired(share);
  const lastAccess = share.lastAccessedAt
    ? formatFileDate(share.lastAccessedAt)
    : '—';

  return (
    <tr
      className={cn(
        'group border-b border-gray-100 transition-colors last:border-0 dark:border-gray-800/70',
        expired && 'opacity-60',
      )}
    >
      {/* Resource */}
      <td className="py-2.5 pr-3">
        <div className="flex min-w-0 items-center">
          <ResourceBadge share={share} />
        </div>
      </td>

      {/* Permission */}
      <td className="py-2.5 pr-3">
        <PermissionBadge permission={share.permission} />
      </td>

      {/* Link status: password + expiry */}
      <td className="py-2.5 pr-3">
        <div className="flex flex-wrap items-center gap-1.5">
          {share.hasPassword && (
            <span
              title="Password protected"
              className="grid h-6 w-6 place-items-center rounded-md bg-amber-500/10 text-amber-600 dark:text-amber-400"
            >
              <Lock className="h-3.5 w-3.5" />
            </span>
          )}
          <ExpiryBadge share={share} />
        </div>
      </td>

      {/* Views */}
      <td className="hidden py-2.5 pr-3 md:table-cell">
        <span className="flex items-center gap-1.5 text-sm text-gray-600 tabular-nums dark:text-gray-300">
          <MousePointerClick className="h-3.5 w-3.5 text-gray-400" />
          {share.viewCount ?? 0}
        </span>
      </td>

      {/* Downloads */}
      <td className="hidden py-2.5 pr-3 lg:table-cell">
        <span className="flex items-center gap-1.5 text-sm text-gray-600 tabular-nums dark:text-gray-300">
          <Link2 className="h-3.5 w-3.5 text-gray-400" />
          {share.downloadCount ?? 0}
        </span>
      </td>

      {/* Last access */}
      <td className="hidden py-2.5 pr-3 text-sm text-gray-500 xl:table-cell dark:text-gray-400">
        {lastAccess}
      </td>

      {/* Shared on */}
      <td className="hidden py-2.5 pr-3 text-sm text-gray-500 2xl:table-cell dark:text-gray-400">
        {formatFileDate(share.createdAt)}
      </td>

      {/* Actions */}
      <td className="w-28 py-2.5 pr-4">
        <div className="flex items-center justify-end gap-0.5">
          <button
            type="button"
            onClick={() => onCopyLink(share)}
            aria-label="Copy share link"
            title="Copy share link"
            className="grid h-8 w-8 place-items-center rounded-lg text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-700 dark:text-gray-500 dark:hover:bg-gray-800 dark:hover:text-gray-200"
          >
            <Link2 className="h-4 w-4" />
          </button>
          <button
            type="button"
            onClick={() => onOpenSettings(share)}
            aria-label="Link settings"
            title="Link settings"
            className="grid h-8 w-8 place-items-center rounded-lg text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-700 dark:text-gray-500 dark:hover:bg-gray-800 dark:hover:text-gray-200"
          >
            <Settings2 className="h-4 w-4" />
          </button>
          <button
            type="button"
            onClick={() => onRevoke(share)}
            aria-label="Revoke share link"
            title="Revoke share link"
            className="grid h-8 w-8 place-items-center rounded-lg text-gray-400 transition-colors hover:bg-rose-50 hover:text-rose-600 dark:text-gray-500 dark:hover:bg-rose-500/10 dark:hover:text-rose-400"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      </td>
    </tr>
  );
}
