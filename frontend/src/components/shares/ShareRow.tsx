import { Link2 } from 'lucide-react';

import type { ShareRecord } from '@/types';
import { cn } from '@/utils/cn';
import { formatFileDate } from '@/utils/file';
import { isShareExpired } from '@/utils/share';
import { ExpiryBadge, PermissionBadge, ResourceBadge } from './ShareBadges';

export interface ShareRowProps {
  share: ShareRecord;
  onCopyLink: (share: ShareRecord) => void;
}

/** Table row for a single shared item. */
export function ShareRow({ share, onCopyLink }: ShareRowProps) {
  const expired = isShareExpired(share);

  return (
    <tr
      className={cn(
        'group border-b border-gray-100 transition-colors last:border-0 dark:border-gray-800/70',
        expired && 'opacity-60',
      )}
    >
      {/* Type + resource */}
      <td className="py-2.5 pr-3">
        <div className="flex min-w-0 items-center">
          <ResourceBadge share={share} />
        </div>
      </td>

      {/* Permission */}
      <td className="py-2.5 pr-3">
        <PermissionBadge permission={share.permission} />
      </td>

      {/* Shared on */}
      <td className="hidden py-2.5 pr-3 text-sm text-gray-500 md:table-cell dark:text-gray-400">
        {formatFileDate(share.createdAt)}
      </td>

      {/* Expires */}
      <td className="hidden py-2.5 pr-3 lg:table-cell">
        <ExpiryBadge share={share} />
      </td>

      {/* Owner */}
      <td className="hidden py-2.5 pr-3 text-sm text-gray-500 xl:table-cell dark:text-gray-400">
        #{share.ownerId}
      </td>

      {/* Actions */}
      <td className="w-20 py-2.5 pr-4">
        <div className="flex items-center justify-end">
          <button
            type="button"
            onClick={() => onCopyLink(share)}
            aria-label="Copy share link"
            title="Copy share link"
            className="grid h-7 w-7 place-items-center rounded-lg text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-700 md:opacity-0 md:group-hover:opacity-100 dark:text-gray-500 dark:hover:bg-gray-800 dark:hover:text-gray-200"
          >
            <Link2 className="h-4 w-4" />
          </button>
        </div>
      </td>
    </tr>
  );
}
