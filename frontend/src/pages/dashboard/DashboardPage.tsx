import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  Clock,
  Files,
  FolderOpen,
  HardDrive,
  Share2,
  type LucideIcon,
} from 'lucide-react';

import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { PageHeader } from '@/components/common/PageHeader';
import { FileIcon } from '@/components/files/FileIcon';
import { Button } from '@/components/ui/Button';
import { Card, CardHeader } from '@/components/ui/Card';
import { APP_ROUTES } from '@/constants/routes';
import { useAuth } from '@/hooks/useAuth';
import { useFilesQuery } from '@/hooks/useFiles';
import { useFoldersQuery } from '@/hooks/useFolders';
import { useMySharesQuery } from '@/hooks/useShare';
import { useTrashQuery } from '@/hooks/useTrash';
import { formatFileDate } from '@/utils/file';
import { formatBytes } from '@/utils/format';
import { cn } from '@/utils/cn';

const RECENT_FILES_LIMIT = 6;

interface StatCardData {
  label: string;
  value: string;
  sub: string;
  icon: LucideIcon;
  href: string;
}

export function DashboardPage() {
  const { user } = useAuth();
  const firstName = (user?.displayName ?? user?.username ?? '').split(' ')[0] || 'there';

  const filesQuery = useFilesQuery();
  const foldersQuery = useFoldersQuery();
  const trashQuery = useTrashQuery();
  const sharesQuery = useMySharesQuery();

  const files = useMemo(() => filesQuery.data ?? [], [filesQuery.data]);
  const folders = useMemo(() => foldersQuery.data ?? [], [foldersQuery.data]);
  const trashFiles = useMemo(() => trashQuery.data?.files ?? [], [trashQuery.data]);
  const trashFolders = useMemo(() => trashQuery.data?.folders ?? [], [trashQuery.data]);
  const myShares = useMemo(() => sharesQuery.data ?? [], [sharesQuery.data]);

  // Real, API-derived statistics.
  const activeBytes = useMemo(
    () => files.reduce((sum, file) => sum + file.fileSize, 0),
    [files],
  );
  const trashBytes = useMemo(
    () => trashFiles.reduce((sum, file) => sum + file.fileSize, 0),
    [trashFiles],
  );
  const totalBytes = activeBytes + trashBytes;

  const recentFiles = useMemo(
    () =>
      [...files]
        .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
        .slice(0, RECENT_FILES_LIMIT),
    [files],
  );

  const stats: StatCardData[] = [
    {
      label: 'Total Files',
      value: files.length.toLocaleString(),
      sub: trashFiles.length > 0 ? `${trashFiles.length} in trash` : 'active files',
      icon: Files,
      href: APP_ROUTES.files,
    },
    {
      label: 'Folders',
      value: folders.length.toLocaleString(),
      sub: trashFolders.length > 0 ? `${trashFolders.length} in trash` : 'your folders',
      icon: FolderOpen,
      href: APP_ROUTES.folders,
    },
    {
      label: 'Shared Links',
      value: myShares.length.toLocaleString(),
      sub: 'links you created',
      icon: Share2,
      href: APP_ROUTES.shared,
    },
    {
      label: 'Storage Used',
      value: formatBytes(totalBytes),
      sub: `${files.length} active file${files.length === 1 ? '' : 's'}`,
      icon: HardDrive,
      href: APP_ROUTES.files,
    },
  ];

  const isLoading =
    filesQuery.isLoading ||
    foldersQuery.isLoading ||
    trashQuery.isLoading ||
    sharesQuery.isLoading;

  const isError = filesQuery.isError || foldersQuery.isError;

  // Ring proportions (active vs trash share of stored data) — real, derived.
  const activePct = totalBytes > 0 ? (activeBytes / totalBytes) * 100 : 0;
  const trashPct = totalBytes > 0 ? (trashBytes / totalBytes) * 100 : 0;

  const RING_RADIUS = 52;
  const RING_CIRCUMFERENCE = 2 * Math.PI * RING_RADIUS;
  const activeOffset = RING_CIRCUMFERENCE * (1 - activePct / 100);
  const trashOffset = RING_CIRCUMFERENCE * (1 - (activePct + trashPct) / 100);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Dashboard"
        description={`Welcome back, ${firstName} 👋 Here's what's happening with your cloud.`}
      />

      {/* Stat cards */}
      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <div
              key={index}
              className="h-32 animate-pulse rounded-2xl border border-gray-200/80 bg-white p-5 dark:border-gray-800 dark:bg-gray-900"
            />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {stats.map((stat, index) => (
            <motion.div
              key={stat.label}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.06, duration: 0.3 }}
              whileHover={{ y: -4 }}
            >
              <Link to={stat.href} className="block">
                <Card className="p-5 transition-shadow duration-200 hover:shadow-lg hover:shadow-gray-900/[0.06]">
                  <div className="flex items-center justify-between">
                    <div className="bg-brand-500/10 text-brand-600 dark:bg-brand-400/10 dark:text-brand-300 grid h-11 w-11 place-items-center rounded-xl">
                      <stat.icon className="h-5 w-5" />
                    </div>
                    <span className="text-xs font-medium text-gray-400 dark:text-gray-500">
                      {stat.sub}
                    </span>
                  </div>
                  <p className="mt-4 text-2xl font-bold tracking-tight text-gray-900 dark:text-white">
                    {stat.value}
                  </p>
                  <p className="mt-0.5 text-sm text-gray-500 dark:text-gray-400">{stat.label}</p>
                </Card>
              </Link>
            </motion.div>
          ))}
        </div>
      )}

      {/* Recent files + storage overview */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.12, duration: 0.3 }}
          className="lg:col-span-2"
        >
          <Card>
            <CardHeader
              title="Recent files"
              description="Your latest uploads, straight from the API."
              action={
                files.length > 0 ? (
                  <Link
                    to={APP_ROUTES.files}
                    className="text-brand-600 hover:text-brand-700 dark:text-brand-400 text-sm font-medium transition-colors hover:underline"
                  >
                    View all
                  </Link>
                ) : undefined
              }
            />
            {isError ? (
              <div className="px-6 py-5">
                <ErrorState
                  title="Couldn't load your cloud"
                  message="Your dashboard data couldn't be fetched right now."
                  onRetry={() => {
                    void filesQuery.refetch();
                    void foldersQuery.refetch();
                  }}
                />
              </div>
            ) : isLoading ? (
              <div className="divide-y divide-gray-100 dark:divide-gray-800">
                {Array.from({ length: 4 }).map((_, index) => (
                  <div key={index} className="flex animate-pulse items-center gap-3.5 px-6 py-3">
                    <div className="h-10 w-10 rounded-xl bg-gray-100 dark:bg-gray-800" />
                    <div className="flex-1 space-y-2">
                      <div className="h-3 w-1/2 rounded-full bg-gray-100 dark:bg-gray-800" />
                      <div className="h-2.5 w-16 rounded-full bg-gray-100 dark:bg-gray-800" />
                    </div>
                    <div className="h-2.5 w-14 rounded-full bg-gray-100 dark:bg-gray-800" />
                  </div>
                ))}
              </div>
            ) : recentFiles.length === 0 ? (
              <div className="px-6 py-5">
                <EmptyState
                  icon={<Clock className="h-6 w-6" />}
                  title="No files yet"
                  description="Upload your first file and it will show up here."
                  action={
                    <Link to={APP_ROUTES.files}>
                      <Button leftIcon={<Files className="h-4 w-4" />}>Upload files</Button>
                    </Link>
                  }
                />
              </div>
            ) : (
              <ul className="divide-y divide-gray-100 dark:divide-gray-800">
                {recentFiles.map((file, index) => (
                  <motion.li
                    key={file.id}
                    initial={{ opacity: 0, y: 6 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.18, delay: Math.min(index * 0.04, 0.25) }}
                  >
                    <Link
                      to={APP_ROUTES.files}
                      className="group flex items-center gap-3.5 px-6 py-3 transition-colors hover:bg-gray-50 dark:hover:bg-gray-800/40"
                    >
                      <FileIcon file={file} size="md" />
                      <div className="min-w-0 flex-1">
                        <p
                          title={file.originalFileName}
                          className="truncate text-sm font-medium text-gray-900 group-hover:text-brand-600 dark:text-white dark:group-hover:text-brand-400"
                        >
                          {file.originalFileName}
                        </p>
                        <p className="text-xs text-gray-400 dark:text-gray-500">
                          {formatBytes(file.fileSize)}
                        </p>
                      </div>
                      <span className="shrink-0 text-xs text-gray-400 dark:text-gray-500">
                        {formatFileDate(file.createdAt)}
                      </span>
                    </Link>
                  </motion.li>
                ))}
              </ul>
            )}
          </Card>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.18, duration: 0.3 }}
        >
          <Card>
            <CardHeader title="Storage" description="Usage across your cloud." />
            <div className="px-6 py-5">
              {isLoading ? (
                <div className="space-y-4 animate-pulse">
                  <div className="h-8 w-24 rounded-full bg-gray-100 dark:bg-gray-800" />
                  <div className="h-2.5 w-full rounded-full bg-gray-100 dark:bg-gray-800" />
                  <div className="h-2.5 w-2/3 rounded-full bg-gray-100 dark:bg-gray-800" />
                </div>
              ) : (
                <>
                  <div className="relative mx-auto h-44 w-44">
                    <svg viewBox="0 0 120 120" className="h-full w-full -rotate-90" aria-hidden="true">
                      {/* Track */}
                      <circle
                        cx="60"
                        cy="60"
                        r={RING_RADIUS}
                        fill="none"
                        strokeWidth="12"
                        className="stroke-gray-100 dark:stroke-gray-800"
                      />
                      {/* Trash share (sits after the active arc) */}
                      {trashPct > 0 && (
                        <motion.circle
                          cx="60"
                          cy="60"
                          r={RING_RADIUS}
                          fill="none"
                          strokeWidth="12"
                          strokeLinecap="round"
                          strokeDasharray={`${RING_CIRCUMFERENCE} ${RING_CIRCUMFERENCE}`}
                          initial={{ strokeDashoffset: RING_CIRCUMFERENCE }}
                          animate={{ strokeDashoffset: trashOffset }}
                          transition={{ duration: 0.9, ease: 'easeOut' }}
                          className="stroke-gray-300 dark:stroke-gray-600"
                        />
                      )}
                      {/* Active share */}
                      <motion.circle
                        cx="60"
                        cy="60"
                        r={RING_RADIUS}
                        fill="none"
                        strokeWidth="12"
                        strokeLinecap="round"
                        stroke="currentColor"
                        strokeDasharray={`${RING_CIRCUMFERENCE} ${RING_CIRCUMFERENCE}`}
                        initial={{ strokeDashoffset: RING_CIRCUMFERENCE }}
                        animate={{ strokeDashoffset: activeOffset }}
                        transition={{ duration: 0.9, ease: 'easeOut' }}
                        className="text-brand-500 dark:text-brand-400"
                      />
                    </svg>
                    <div className="absolute inset-0 flex flex-col items-center justify-center">
                      <p className="text-2xl font-bold tracking-tight text-gray-900 dark:text-white">
                        {Math.round(activePct)}%
                      </p>
                      <p className="text-[11px] font-medium text-gray-400 dark:text-gray-500">
                        active files
                      </p>
                    </div>
                  </div>

                  <p className="mt-4 text-center text-2xl font-bold tracking-tight text-gray-900 dark:text-white">
                    {formatBytes(totalBytes)}
                  </p>
                  <p className="text-center text-sm text-gray-500 dark:text-gray-400">
                    stored in your cloud
                  </p>

              <ul className="mt-5 space-y-2.5 text-sm">
                <li className="flex items-center justify-between">
                  <span className="flex items-center gap-2 text-gray-600 dark:text-gray-300">
                    <span className="from-brand-500 to-accent-500 h-2.5 w-2.5 rounded-full bg-linear-to-r" />
                    Active files
                  </span>
                  <span className="font-medium text-gray-900 dark:text-white">
                    {files.length} · {formatBytes(activeBytes)}
                  </span>
                </li>
                <li className="flex items-center justify-between">
                  <span className="flex items-center gap-2 text-gray-600 dark:text-gray-300">
                    <span className="h-2.5 w-2.5 rounded-full bg-gray-300 dark:bg-gray-600" />
                    In trash
                  </span>
                  <span className="font-medium text-gray-900 dark:text-white">
                    {trashFiles.length} · {formatBytes(trashBytes)}
                  </span>
                </li>
                <li className="flex items-center justify-between">
                  <span className="flex items-center gap-2 text-gray-600 dark:text-gray-300">
                    <span className="bg-amber-500/80 h-2.5 w-2.5 rounded-full" />
                    Folders
                  </span>
                  <span className="font-medium text-gray-900 dark:text-white">
                    {folders.length}
                  </span>
                </li>
              </ul>

                  <p
                    className={cn(
                      'mt-5 rounded-lg bg-gray-50 px-3 py-2 text-xs text-gray-500 dark:bg-gray-800/60 dark:text-gray-400',
                      totalBytes === 0 && 'text-center',
                    )}
                  >
                    {totalBytes > 0
                      ? 'The ring shows the share of stored data held by active files vs. the trash.'
                      : 'Upload files to start tracking your storage.'}
                  </p>
                </>
              )}
            </div>
          </Card>
        </motion.div>
      </div>
    </div>
  );
}
