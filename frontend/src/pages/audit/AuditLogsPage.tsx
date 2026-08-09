import { useMemo, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import {
  Archive,
  ChevronLeft,
  ChevronRight,
  CopyX,
  Download,
  Eraser,
  Eye,
  FolderInput,
  History,
  Pencil,
  RefreshCw,
  RotateCcw,
  ScrollText,
  Share2,
  Star,
  StarOff,
  Trash2,
  UploadCloud,
  type LucideIcon,
} from 'lucide-react';

import { ErrorState } from '@/components/common/ErrorState';
import { PageHeader } from '@/components/common/PageHeader';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { useAuditLogsQuery } from '@/hooks/useAuditLogs';
import { fileService } from '@/services/file.service';
import type { AuditLogEntry } from '@/types';
import { cn } from '@/utils/cn';
import { getErrorMessage } from '@/utils/error';
import { formatRelativeTime } from '@/utils/format';

const ACTION_META: Record<string, { label: string; icon: LucideIcon; className: string }> = {
  UPLOAD: { label: 'Uploaded', icon: UploadCloud, className: 'bg-brand-500/10 text-brand-500' },
  UPLOAD_REPLACED: { label: 'Replaced content', icon: RefreshCw, className: 'bg-brand-500/10 text-brand-500' },
  UPLOAD_DUPLICATE_SKIPPED: { label: 'Duplicate skipped', icon: CopyX, className: 'bg-gray-500/10 text-gray-500' },
  DOWNLOAD: { label: 'Downloaded', icon: Download, className: 'bg-sky-500/10 text-sky-500' },
  SHARE_DOWNLOAD: { label: 'Shared download', icon: Share2, className: 'bg-sky-500/10 text-sky-500' },
  PREVIEW: { label: 'Previewed', icon: Eye, className: 'bg-violet-500/10 text-violet-500' },
  RENAME: { label: 'Renamed', icon: Pencil, className: 'bg-amber-500/10 text-amber-500' },
  MOVE: { label: 'Moved', icon: FolderInput, className: 'bg-amber-500/10 text-amber-500' },
  DELETE: { label: 'Deleted', icon: Trash2, className: 'bg-rose-500/10 text-rose-500' },
  RESTORE: { label: 'Restored', icon: RotateCcw, className: 'bg-emerald-500/10 text-emerald-500' },
  PERMANENT_DELETE: { label: 'Permanently deleted', icon: Trash2, className: 'bg-rose-500/10 text-rose-500' },
  EMPTY_TRASH: { label: 'Emptied trash', icon: Eraser, className: 'bg-rose-500/10 text-rose-500' },
  FAVORITE_ADD: { label: 'Added to favorites', icon: Star, className: 'bg-amber-500/10 text-amber-500' },
  FAVORITE_REMOVE: { label: 'Removed from favorites', icon: StarOff, className: 'bg-amber-500/10 text-amber-500' },
  VERSION_UPLOAD: { label: 'Version uploaded', icon: History, className: 'bg-indigo-500/10 text-indigo-500' },
  VERSION_RESTORE: { label: 'Version restored', icon: History, className: 'bg-indigo-500/10 text-indigo-500' },
  VERSION_DELETE: { label: 'Version deleted', icon: History, className: 'bg-indigo-500/10 text-indigo-500' },
  ZIP_DOWNLOAD: { label: 'Bulk ZIP download', icon: Archive, className: 'bg-sky-500/10 text-sky-500' },
};

const ALL_ACTIONS = fileService.auditActions();

function actionMeta(action: string) {
  return (
    ACTION_META[action] ?? { label: action, icon: ScrollText, className: 'bg-gray-500/10 text-gray-500' }
  );
}

const PAGE_SIZE = 25;

/** Immutable audit trail of every file-management action, filterable. */
export function AuditLogsPage() {
  const [page, setPage] = useState(0);
  const [action, setAction] = useState<string | undefined>(undefined);

  const { data, isLoading, isError, error, refetch } = useAuditLogsQuery(page, PAGE_SIZE, action);

  const entries = useMemo(() => data?.content ?? [], [data]);
  const totalPages = Math.max(1, data?.totalPages ?? 1);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Audit logs"
        description="A complete timeline of every file action in your account."
      />

      {/* Action filter chips */}
      <div className="flex flex-wrap gap-1.5">
        <button
          type="button"
          onClick={() => {
            setAction(undefined);
            setPage(0);
          }}
          className={cn(
            'rounded-full px-3 py-1.5 text-xs font-medium transition-colors',
            action === undefined
              ? 'bg-brand-600 text-white shadow-sm'
              : 'bg-gray-100 text-gray-600 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-700',
          )}
        >
          All
        </button>
        {ALL_ACTIONS.map((item) => (
          <button
            key={item}
            type="button"
            onClick={() => {
              setAction(item === action ? undefined : item);
              setPage(0);
            }}
            className={cn(
              'rounded-full px-3 py-1.5 text-xs font-medium transition-colors',
              action === item
                ? 'bg-brand-600 text-white shadow-sm'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-700',
            )}
          >
            {item.replaceAll('_', ' ').toLowerCase()}
          </button>
        ))}
      </div>

      <Card className="p-2">
        {isLoading ? (
          <div className="space-y-2 p-3">
            {[0, 1, 2, 3, 4].map((index) => (
              <div key={index} className="h-14 animate-pulse rounded-xl bg-gray-100 dark:bg-gray-800" />
            ))}
          </div>
        ) : isError ? (
          <div className="p-4">
            <ErrorState
              message={getErrorMessage(error, 'Failed to load audit logs.')}
              onRetry={() => void refetch()}
            />
          </div>
        ) : entries.length === 0 ? (
          <div className="flex flex-col items-center gap-2 py-14 text-center">
            <ScrollText className="h-8 w-8 text-gray-300 dark:text-gray-600" />
            <p className="text-sm text-gray-500 dark:text-gray-400">
              No audit entries match this filter yet.
            </p>
          </div>
        ) : (
          <motion.ul layout className="space-y-0.5">
            <AnimatePresence initial={false}>
              {entries.map((entry: AuditLogEntry) => {
                const meta = actionMeta(entry.action);
                const Icon = meta.icon;
                return (
                  <motion.li
                    key={entry.id}
                    layout
                    initial={{ opacity: 0, y: 6 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="flex items-center gap-3 rounded-xl px-3 py-2.5 transition-colors hover:bg-gray-50 dark:hover:bg-gray-800/50"
                  >
                    <span className={cn('grid h-9 w-9 shrink-0 place-items-center rounded-lg', meta.className)}>
                      <Icon className="h-4 w-4" />
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-gray-900 dark:text-white">
                        {meta.label}
                        {entry.resourceName ? (
                          <span className="text-gray-400 dark:text-gray-500">
                            {' '}
                            — {entry.resourceName}
                          </span>
                        ) : null}
                      </p>
                      <p className="truncate text-xs text-gray-400 dark:text-gray-500">
                        {entry.details ?? entry.action}
                        {entry.ipAddress ? ` · ${entry.ipAddress}` : ''}
                      </p>
                    </div>
                    <span
                      className="shrink-0 text-xs text-gray-400 tabular-nums dark:text-gray-500"
                      title={new Date(entry.createdAt).toLocaleString()}
                    >
                      {formatRelativeTime(entry.createdAt)}
                    </span>
                  </motion.li>
                );
              })}
            </AnimatePresence>
          </motion.ul>
        )}

        {/* Pagination */}
        {!isLoading && !isError && (
          <div className="flex items-center justify-between border-t border-gray-100 px-3 py-3 dark:border-gray-800">
            <p className="text-xs text-gray-400 tabular-nums dark:text-gray-500">
              Page {page + 1} of {totalPages} · {data?.totalElements ?? 0} entries
            </p>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((current) => Math.max(0, current - 1))}
                leftIcon={<ChevronLeft className="h-4 w-4" />}
              >
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((current) => current + 1)}
              >
                Next
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}
