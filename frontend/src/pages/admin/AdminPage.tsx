import { useMemo, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import {
  Activity,
  Archive,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  CopyX,
  Database,
  Download,
  Eraser,
  Eye,
  FileText,
  FolderInput,
  HardDrive,
  History,
  Image as ImageIcon,
  Lock,
  LogIn,
  Mail,
  MonitorSmartphone,
  Music,
  Paperclip,
  Pencil,
  RefreshCw,
  RotateCcw,
  ScrollText,
  Search,
  Server,
  Settings2,
  Share2,
  ShieldAlert,
  ShieldCheck,
  Star,
  StarOff,
  Trash2,
  UploadCloud,
  UserCheck,
  UserX,
  Users,
  Video,
  XCircle,
  type LucideIcon,
} from 'lucide-react';

import { ErrorState } from '@/components/common/ErrorState';
import { PageHeader } from '@/components/common/PageHeader';
import { Avatar } from '@/components/common/Avatar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { useAdminAuditLogs, useAdminLoginHistory, useAdminMinioStatus, useAdminSecurityLogs, useAdminSecurityOverview, useAdminStorageOverview, useAdminSystemHealth, useAdminUserMutations, useAdminUsers, useAdminUserSummary } from '@/hooks/useAdmin';
import { fileService } from '@/services/file.service';
import type { AdminTab, AuditLogEntry, LoginHistoryEntry, SecurityLogEntry } from '@/types';
import { cn } from '@/utils/cn';
import { getErrorMessage } from '@/utils/error';
import { formatBytes, formatRelativeTime } from '@/utils/format';
import { isAdminRole } from '@/utils/role';

const TABS: Array<{ key: AdminTab; label: string; icon: LucideIcon }> = [
  { key: 'overview', label: 'Overview', icon: Activity },
  { key: 'users', label: 'Users', icon: Users },
  { key: 'storage', label: 'Storage', icon: HardDrive },
  { key: 'audit', label: 'Audit', icon: ScrollText },
  { key: 'security', label: 'Security', icon: ShieldCheck },
  { key: 'system', label: 'System', icon: Server },
];

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

const AUDIT_ACTION_META: Record<string, { label: string; icon: LucideIcon; className: string }> = {
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

const PAGE_SIZE = 25;

interface StatCardProps {
  label: string;
  value: string | number;
  icon: LucideIcon;
  className: string;
}

function StatCard({ label, value, icon: Icon, className }: StatCardProps) {
  return (
    <Card className="flex items-center gap-3 p-4">
      <span className={cn('grid h-10 w-10 shrink-0 place-items-center rounded-xl', className)}>
        <Icon className="h-5 w-5" />
      </span>
      <div className="min-w-0">
        <p className="truncate text-lg leading-tight font-semibold text-gray-900 tabular-nums dark:text-white">
          {value}
        </p>
        <p className="truncate text-xs text-gray-400 dark:text-gray-500">{label}</p>
      </div>
    </Card>
  );
}

function CardSkeleton({ className = 'h-24' }: { className?: string }) {
  return <div className={cn('animate-pulse rounded-2xl bg-gray-100 dark:bg-gray-800', className)} />;
}

function Pagination({
  page,
  totalPages,
  totalElements,
  onPage,
}: {
  page: number;
  totalPages: number;
  totalElements?: number;
  onPage: (page: number) => void;
}) {
  const safePages = Math.max(1, totalPages);
  return (
    <div className="flex items-center justify-between border-t border-gray-100 px-3 py-3 dark:border-gray-800">
      <p className="text-xs text-gray-400 tabular-nums dark:text-gray-500">
        Page {page + 1} of {safePages}
        {totalElements !== undefined ? ` · ${totalElements.toLocaleString()} entries` : ''}
      </p>
      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          size="sm"
          disabled={page === 0}
          onClick={() => onPage(Math.max(0, page - 1))}
          leftIcon={<ChevronLeft className="h-4 w-4" />}
        >
          Previous
        </Button>
        <Button
          variant="outline"
          size="sm"
          disabled={page >= safePages - 1}
          onClick={() => onPage(page + 1)}
        >
          Next
          <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}

/** Tabbed admin dashboard: platform-wide users, storage, audit, security and system health. */
export function AdminPage() {
  const [tab, setTab] = useState<AdminTab>('overview');

  return (
    <div className="space-y-6">
      <PageHeader
        title="Admin dashboard"
        description="Platform-wide users, storage, security, audit and system health."
      />

      {/* Tab bar */}
      <div className="flex flex-wrap gap-1.5 rounded-2xl border border-gray-200/80 bg-white p-1.5 shadow-sm dark:border-gray-800 dark:bg-gray-900">
        {TABS.map((item) => {
          const Icon = item.icon;
          const active = tab === item.key;
          return (
            <button
              key={item.key}
              type="button"
              onClick={() => setTab(item.key)}
              className={cn(
                'flex flex-1 items-center justify-center gap-2 rounded-xl px-3 py-2 text-sm font-medium whitespace-nowrap transition-colors',
                active
                  ? 'bg-brand-600 text-white shadow-sm'
                  : 'text-gray-500 hover:bg-gray-100 hover:text-gray-900 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-white',
              )}
            >
              <Icon className="h-4 w-4" />
              <span className="hidden sm:inline">{item.label}</span>
            </button>
          );
        })}
      </div>

      <AnimatePresence mode="wait">
        <motion.div
          key={tab}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -8 }}
          transition={{ duration: 0.18 }}
        >
          {tab === 'overview' && <OverviewTab />}
          {tab === 'users' && <UsersTab />}
          {tab === 'storage' && <StorageTab />}
          {tab === 'audit' && <AuditTab />}
          {tab === 'security' && <SecurityTab />}
          {tab === 'system' && <SystemTab />}
        </motion.div>
      </AnimatePresence>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Overview tab
// ─────────────────────────────────────────────────────────────────────────────
function OverviewTab() {
  const users = useAdminUserSummary();
  const security = useAdminSecurityOverview();
  const storage = useAdminStorageOverview();
  const health = useAdminSystemHealth();

  const loading = users.isLoading || security.isLoading || storage.isLoading;
  const failed = users.isError || security.isError || storage.isError;

  if (failed) {
    return (
      <ErrorState
        message="Some admin views failed to load."
        onRetry={() => {
          void users.refetch();
          void security.refetch();
          void storage.refetch();
        }}
      />
    );
  }

  if (loading || !users.data || !security.data || !storage.data) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 8 }).map((_, index) => (
          <CardSkeleton key={index} />
        ))}
      </div>
    );
  }

  const userCards = [
    { label: 'Total users', value: users.data.totalUsers, icon: Users, className: 'bg-brand-500/10 text-brand-500' },
    { label: 'Active', value: users.data.activeUsers, icon: UserCheck, className: 'bg-emerald-500/10 text-emerald-500' },
    { label: 'Disabled', value: users.data.disabledUsers, icon: UserX, className: 'bg-rose-500/10 text-rose-500' },
    { label: 'Admins', value: users.data.adminUsers, icon: ShieldCheck, className: 'bg-violet-500/10 text-violet-500' },
    { label: 'New (7d)', value: users.data.newLast7Days, icon: UserCheck, className: 'bg-sky-500/10 text-sky-500' },
  ];

  const securityCards = [
    { label: 'Total accounts', value: security.data.totalAccounts, icon: Users, className: 'bg-brand-500/10 text-brand-500' },
    { label: 'Locked', value: security.data.lockedAccounts, icon: Lock, className: 'bg-amber-500/10 text-amber-500' },
    { label: 'Pending verify', value: security.data.pendingVerification, icon: Mail, className: 'bg-sky-500/10 text-sky-500' },
    { label: 'Total logins', value: security.data.totalLogins, icon: LogIn, className: 'bg-emerald-500/10 text-emerald-500' },
    { label: 'Failed (7d)', value: security.data.failedLoginsLast7Days, icon: ShieldAlert, className: 'bg-rose-500/10 text-rose-500' },
    { label: 'Active sessions', value: security.data.activeSessions, icon: MonitorSmartphone, className: 'bg-violet-500/10 text-violet-500' },
  ];

  const storageCards = [
    { label: 'Files', value: storage.data.totalFiles, icon: Paperclip, className: 'bg-brand-500/10 text-brand-500' },
    { label: 'Storage used', value: formatBytes(storage.data.totalBytes), icon: HardDrive, className: 'bg-sky-500/10 text-sky-500' },
    { label: 'In trash', value: storage.data.trashFileCount, icon: Trash2, className: 'bg-rose-500/10 text-rose-500' },
    { label: 'Trash size', value: formatBytes(storage.data.trashSize), icon: Archive, className: 'bg-amber-500/10 text-amber-500' },
  ];

  const healthy = health.data?.healthyCount ?? 0;
  const total = health.data?.totalCount ?? 0;

  return (
    <div className="space-y-6">
      <div>
        <h2 className="mb-3 text-sm font-semibold text-gray-900 dark:text-white">Users</h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          {userCards.map((card) => (
            <StatCard key={card.label} {...card} />
          ))}
        </div>
      </div>

      <div>
        <h2 className="mb-3 text-sm font-semibold text-gray-900 dark:text-white">Security</h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {securityCards.map((card) => (
            <StatCard key={card.label} {...card} />
          ))}
          <Card className="flex items-center gap-3 p-4">
            <span className="bg-gray-500/10 text-gray-500 grid h-10 w-10 shrink-0 place-items-center rounded-xl">
              <Server className="h-5 w-5" />
            </span>
            <div className="min-w-0">
              <p className="truncate text-lg leading-tight font-semibold text-gray-900 tabular-nums dark:text-white">
                {healthy}/{total}
              </p>
              <p className="truncate text-xs text-gray-400 dark:text-gray-500">
                Services healthy
              </p>
            </div>
          </Card>
        </div>
      </div>

      <div>
        <h2 className="mb-3 text-sm font-semibold text-gray-900 dark:text-white">Storage</h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {storageCards.map((card) => (
            <StatCard key={card.label} {...card} />
          ))}
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Users tab
// ─────────────────────────────────────────────────────────────────────────────
function UsersTab() {
  const [page, setPage] = useState(0);
  const [query, setQuery] = useState('');
  const [search, setSearch] = useState('');
  const { data, isLoading, isError, error, refetch } = useAdminUsers(page, PAGE_SIZE, search);
  const { setEnabled, setRole } = useAdminUserMutations();

  const applySearch = () => {
    setSearch(query.trim());
    setPage(0);
  };

  return (
    <Card>
      {/* Search + refresh */}
      <div className="flex flex-wrap items-center gap-2 border-b border-gray-100 p-4 dark:border-gray-800">
        <div className="relative min-w-0 flex-1">
          <Search className="pointer-events-none absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            onKeyDown={(event) => event.key === 'Enter' && applySearch()}
            placeholder="Search by name, username or email…"
            className="w-full rounded-lg border border-gray-300 bg-white py-2 pr-3 pl-9 text-sm text-gray-900 shadow-sm transition-colors placeholder:text-gray-400 focus:border-brand-500 focus:ring-2 focus:ring-brand-500/25 focus:outline-none dark:border-gray-700 dark:bg-gray-950 dark:text-white dark:placeholder:text-gray-500"
          />
        </div>
        <Button variant="outline" size="sm" onClick={applySearch}>
          Search
        </Button>
        <Button variant="outline" size="sm" leftIcon={<RefreshCw className="h-4 w-4" />} onClick={() => void refetch()}>
          Refresh
        </Button>
      </div>

      {isLoading ? (
        <div className="space-y-2 p-4">
          {Array.from({ length: 5 }).map((_, index) => (
            <CardSkeleton key={index} className="h-14" />
          ))}
        </div>
      ) : isError ? (
        <div className="p-4">
          <ErrorState
            message={getErrorMessage(error, 'Failed to load users.')}
            onRetry={() => void refetch()}
          />
        </div>
      ) : (data?.content.length ?? 0) === 0 ? (
        <div className="flex flex-col items-center gap-2 py-14 text-center">
          <Users className="h-8 w-8 text-gray-300 dark:text-gray-600" />
          <p className="text-sm text-gray-500 dark:text-gray-400">No users match this filter.</p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-gray-100 text-xs tracking-wide text-gray-400 uppercase dark:border-gray-800 dark:text-gray-500">
                <th className="px-4 py-3 font-semibold">User</th>
                <th className="hidden px-4 py-3 font-semibold md:table-cell">Email</th>
                <th className="px-4 py-3 font-semibold">Role</th>
                <th className="hidden px-4 py-3 font-semibold sm:table-cell">Status</th>
                <th className="hidden px-4 py-3 font-semibold lg:table-cell">Joined</th>
                <th className="px-4 py-3 text-right font-semibold">Actions</th>
              </tr>
            </thead>
            <tbody>
              {data?.content.map((user) => {
                const admin = isAdminRole(user.role);
                return (
                  <tr
                    key={user.id}
                    className="border-b border-gray-50 transition-colors last:border-0 hover:bg-gray-50/70 dark:border-gray-800/50 dark:hover:bg-gray-800/40"
                  >
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <Avatar name={user.displayName ?? user.username} avatarUrl={user.avatarUrl} size="sm" />
                        <div className="min-w-0">
                          <p className="truncate font-medium text-gray-900 dark:text-white">
                            {user.displayName ?? user.username}
                          </p>
                          <p className="truncate text-xs text-gray-400 dark:text-gray-500">
                            @{user.username}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="hidden px-4 py-3 text-gray-500 md:table-cell dark:text-gray-400">
                      {user.email}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={cn(
                          'inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold',
                          admin
                            ? 'bg-violet-500/10 text-violet-600 dark:text-violet-400'
                            : 'bg-gray-500/10 text-gray-600 dark:text-gray-400',
                        )}
                      >
                        {admin && <ShieldCheck className="h-3 w-3" />}
                        {admin ? 'Admin' : 'User'}
                      </span>
                    </td>
                    <td className="hidden px-4 py-3 sm:table-cell">
                      <span
                        className={cn(
                          'inline-flex items-center gap-1 text-xs font-medium',
                          user.enabled
                            ? 'text-emerald-600 dark:text-emerald-400'
                            : 'text-rose-600 dark:text-rose-400',
                        )}
                      >
                        {user.enabled ? (
                          <CheckCircle2 className="h-3.5 w-3.5" />
                        ) : (
                          <XCircle className="h-3.5 w-3.5" />
                        )}
                        {user.enabled ? 'Active' : 'Disabled'}
                      </span>
                    </td>
                    <td className="hidden px-4 py-3 text-xs text-gray-400 lg:table-cell dark:text-gray-500">
                      {formatRelativeTime(user.createdAt)}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1.5">
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={setRole.isPending}
                          isLoading={setRole.isPending && setRole.variables?.id === Number(user.id)}
                          onClick={() =>
                            setRole.mutate({
                              id: Number(user.id),
                              role: admin ? 'ROLE_USER' : 'ROLE_ADMIN',
                            })
                          }
                        >
                          {admin ? 'Demote' : 'Promote'}
                        </Button>
                        <Button
                          variant={user.enabled ? 'danger' : 'outline'}
                          size="sm"
                          disabled={setEnabled.isPending}
                          isLoading={setEnabled.isPending && setEnabled.variables?.id === Number(user.id)}
                          onClick={() =>
                            setEnabled.mutate({ id: Number(user.id), enabled: !user.enabled })
                          }
                        >
                          {user.enabled ? 'Disable' : 'Enable'}
                        </Button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {!isLoading && !isError && data && (
        <Pagination
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          onPage={setPage}
        />
      )}
    </Card>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Storage tab
// ─────────────────────────────────────────────────────────────────────────────
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

function StorageTab() {
  const { data, isLoading, isError, error, refetch } = useAdminStorageOverview();

  if (isLoading || !data) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, index) => (
          <CardSkeleton key={index} />
        ))}
      </div>
    );
  }

  if (isError) {
    return (
      <ErrorState
        message={getErrorMessage(error, 'Failed to load storage overview.')}
        onRetry={() => void refetch()}
      />
    );
  }

  const cards = [
    { label: 'Users with files', value: data.totalUsers, icon: Users, className: 'bg-brand-500/10 text-brand-500' },
    { label: 'Total files', value: data.totalFiles, icon: Paperclip, className: 'bg-sky-500/10 text-sky-500' },
    { label: 'Storage used', value: formatBytes(data.totalBytes), icon: HardDrive, className: 'bg-emerald-500/10 text-emerald-500' },
    { label: 'In trash', value: data.trashFileCount, icon: Trash2, className: 'bg-rose-500/10 text-rose-500' },
  ];

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {cards.map((card) => (
          <StatCard key={card.label} {...card} />
        ))}
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        {/* File types */}
        <Card className="p-5">
          <h3 className="mb-4 text-sm font-semibold text-gray-900 dark:text-white">Files by type</h3>
          {(data.fileTypeStats ?? []).length === 0 ? (
            <p className="text-sm text-gray-400 dark:text-gray-500">No files yet.</p>
          ) : (
            <div className="space-y-3">
              {[...(data.fileTypeStats ?? [])]
                .sort((a, b) => b.bytes - a.bytes)
                .map((stat) => {
                  const meta = CATEGORY_META[stat.category] ?? CATEGORY_META.other;
                  const Icon = meta.icon;
                  const percent = (stat.bytes / Math.max(1, data.totalBytes)) * 100;
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
                })}
            </div>
          )}
        </Card>

        {/* Usage timeline */}
        <Card className="p-5">
          <h3 className="mb-4 text-sm font-semibold text-gray-900 dark:text-white">Weekly uploads</h3>
          <UsageBars points={data.weeklyUsage ?? []} label="week" />
          <h3 className="mt-6 mb-4 text-sm font-semibold text-gray-900 dark:text-white">Monthly uploads</h3>
          <UsageBars points={data.monthlyUsage ?? []} label="month" />
        </Card>
      </div>

      {/* Largest files */}
      <Card className="p-5">
        <h3 className="mb-4 text-sm font-semibold text-gray-900 dark:text-white">Largest files</h3>
        {(data.largestFiles ?? []).length === 0 ? (
          <p className="text-sm text-gray-400 dark:text-gray-500">No files yet.</p>
        ) : (
          <div className="space-y-2">
            {(data.largestFiles ?? []).map((file, index) => {
              const Icon = (CATEGORY_META[file.fileType ?? 'other'] ?? CATEGORY_META.other).icon;
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
                      {file.uploadedAt ? new Date(file.uploadedAt).toLocaleDateString() : '—'}
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
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Audit tab
// ─────────────────────────────────────────────────────────────────────────────
const ALL_AUDIT_ACTIONS = fileService.auditActions();

function AuditTab() {
  const [page, setPage] = useState(0);
  const [action, setAction] = useState<string | undefined>(undefined);
  const [ownerFilter, setOwnerFilter] = useState<string>('');
  const parsedOwner = Number(ownerFilter.trim());
  const ownerId = ownerFilter.trim() && !Number.isNaN(parsedOwner) ? parsedOwner : undefined;

  const { data, isLoading, isError, error, refetch } = useAdminAuditLogs(
    page,
    PAGE_SIZE,
    action,
    ownerId,
  );

  const entries = useMemo(() => data?.content ?? [], [data]);
  const totalPages = Math.max(1, data?.totalPages ?? 1);

  return (
    <div className="space-y-4">
      {/* Filters */}
      <div className="space-y-3">
        <div className="flex flex-wrap items-center gap-2">
          <div className="relative">
            <Search className="pointer-events-none absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <input
              type="number"
              min={1}
              value={ownerFilter}
              onChange={(event) => {
                setOwnerFilter(event.target.value);
                setPage(0);
              }}
              placeholder="Filter by user ID…"
              className="w-40 rounded-lg border border-gray-300 bg-white py-2 pr-3 pl-9 text-sm text-gray-900 shadow-sm transition-colors placeholder:text-gray-400 focus:border-brand-500 focus:ring-2 focus:ring-brand-500/25 focus:outline-none dark:border-gray-700 dark:bg-gray-950 dark:text-white dark:placeholder:text-gray-500"
            />
          </div>
          <Button variant="outline" size="sm" leftIcon={<RefreshCw className="h-4 w-4" />} onClick={() => void refetch()}>
            Refresh
          </Button>
        </div>

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
          {ALL_AUDIT_ACTIONS.map((item) => (
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
      </div>

      <Card className="p-2">
        {isLoading ? (
          <div className="space-y-2 p-3">
            {Array.from({ length: 5 }).map((_, index) => (
              <CardSkeleton key={index} className="h-14" />
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
            <p className="text-sm text-gray-500 dark:text-gray-400">No audit entries match this filter.</p>
          </div>
        ) : (
          <motion.ul layout className="space-y-0.5">
            <AnimatePresence initial={false}>
              {entries.map((entry: AuditLogEntry) => {
                const meta = AUDIT_ACTION_META[entry.action] ?? {
                  label: entry.action,
                  icon: ScrollText,
                  className: 'bg-gray-500/10 text-gray-500',
                };
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
                          <span className="text-gray-400 dark:text-gray-500"> — {entry.resourceName}</span>
                        ) : null}
                      </p>
                      <p className="truncate text-xs text-gray-400 dark:text-gray-500">
                        user #{entry.ownerId ?? '?'}
                        {entry.details ? ` · ${entry.details}` : ''}
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

        {!isLoading && !isError && (
          <Pagination
            page={data?.page ?? 0}
            totalPages={totalPages}
            totalElements={data?.totalElements}
            onPage={setPage}
          />
        )}
      </Card>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Security tab
// ─────────────────────────────────────────────────────────────────────────────
function SecurityTab() {
  const overview = useAdminSecurityOverview();

  return (
    <div className="space-y-6">
      {overview.data && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[
            { label: 'Total accounts', value: overview.data.totalAccounts, icon: Users, className: 'bg-brand-500/10 text-brand-500' },
            { label: 'Locked', value: overview.data.lockedAccounts, icon: Lock, className: 'bg-amber-500/10 text-amber-500' },
            { label: 'Pending verify', value: overview.data.pendingVerification, icon: Mail, className: 'bg-sky-500/10 text-sky-500' },
            { label: 'Total logins', value: overview.data.totalLogins, icon: LogIn, className: 'bg-emerald-500/10 text-emerald-500' },
            { label: 'Failed (7d)', value: overview.data.failedLoginsLast7Days, icon: ShieldAlert, className: 'bg-rose-500/10 text-rose-500' },
            { label: 'Active sessions', value: overview.data.activeSessions, icon: MonitorSmartphone, className: 'bg-violet-500/10 text-violet-500' },
            { label: 'Trusted devices', value: overview.data.trustedDeviceCount, icon: Settings2, className: 'bg-indigo-500/10 text-indigo-500' },
            { label: 'Disabled', value: overview.data.disabledAccounts, icon: UserX, className: 'bg-rose-500/10 text-rose-500' },
            { label: 'Admins', value: overview.data.adminCount, icon: ShieldCheck, className: 'bg-violet-500/10 text-violet-500' },
          ].map((card) => (
            <StatCard key={card.label} {...card} />
          ))}
        </div>
      )}

      <LoginHistorySection />
      <SecurityLogsSection />
    </div>
  );
}

function LoginHistorySection() {
  const [page, setPage] = useState(0);
  const { data, isLoading, isError, error, refetch } = useAdminLoginHistory(page, PAGE_SIZE);
  const entries = useMemo(() => data?.content ?? [], [data]);
  const totalPages = Math.max(1, data?.totalPages ?? 1);

  return (
    <Card>
      <div className="flex items-center justify-between border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <div>
          <h3 className="text-sm font-semibold text-gray-900 dark:text-white">Login history</h3>
          <p className="text-xs text-gray-400 dark:text-gray-500">Successful and failed sign-ins across all users.</p>
        </div>
        <Button variant="outline" size="sm" leftIcon={<RefreshCw className="h-4 w-4" />} onClick={() => void refetch()}>
          Refresh
        </Button>
      </div>

      {isLoading ? (
        <div className="space-y-2 p-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <CardSkeleton key={index} className="h-12" />
          ))}
        </div>
      ) : isError ? (
        <div className="p-4">
          <ErrorState message={getErrorMessage(error, 'Failed to load login history.')} onRetry={() => void refetch()} />
        </div>
      ) : entries.length === 0 ? (
        <div className="flex flex-col items-center gap-2 py-12 text-center">
          <LogIn className="h-8 w-8 text-gray-300 dark:text-gray-600" />
          <p className="text-sm text-gray-500 dark:text-gray-400">No sign-in activity recorded yet.</p>
        </div>
      ) : (
        <div className="divide-y divide-gray-50 dark:divide-gray-800/60">
          {entries.map((entry: LoginHistoryEntry) => (
            <div key={entry.id} className="flex items-center gap-3 px-5 py-3">
              <span
                className={cn(
                  'grid h-9 w-9 shrink-0 place-items-center rounded-lg',
                  entry.success
                    ? 'bg-emerald-500/10 text-emerald-500'
                    : 'bg-rose-500/10 text-rose-500',
                )}
              >
                {entry.success ? <LogIn className="h-4 w-4" /> : <XCircle className="h-4 w-4" />}
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-gray-900 dark:text-white">
                  {entry.success ? 'Sign-in' : 'Failed sign-in'}
                  <span className="text-gray-400 dark:text-gray-500">
                    {' '}· user #{entry.userId ?? '?'}
                  </span>
                </p>
                <p className="truncate text-xs text-gray-400 dark:text-gray-500">
                  {[entry.browser, entry.os, entry.location, entry.ipAddress]
                    .filter(Boolean)
                    .join(' · ')}
                </p>
              </div>
              <span className="shrink-0 text-xs text-gray-400 tabular-nums dark:text-gray-500">
                {formatRelativeTime(entry.loginTime)}
              </span>
            </div>
          ))}
        </div>
      )}

      {!isLoading && !isError && (
        <Pagination page={data?.page ?? 0} totalPages={totalPages} totalElements={data?.totalElements} onPage={setPage} />
      )}
    </Card>
  );
}

function SecurityLogsSection() {
  const [page, setPage] = useState(0);
  const { data, isLoading, isError, error, refetch } = useAdminSecurityLogs(page, PAGE_SIZE);
  const entries = useMemo(() => data?.content ?? [], [data]);
  const totalPages = Math.max(1, data?.totalPages ?? 1);

  return (
    <Card>
      <div className="flex items-center justify-between border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <div>
          <h3 className="text-sm font-semibold text-gray-900 dark:text-white">Security log</h3>
          <p className="text-xs text-gray-400 dark:text-gray-500">Security-relevant actions across all users.</p>
        </div>
        <Button variant="outline" size="sm" leftIcon={<RefreshCw className="h-4 w-4" />} onClick={() => void refetch()}>
          Refresh
        </Button>
      </div>

      {isLoading ? (
        <div className="space-y-2 p-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <CardSkeleton key={index} className="h-12" />
          ))}
        </div>
      ) : isError ? (
        <div className="p-4">
          <ErrorState message={getErrorMessage(error, 'Failed to load security logs.')} onRetry={() => void refetch()} />
        </div>
      ) : entries.length === 0 ? (
        <div className="flex flex-col items-center gap-2 py-12 text-center">
          <ShieldAlert className="h-8 w-8 text-gray-300 dark:text-gray-600" />
          <p className="text-sm text-gray-500 dark:text-gray-400">No security events recorded yet.</p>
        </div>
      ) : (
        <div className="divide-y divide-gray-50 dark:divide-gray-800/60">
          {entries.map((entry: SecurityLogEntry) => (
            <div key={entry.id} className="flex items-center gap-3 px-5 py-3">
              <span className="bg-amber-500/10 text-amber-500 grid h-9 w-9 shrink-0 place-items-center rounded-lg">
                <ShieldAlert className="h-4 w-4" />
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-gray-900 dark:text-white">
                  {entry.action.replaceAll('_', ' ').toLowerCase()}
                  <span className="text-gray-400 dark:text-gray-500">
                    {' '}· user #{entry.userId ?? '?'}
                  </span>
                </p>
                <p className="truncate text-xs text-gray-400 dark:text-gray-500">
                  {[entry.details, entry.location, entry.ipAddress].filter(Boolean).join(' · ')}
                </p>
              </div>
              <span className="shrink-0 text-xs text-gray-400 tabular-nums dark:text-gray-500">
                {formatRelativeTime(entry.createdAt)}
              </span>
            </div>
          ))}
        </div>
      )}

      {!isLoading && !isError && (
        <Pagination page={data?.page ?? 0} totalPages={totalPages} totalElements={data?.totalElements} onPage={setPage} />
      )}
    </Card>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// System tab
// ─────────────────────────────────────────────────────────────────────────────
function SystemTab() {
  const health = useAdminSystemHealth();
  const minio = useAdminMinioStatus();

  return (
    <div className="space-y-6">
      {/* Microservice health */}
      <Card>
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-100 px-5 py-4 dark:border-gray-800">
          <div>
            <h3 className="text-sm font-semibold text-gray-900 dark:text-white">Microservice health</h3>
            <p className="text-xs text-gray-400 dark:text-gray-500">
              {health.data
                ? `${health.data.healthyCount} of ${health.data.totalCount} services healthy`
                : 'Discovering services…'}
            </p>
          </div>
          <Button
            variant="outline"
            size="sm"
            leftIcon={<RefreshCw className="h-4 w-4" />}
            onClick={() => {
              void health.refetch();
              void minio.refetch();
            }}
          >
            Refresh
          </Button>
        </div>

        {health.isLoading ? (
          <div className="space-y-2 p-4">
            {Array.from({ length: 6 }).map((_, index) => (
              <CardSkeleton key={index} className="h-14" />
            ))}
          </div>
        ) : health.isError ? (
          <div className="p-4">
            <ErrorState message={getErrorMessage(health.error, 'Failed to load system health.')} onRetry={() => void health.refetch()} />
          </div>
        ) : (
          <div className="divide-y divide-gray-50 dark:divide-gray-800/60">
            {health.data?.services.map((service) => {
              const up = service.status === 'UP';
              const unknown = service.status === 'UNKNOWN' || service.status === null;
              return (
                <div key={service.name} className="flex items-center gap-3 px-5 py-3">
                  <span
                    className={cn(
                      'grid h-9 w-9 shrink-0 place-items-center rounded-lg',
                      up
                        ? 'bg-emerald-500/10 text-emerald-500'
                        : unknown
                          ? 'bg-gray-500/10 text-gray-500'
                          : 'bg-rose-500/10 text-rose-500',
                    )}
                  >
                    {up ? <CheckCircle2 className="h-4 w-4" /> : <XCircle className="h-4 w-4" />}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-gray-900 dark:text-white">
                      {service.name.replaceAll('-', ' ')}
                    </p>
                    <p className="truncate text-xs text-gray-400 dark:text-gray-500">
                      {service.instanceCount} instance{service.instanceCount === 1 ? '' : 's'}
                      {service.endpoint ? ` · ${service.endpoint}` : ''}
                    </p>
                  </div>
                  <span
                    className={cn(
                      'inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold',
                      up
                        ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400'
                        : unknown
                          ? 'bg-gray-500/10 text-gray-500 dark:text-gray-400'
                          : 'bg-rose-500/10 text-rose-600 dark:text-rose-400',
                    )}
                  >
                    <span className={cn('h-1.5 w-1.5 rounded-full', up ? 'bg-emerald-500' : unknown ? 'bg-gray-400' : 'bg-rose-500')} />
                    {service.status}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </Card>

      {/* MinIO status */}
      <Card className="p-5">
        <h3 className="mb-1 flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white">
          <Database className="h-4 w-4 text-gray-400" /> Object storage (MinIO)
        </h3>
        {minio.isLoading ? (
          <CardSkeleton className="mt-3 h-20" />
        ) : minio.isError ? (
          <ErrorState message={getErrorMessage(minio.error, 'Failed to load MinIO status.')} onRetry={() => void minio.refetch()} />
        ) : minio.data ? (
          <div className="mt-3 flex flex-wrap items-center gap-4">
            <span
              className={cn(
                'inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold',
                minio.data.reachable
                  ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400'
                  : 'bg-rose-500/10 text-rose-600 dark:text-rose-400',
              )}
            >
              <span
                className={cn(
                  'h-2 w-2 rounded-full',
                  minio.data.reachable ? 'bg-emerald-500' : 'bg-rose-500',
                )}
              />
              {minio.data.status}
            </span>
            <span className="text-xs text-gray-500 dark:text-gray-400">
              <span className="font-medium text-gray-700 dark:text-gray-200">Endpoint:</span>{' '}
              {minio.data.endpoint}
            </span>
            <span className="text-xs text-gray-500 dark:text-gray-400">
              <span className="font-medium text-gray-700 dark:text-gray-200">Bucket:</span>{' '}
              {minio.data.bucket}
              {minio.data.reachable && !minio.data.bucketExists && (
                <span className="ml-1 text-amber-500">(missing)</span>
              )}
            </span>
          </div>
        ) : null}
      </Card>
    </div>
  );
}
