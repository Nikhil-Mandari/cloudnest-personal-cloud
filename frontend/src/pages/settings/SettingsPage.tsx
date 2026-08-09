import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import {
  Bell,
  Database,
  ExternalLink,
  FolderOpen,
  GitBranch,
  HardDrive,
  Info,
  Languages,
  Layers,
  LogOut,
  Monitor,
  Moon,
  Palette,
  RefreshCw,
  Server,
  ShieldAlert,
  Sun,
  Trash2,
  UserX,
} from 'lucide-react';
import { useNavigate, Link } from 'react-router-dom';
import { toast } from 'react-toastify';

import { PageHeader } from '@/components/common/PageHeader';
import { ConfirmationDialog } from '@/components/ui/ConfirmationDialog';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import {
  APP_DEVELOPER,
  APP_GITHUB_URL,
  APP_NAME,
  APP_TAGLINE,
  APP_TECH_STACK,
  APP_VERSION,
  BACKEND_VERSION,
} from '@/constants/app';
import { APP_ROUTES } from '@/constants/routes';
import { useAuth } from '@/hooks/useAuth';
import { useProfileStats } from '@/hooks/useProfileStats';
import { useSystemStatus } from '@/hooks/useSystemStatus';
import { userService } from '@/services/user.service';
import { useThemeStore, type Theme } from '@/store/themeStore';
import { cn } from '@/utils/cn';
import { formatBytes } from '@/utils/format';
import { getErrorMessage } from '@/utils/error';

const THEMES: ReadonlyArray<{ value: Theme; label: string; icon: typeof Sun }> = [
  { value: 'light', label: 'Light', icon: Sun },
  { value: 'dark', label: 'Dark', icon: Moon },
  { value: 'system', label: 'System', icon: Monitor },
];

/** Microservices known to CloudNest — used to label the registry list. */
const SERVICE_LABELS: Record<string, string> = {
  'auth-service': 'Auth service',
  'user-service': 'User service',
  'file-service': 'File service',
  'folder-service': 'Folder service',
  'share-service': 'Share service',
  'notification-service': 'Notification service',
  'config-server': 'Config server',
  'eureka-server': 'Eureka discovery',
  'api-gateway': 'API gateway',
};

function StatusDot({ online }: { online: boolean }) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        'relative inline-flex h-2.5 w-2.5 shrink-0 rounded-full',
        online ? 'bg-emerald-500' : 'bg-rose-500',
      )}
    >
      {online && (
        <span className="absolute inset-0 animate-ping rounded-full bg-emerald-500/60" />
      )}
    </span>
  );
}

export function SettingsPage() {
  const theme = useThemeStore((state) => state.theme);
  const setTheme = useThemeStore((state) => state.setTheme);
  const { logout } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const stats = useProfileStats();
  const { data: health, refetch, isFetching } = useSystemStatus();

  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);
  const [confirmLogoutAllOpen, setConfirmLogoutAllOpen] = useState(false);

  const gatewayUp = health?.status === 'UP';
  const services = health?.services ?? [];
  const minioUp = services.includes('file-service');

  const deleteAccount = useMutation({
    mutationFn: () => userService.deleteAccount(),
    onSuccess: () => {
      toast.success('Your account has been deleted. Thank you for using CloudNest!');
      queryClient.clear();
      logout();
      navigate(APP_ROUTES.login, { replace: true });
    },
    onError: (error) =>
      toast.error(getErrorMessage(error, 'Failed to delete your account. Please try again.')),
  });

  const handleLogoutAllDevices = () => {
    setConfirmLogoutAllOpen(false);
    queryClient.clear();
    logout();
    navigate(APP_ROUTES.login, { replace: true });
    toast.info('Signed out on this device. CloudNest uses stateless JWTs, so remote sessions expire when their tokens do.');
  };

  const selectTheme = (next: Theme) => setTheme(next);

  return (
    <div className="space-y-6">
      <PageHeader title="Settings" description="Customise CloudNest to suit the way you work." />

      {/* ── Appearance ────────────────────────────────────────────────────── */}
      <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}>
        <Card>
          <CardHeader
            title="Appearance"
            description="Choose how CloudNest looks on this device."
            action={<Palette className="h-5 w-5 text-gray-400" />}
          />
          <CardBody>
            <div role="radiogroup" aria-label="Theme" className="flex flex-wrap gap-3">
              {THEMES.map(({ value, label, icon: Icon }) => (
                <button
                  key={value}
                  type="button"
                  role="radio"
                  aria-checked={theme === value}
                  onClick={() => selectTheme(value)}
                  className={cn(
                    'flex w-36 items-center gap-2.5 rounded-xl border px-4 py-3 text-sm font-medium transition-all',
                    theme === value
                      ? 'border-brand-500 ring-brand-500/30 bg-brand-500/5 text-gray-900 ring-2 dark:text-white'
                      : 'border-gray-200 text-gray-600 hover:border-gray-300 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-800/60',
                  )}
                >
                  <Icon
                    className={cn(
                      'h-4 w-4',
                      theme === value ? 'text-brand-500 dark:text-brand-400' : 'text-gray-400',
                    )}
                  />
                  {label}
                </button>
              ))}
            </div>
          </CardBody>
        </Card>
      </motion.div>

      {/* ── Language ──────────────────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.05 }}
      >
        <Card>
          <CardHeader
            title="Language"
            description="Localisation is planned for a future release."
            action={
              <span className="rounded-full bg-gray-100 px-2.5 py-1 text-[11px] font-semibold tracking-wide text-gray-500 uppercase dark:bg-gray-800 dark:text-gray-400">
                Soon
              </span>
            }
          />
          <CardBody className="flex items-center justify-between gap-4">
            <div className="flex items-start gap-3">
              <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-gray-500/10 text-gray-500 dark:text-gray-400">
                <Languages className="h-5 w-5" />
              </span>
              <div>
                <p className="text-sm font-medium text-gray-900 dark:text-white">
                  English (United States)
                </p>
                <p className="mt-0.5 text-sm text-gray-500 dark:text-gray-400">
                  Additional languages will be added soon.
                </p>
              </div>
            </div>
          </CardBody>
        </Card>
      </motion.div>

      {/* ── Notification preferences ─────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
      >
        <Card>
          <CardHeader
            title="Notifications"
            description="Review and manage your in-app notifications."
            action={<Bell className="h-5 w-5 text-gray-400" />}
          />
          <CardBody className="flex items-center justify-between gap-4">
            <div className="flex items-start gap-3">
              <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-gray-500/10 text-gray-500 dark:text-gray-400">
                <Bell className="h-5 w-5" />
              </span>
              <div>
                <p className="text-sm font-medium text-gray-900 dark:text-white">
                  Notification centre
                </p>
                <p className="mt-0.5 text-sm text-gray-500 dark:text-gray-400">
                  Shares, alerts and account activity live in the notification centre.
                </p>
              </div>
            </div>
            <Link
              to={APP_ROUTES.notifications}
              className="text-brand-600 hover:text-brand-700 dark:text-brand-400 text-sm font-medium transition-colors hover:underline"
            >
              Open notifications
            </Link>
          </CardBody>
        </Card>
      </motion.div>

      {/* ── Storage usage ─────────────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.15 }}
      >
        <Card>
          <CardHeader
            title="Storage usage"
            description="How much of your cloud quota is in use."
            action={<HardDrive className="h-5 w-5 text-gray-400" />}
          />
          <CardBody>
            {stats.isLoading ? (
              <div className="space-y-3 animate-pulse">
                <div className="h-4 w-24 rounded-full bg-gray-100 dark:bg-gray-800" />
                <div className="h-2.5 w-full rounded-full bg-gray-100 dark:bg-gray-800" />
              </div>
            ) : (
              <>
                <div className="flex items-baseline justify-between">
                  <p className="text-lg font-bold tracking-tight text-gray-900 dark:text-white">
                    {formatBytes(stats.storageUsed)}
                  </p>
                  <p className="text-xs text-gray-400 dark:text-gray-500">
                    of {formatBytes(stats.storageQuota)} ({stats.storagePercent}%)
                  </p>
                </div>
                <div className="mt-2 h-2.5 w-full overflow-hidden rounded-full bg-gray-100 dark:bg-gray-800">
                  <motion.div
                    className={cn(
                      'h-full rounded-full bg-linear-to-r',
                      stats.storagePercent > 85
                        ? 'from-amber-500 to-rose-500'
                        : 'from-brand-500 to-accent-600',
                    )}
                    initial={{ width: 0 }}
                    animate={{ width: `${stats.storagePercent}%` }}
                    transition={{ duration: 0.6, ease: 'easeOut' }}
                  />
                </div>
                <p className="mt-3 flex items-center gap-1.5 text-sm text-gray-500 dark:text-gray-400">
                  <FolderOpen className="h-4 w-4 text-gray-400" />
                  {stats.filesCount.toLocaleString()} files · {stats.foldersCount.toLocaleString()}{' '}
                  folders
                </p>
              </>
            )}
          </CardBody>
        </Card>
      </motion.div>

      {/* ── System status ─────────────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
      >
        <Card>
          <CardHeader
            title="System status"
            description="Live health of the CloudNest backend."
            action={
              <button
                type="button"
                onClick={() => void refetch()}
                disabled={isFetching}
                aria-label="Refresh status"
                className="grid h-9 w-9 place-items-center rounded-lg text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-700 disabled:opacity-50 dark:hover:bg-gray-800 dark:hover:text-gray-200"
              >
                <RefreshCw className={cn('h-4 w-4', isFetching && 'animate-spin')} />
              </button>
            }
          />
          <CardBody className="space-y-3">
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div className="flex items-center gap-3 rounded-xl border border-gray-200 bg-gray-50/60 px-4 py-3 dark:border-gray-800 dark:bg-gray-950/40">
                <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-brand-500/10 text-brand-500">
                  <Server className="h-5 w-5" />
                </span>
                <div className="min-w-0">
                  <p className="flex items-center gap-2 text-sm font-medium text-gray-900 dark:text-white">
                    <StatusDot online={gatewayUp} />
                    API gateway
                  </p>
                  <p className="truncate text-xs text-gray-400 dark:text-gray-500">
                    {health?.status === 'unknown' ? 'Offline / unreachable' : gatewayUp ? 'Online' : 'Offline'}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-3 rounded-xl border border-gray-200 bg-gray-50/60 px-4 py-3 dark:border-gray-800 dark:bg-gray-950/40">
                <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-emerald-500/10 text-emerald-500">
                  <Database className="h-5 w-5" />
                </span>
                <div className="min-w-0">
                  <p className="flex items-center gap-2 text-sm font-medium text-gray-900 dark:text-white">
                    <StatusDot online={gatewayUp} />
                    Database (MySQL)
                  </p>
                  <p className="truncate text-xs text-gray-400 dark:text-gray-500">
                    {gatewayUp ? 'Online — services connected' : 'Unknown'}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-3 rounded-xl border border-gray-200 bg-gray-50/60 px-4 py-3 dark:border-gray-800 dark:bg-gray-950/40">
                <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-sky-500/10 text-sky-500">
                  <HardDrive className="h-5 w-5" />
                </span>
                <div className="min-w-0">
                  <p className="flex items-center gap-2 text-sm font-medium text-gray-900 dark:text-white">
                    <StatusDot online={minioUp} />
                    MinIO storage
                  </p>
                  <p className="truncate text-xs text-gray-400 dark:text-gray-500">
                    {minioUp ? 'Online — via file service' : 'Unknown'}
                  </p>
                </div>
              </div>
            </div>

            <div className="rounded-xl border border-gray-200 px-4 py-3 dark:border-gray-800">
              <p className="mb-2 flex items-center gap-1.5 text-xs font-semibold tracking-wide text-gray-400 uppercase dark:text-gray-500">
                <Layers className="h-3.5 w-3.5" /> Registered microservices
              </p>
              {services.length > 0 ? (
                <div className="flex flex-wrap gap-2">
                  {services.map((service) => (
                    <span
                      key={service}
                      className="flex items-center gap-1.5 rounded-lg border border-emerald-500/30 bg-emerald-500/5 px-2.5 py-1 text-xs font-medium text-emerald-700 dark:text-emerald-300"
                    >
                      <StatusDot online />
                      {SERVICE_LABELS[service] ?? service}
                    </span>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-gray-400 dark:text-gray-500">
                  {health?.status === 'unknown'
                    ? 'Status unavailable — the gateway health endpoint could not be reached.'
                    : 'No services registered with discovery right now.'}
                </p>
              )}
            </div>
          </CardBody>
        </Card>
      </motion.div>

      {/* ── About ─────────────────────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.25 }}
      >
        <Card>
          <CardHeader
            title="About"
            description={`About ${APP_NAME}.`}
            action={<Info className="h-5 w-5 text-gray-400" />}
          />
          <CardBody className="space-y-5">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <p className="text-sm font-semibold text-gray-900 dark:text-white">{APP_NAME}</p>
                <p className="mt-0.5 text-sm text-gray-500 dark:text-gray-400">{APP_TAGLINE}</p>
                <p className="mt-2 text-xs text-gray-400 dark:text-gray-500">
                  Built and maintained by {APP_DEVELOPER}.
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <span className="rounded-lg border border-gray-200 px-2.5 py-1 text-xs font-medium text-gray-500 dark:border-gray-700 dark:text-gray-400">
                  Frontend v{APP_VERSION}
                </span>
                <span className="rounded-lg border border-gray-200 px-2.5 py-1 text-xs font-medium text-gray-500 dark:border-gray-700 dark:text-gray-400">
                  Backend v{BACKEND_VERSION}
                </span>
                <a
                  href={APP_GITHUB_URL}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-1.5 rounded-lg border border-gray-200 px-2.5 py-1 text-xs font-medium text-gray-600 transition-colors hover:border-brand-400 hover:text-brand-600 dark:border-gray-700 dark:text-gray-300 dark:hover:text-brand-400"
                >
                  <GitBranch className="h-3.5 w-3.5" />
                  GitHub
                  <ExternalLink className="h-3 w-3" />
                </a>
              </div>
            </div>

            <div>
              <p className="mb-2 flex items-center gap-1.5 text-xs font-semibold tracking-wide text-gray-400 uppercase dark:text-gray-500">
                <Layers className="h-3.5 w-3.5" /> Technology stack
              </p>
              <div className="flex flex-wrap gap-2">
                {APP_TECH_STACK.map((tech) => (
                  <span
                    key={tech}
                    className="rounded-lg bg-gray-100 px-2.5 py-1 text-xs font-medium text-gray-600 dark:bg-gray-800 dark:text-gray-300"
                  >
                    {tech}
                  </span>
                ))}
              </div>
            </div>
          </CardBody>
        </Card>
      </motion.div>

      {/* ── Danger zone ───────────────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3 }}
      >
        <Card className="border-rose-200/70 dark:border-rose-500/25">
          <CardHeader
            title="Danger zone"
            description="Irreversible account actions. Please read carefully."
            action={<ShieldAlert className="h-5 w-5 text-rose-400" />}
          />
          <CardBody className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-gray-200 px-4 py-3 dark:border-gray-800">
              <div className="flex items-start gap-3">
                <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-gray-500/10 text-gray-500 dark:text-gray-400">
                  <LogOut className="h-5 w-5" />
                </span>
                <div>
                  <p className="text-sm font-medium text-gray-900 dark:text-white">
                    Log out of all devices
                  </p>
                  <p className="mt-0.5 text-sm text-gray-500 dark:text-gray-400">
                    Ends your session everywhere. CloudNest tokens are stateless, so remote
                    devices are signed out when their tokens expire.
                  </p>
                </div>
              </div>
              <Button variant="outline" size="sm" onClick={() => setConfirmLogoutAllOpen(true)}>
                Log out all devices
              </Button>
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-rose-200/70 px-4 py-3 dark:border-rose-500/25">
              <div className="flex items-start gap-3">
                <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-rose-500/10 text-rose-500">
                  <UserX className="h-5 w-5" />
                </span>
                <div>
                  <p className="text-sm font-medium text-gray-900 dark:text-white">
                    Delete account
                  </p>
                  <p className="mt-0.5 text-sm text-gray-500 dark:text-gray-400">
                    Permanently deletes your CloudNest profile and signs you out. This cannot be
                    undone.
                  </p>
                </div>
              </div>
              <Button
                variant="danger"
                size="sm"
                leftIcon={<Trash2 className="h-3.5 w-3.5" />}
                onClick={() => setConfirmDeleteOpen(true)}
              >
                Delete account
              </Button>
            </div>
          </CardBody>
        </Card>
      </motion.div>

      {/* ── Confirmation dialogs ──────────────────────────────────────────── */}
      <ConfirmationDialog
        open={confirmLogoutAllOpen}
        onClose={() => setConfirmLogoutAllOpen(false)}
        onConfirm={handleLogoutAllDevices}
        title="Log out of all devices?"
        description="You'll be signed out everywhere. You can sign back in at any time with your password."
        confirmLabel="Log out everywhere"
        variant="primary"
      />
      <ConfirmationDialog
        open={confirmDeleteOpen}
        onClose={() => setConfirmDeleteOpen(false)}
        onConfirm={() => deleteAccount.mutate()}
        title="Delete your account?"
        description="This permanently deletes your CloudNest profile and signs you out everywhere. It cannot be undone."
        confirmLabel="Delete my account"
        isLoading={deleteAccount.isPending}
      />
    </div>
  );
}
