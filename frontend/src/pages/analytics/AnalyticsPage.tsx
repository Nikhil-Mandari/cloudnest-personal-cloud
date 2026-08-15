import { useMemo } from 'react';
import { motion } from 'framer-motion';
import {
  Archive,
  Database,
  FileText,
  FolderOpen,
  HardDrive,
  Image as ImageIcon,
  Music,
  Paperclip,
  Trash2,
  Video,
} from 'lucide-react';

import { ErrorState } from '@/components/common/ErrorState';
import { PageHeader } from '@/components/common/PageHeader';
import { Card } from '@/components/ui/Card';
import { useStorageOverviewQuery, deriveStorageSummary } from '@/hooks/useStorageAnalytics';
import type { FileTypeStat, LargestFileInfo } from '@/types';
import { cn } from '@/utils/cn';
import { getErrorMessage } from '@/utils/error';
import { formatBytes } from '@/utils/format';

const CATEGORY_META: Record<string, { icon: typeof Paperclip; className: string }> = {
  image: { icon: ImageIcon, className: 'bg-emerald-500/10 text-emerald-500' },
  video: { icon: Video, className: 'bg-rose-500/10 text-rose-500' },
  audio: { icon: Music, className: 'bg-violet-500/10 text-violet-500' },
  pdf: { icon: FileText, className: 'bg-red-500/10 text-red-500' },
  document: { icon: FileText, className: 'bg-sky-500/10 text-sky-500' },
  archive: { icon: Archive, className: 'bg-amber-500/10 text-amber-500' },
  code: { icon: Paperclip, className: 'bg-indigo-500/10 text-indigo-500' },
  other: { icon: Paperclip, className: 'bg-gray-500/10 text-gray-500' },
};

function categoryMeta(category: string) {
  return CATEGORY_META[category] ?? { icon: Paperclip, className: 'bg-gray-500/10 text-gray-500' };
}

/** Simple stacked bar chart for the weekly / monthly usage timelines. */
function UsageBars({
  points,
  label,
}: {
  points: Array<{ label: string; bytes: number }>;
  label: string;
}) {
  const max = Math.max(1, ...points.map((point) => point.bytes));
  return (
    <div className="space-y-1.5">
      {points.map((point, index) => (
        <div key={`${label}-${point.label}-${index}`} className="flex items-center gap-3">
          <span className="w-10 shrink-0 text-right text-[10px] text-gray-400 tabular-nums dark:text-gray-500">
            {point.label}
          </span>
          <div className="h-5 flex-1 overflow-hidden rounded-md bg-gray-100 dark:bg-gray-800">
            <motion.div
              initial={{ width: 0 }}
              animate={{ width: `${Math.max(2, (point.bytes / max) * 100)}%` }}
              transition={{ duration: 0.5, ease: 'easeOut' }}
              className="from-brand-500 to-accent-500 h-full rounded-md bg-linear-to-r"
            />
          </div>
          <span className="w-14 shrink-0 text-right text-[10px] text-gray-400 tabular-nums dark:text-gray-500">
            {formatBytes(point.bytes)}
          </span>
        </div>
      ))}
    </div>
  );
}

/** Storage analytics dashboard — usage, file types, largest files, trends. */
export function AnalyticsPage() {
  const { data: overview, isLoading, isError, error, refetch } = useStorageOverviewQuery();

  const summary = useMemo(
    () => deriveStorageSummary(overview?.storageUsed ?? 0),
    [overview?.storageUsed],
  );

  const lowStorage = summary.percentUsed >= 80;

  const statCards = [
    {
      label: 'Files',
      value: overview?.fileCount ?? 0,
      icon: Paperclip,
      className: 'bg-brand-500/10 text-brand-500',
    },
    {
      label: 'Folders',
      value: overview?.folderCount ?? 0,
      icon: FolderOpen,
      className: 'bg-sky-500/10 text-sky-500',
    },
    {
      label: 'In trash',
      value: overview?.trashFileCount ?? 0,
      icon: Trash2,
      className: 'bg-rose-500/10 text-rose-500',
    },
    {
      label: 'Trash size',
      value: overview?.trashSize ?? 0,
      icon: Archive,
      className: 'bg-amber-500/10 text-amber-500',
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Storage analytics"
        description="Track your storage usage, file composition and upload trends."
      />

      {isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {[0, 1, 2, 3].map((index) => (
            <div key={index} className="h-24 animate-pulse rounded-2xl bg-gray-100 dark:bg-gray-800" />
          ))}
        </div>
      ) : isError || !overview ? (
        <ErrorState
          message={getErrorMessage(error, 'Failed to load storage analytics.')}
          onRetry={() => void refetch()}
        />
      ) : (
        <>
          {/* Storage meter */}
          <Card className="p-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <span className="bg-brand-500/10 text-brand-500 grid h-11 w-11 place-items-center rounded-xl">
                  <HardDrive className="h-6 w-6" />
                </span>
                <div>
                  <h2 className="text-sm font-semibold text-gray-900 dark:text-white">
                    Storage usage
                  </h2>
                  <p className="text-xs text-gray-400 dark:text-gray-500">
                    {formatBytes(summary.used)} of {formatBytes(summary.quota)} used ·{' '}
                    {formatBytes(summary.remaining)} free
                  </p>
                </div>
              </div>
              <span
                className={cn(
                  'rounded-full px-3 py-1 text-sm font-semibold tabular-nums',
                  lowStorage
                    ? 'bg-rose-500/10 text-rose-600 dark:text-rose-400'
                    : 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400',
                )}
              >
                {summary.percentUsed.toFixed(1)}%
              </span>
            </div>
            <div className="mt-4 h-3 w-full overflow-hidden rounded-full bg-gray-100 dark:bg-gray-800">
              <motion.div
                initial={{ width: 0 }}
                animate={{ width: `${Math.min(100, summary.percentUsed)}%` }}
                transition={{ duration: 0.7, ease: 'easeOut' }}
                className={cn(
                  'h-full rounded-full',
                  lowStorage
                    ? 'bg-linear-to-r from-amber-500 to-rose-500'
                    : 'from-brand-500 to-accent-500 bg-linear-to-r',
                )}
              />
            </div>
            {lowStorage && (
              <p className="mt-3 flex items-center gap-1.5 text-xs font-medium text-rose-600 dark:text-rose-400">
                <Database className="h-3.5 w-3.5" /> You have used {summary.percentUsed.toFixed(0)}%
                of your storage — consider cleaning up your trash.
              </p>
            )}
          </Card>

          {/* Stat cards */}
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {statCards.map((stat) => {
              const Icon = stat.icon;
              return (
                <Card key={stat.label} className="flex items-center gap-3 p-4">
                  <span className={cn('grid h-10 w-10 place-items-center rounded-xl', stat.className)}>
                    <Icon className="h-5 w-5" />
                  </span>
                  <div>
                    <p className="text-lg font-semibold text-gray-900 tabular-nums dark:text-white">
                      {stat.label === 'Trash size' ? formatBytes(stat.value) : stat.value}
                    </p>
                    <p className="text-xs text-gray-400 dark:text-gray-500">{stat.label}</p>
                  </div>
                </Card>
              );
            })}
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            {/* File types */}
            <Card className="p-5">
              <h3 className="mb-4 text-sm font-semibold text-gray-900 dark:text-white">
                Files by type
              </h3>
              <div className="space-y-3">
                {(overview.fileTypeStats ?? []).length === 0 ? (
                  <p className="text-sm text-gray-400 dark:text-gray-500">No files yet.</p>
                ) : (
                  [...(overview.fileTypeStats ?? [])]
                    .sort((a, b) => b.bytes - a.bytes)
                    .map((stat: FileTypeStat) => {
                      const meta = categoryMeta(stat.category);
                      const Icon = meta.icon;
                      const percent =
                        (stat.bytes / Math.max(1, summary.used)) * 100;
                      return (
                        <div key={stat.category} className="flex items-center gap-3">
                          <span className={cn('grid h-8 w-8 shrink-0 place-items-center rounded-lg', meta.className)}>
                            <Icon className="h-4 w-4" />
                          </span>
                          <div className="min-w-0 flex-1">
                            <div className="flex items-center justify-between text-xs">
                              <span className="font-medium text-gray-700 capitalize dark:text-gray-200">
                                {stat.category}
                                <span className="ml-1.5 text-gray-400 dark:text-gray-500">
                                  {stat.count} file{stat.count === 1 ? '' : 's'}
                                </span>
                              </span>
                              <span className="text-gray-400 tabular-nums dark:text-gray-500">
                                {formatBytes(stat.bytes)} · {percent.toFixed(1)}%
                              </span>
                            </div>
                            <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-gray-100 dark:bg-gray-800">
                              <motion.div
                                initial={{ width: 0 }}
                                animate={{ width: `${percent}%` }}
                                transition={{ duration: 0.5, ease: 'easeOut' }}
                                className="from-brand-500 to-accent-500 h-full rounded-full bg-linear-to-r"
                              />
                            </div>
                          </div>
                        </div>
                      );
                    })
                )}
              </div>
            </Card>

            {/* Usage timeline */}
            <Card className="p-5">
              <h3 className="mb-4 text-sm font-semibold text-gray-900 dark:text-white">
                Weekly uploads
              </h3>
              <UsageBars points={overview.weeklyUsage ?? []} label="week" />
              <h3 className="mt-6 mb-4 text-sm font-semibold text-gray-900 dark:text-white">
                Monthly uploads
              </h3>
              <UsageBars points={overview.monthlyUsage ?? []} label="month" />
            </Card>
          </div>

          {/* Largest files */}
          <Card className="p-5">
            <h3 className="mb-4 text-sm font-semibold text-gray-900 dark:text-white">
              Largest files
            </h3>
            {(overview.largestFiles ?? []).length === 0 ? (
              <p className="text-sm text-gray-400 dark:text-gray-500">No files yet.</p>
            ) : (
              <div className="space-y-2">
                {(overview.largestFiles ?? []).map((file: LargestFileInfo, index: number) => {
                  const Icon = categoryMeta(file.fileType ?? 'other').icon;
                  return (
                    <div
                      key={file.id}
                      className="flex items-center gap-3 rounded-xl px-2 py-1.5 transition-colors hover:bg-gray-50 dark:hover:bg-gray-800/50"
                    >
                      <span className="w-5 text-right text-xs text-gray-300 tabular-nums dark:text-gray-600">
                        {index + 1}
                      </span>
                      <span className="bg-gray-500/10 text-gray-500 grid h-8 w-8 shrink-0 place-items-center rounded-lg">
                        <Icon className="h-4 w-4" />
                      </span>
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium text-gray-900 dark:text-white">
                          {file.originalFileName}
                        </p>
                        <p className="text-xs text-gray-400 dark:text-gray-500">
                          {file.uploadedAt
                            ? new Date(file.uploadedAt).toLocaleDateString()
                            : '—'}
                        </p>
                      </div>
                      <span className="text-sm text-gray-500 tabular-nums dark:text-gray-400">
                        {formatBytes(file.fileSize)}
                      </span>
                    </div>
                  );
                })}
              </div>
            )}
          </Card>
        </>
      )}
    </div>
  );
}
