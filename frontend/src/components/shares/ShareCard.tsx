import { motion } from 'framer-motion';
import { Clock, Link2, User } from 'lucide-react';

import type { ShareRecord } from '@/types';
import { cn } from '@/utils/cn';
import { formatFileDate } from '@/utils/file';
import { isShareExpired } from '@/utils/share';
import { ExpiryBadge, PermissionBadge, ResourceBadge } from './ShareBadges';

export interface ShareCardProps {
  share: ShareRecord;
  onCopyLink: (share: ShareRecord) => void;
}

/** Grid tile for a single shared item. */
export function ShareCard({ share, onCopyLink }: ShareCardProps) {
  const expired = isShareExpired(share);

  return (
    <motion.div
      layout
      initial={{ opacity: 0, scale: 0.92 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0, scale: 0.92 }}
      transition={{ duration: 0.18, ease: 'easeOut' }}
      whileHover={{ y: -4 }}
      className={cn(
        'group relative flex flex-col rounded-2xl border border-gray-200 bg-white p-3 shadow-sm shadow-gray-900/[0.03]',
        'transition-shadow duration-200 hover:shadow-lg hover:shadow-gray-900/[0.06]',
        'dark:border-gray-800 dark:bg-gray-900',
        expired && 'opacity-70',
      )}
    >
      <div className="flex items-start justify-between gap-2">
        <ResourceBadge share={share} />
        <PermissionBadge permission={share.permission} />
      </div>

      <div className="mt-3 flex flex-col gap-1.5 border-t border-gray-100 pt-2.5 text-xs text-gray-500 dark:border-gray-800 dark:text-gray-400">
        <span className="flex items-center gap-1.5">
          <Clock className="h-3.5 w-3.5 shrink-0 text-gray-400" />
          Shared {formatFileDate(share.createdAt)}
        </span>
        <span className="flex items-center gap-1.5">
          <User className="h-3.5 w-3.5 shrink-0 text-gray-400" />
          Owner #{share.ownerId}
        </span>
        <ExpiryBadge share={share} />
      </div>

      <div className="mt-3 flex items-center justify-between border-t border-gray-100 pt-2.5 dark:border-gray-800">
        <span className="text-xs text-gray-400 dark:text-gray-500">
          {expired ? 'Link expired' : 'Link active'}
        </span>
        <button
          type="button"
          onClick={() => onCopyLink(share)}
          aria-label="Copy share link"
          title="Copy share link"
          className="grid h-7 w-7 place-items-center rounded-lg text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-700 dark:text-gray-500 dark:hover:bg-gray-800 dark:hover:text-gray-200"
        >
          <Link2 className="h-4 w-4" />
        </button>
      </div>
    </motion.div>
  );
}
