import { useState } from 'react';
import {
  BellRing,
  Fingerprint,
  Laptop,
  LogOut,
  Monitor,
  ShieldCheck,
  ShieldX,
  Smartphone,
  Tablet,
  Trash2,
  UserX,
  type LucideIcon,
} from 'lucide-react';

import { EmptyState } from '@/components/common/EmptyState';
import { ErrorState } from '@/components/common/ErrorState';
import { PageHeader } from '@/components/common/PageHeader';
import { Loader } from '@/components/common/Loader';
import { PasskeysPanel, TwoFactorPanel } from '@/components/security/MfaPanels';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import {
  useLoginHistory,
  useSecurityLogs,
  useSecurityMutations,
  useSecurityOverview,
  useSessions,
  useTrustedDevices,
} from '@/hooks/useSecurity';
import type { SessionInfo, TrustedDeviceInfo } from '@/types';
import { cn } from '@/utils/cn';
import { formatRelativeTime } from '@/utils/format';

type Tab = 'overview' | 'sessions' | 'devices' | 'history' | 'logs' | 'mfa';

const TABS: readonly { id: Tab; label: string }[] = [
  { id: 'overview', label: 'Overview' },
  { id: 'mfa', label: '2FA & passkeys' },
  { id: 'sessions', label: 'Active sessions' },
  { id: 'devices', label: 'Trusted devices' },
  { id: 'history', label: 'Login history' },
  { id: 'logs', label: 'Security log' },
];

function DeviceIcon({ type }: { type: SessionInfo['deviceType'] }) {
  const icons: Record<string, LucideIcon> = {
    DESKTOP: Monitor,
    TABLET: Tablet,
    MOBILE: Smartphone,
    OTHER: Laptop,
  };
  const Icon = icons[type] ?? Laptop;
  return (
    <div className="bg-brand-500/10 text-brand-600 dark:bg-brand-400/10 dark:text-brand-300 grid h-11 w-11 shrink-0 place-items-center rounded-xl">
      <Icon className="h-5 w-5" />
    </div>
  );
}

function scoreColor(score: number): string {
  if (score >= 80) return 'text-emerald-500';
  if (score >= 50) return 'text-amber-500';
  return 'text-rose-500';
}

export function SecurityPage() {
  const [tab, setTab] = useState<Tab>('overview');
  const [historyPage, setHistoryPage] = useState(0);
  const [logsPage, setLogsPage] = useState(0);

  const overview = useSecurityOverview();
  const sessions = useSessions();
  const trustedDevices = useTrustedDevices();
  const loginHistory = useLoginHistory(historyPage);
  const securityLogs = useSecurityLogs(logsPage);
  const { endSession, logoutAll, removeTrustedDevice } = useSecurityMutations();

  return (
    <div className="space-y-6">
      <PageHeader
        title="Security"
        description="Protect your account, review sign-ins and manage your devices."
        actions={
          <Button
            variant="outline"
            leftIcon={<LogOut className="h-4 w-4" />}
            isLoading={logoutAll.isPending}
            onClick={() => {
              if (window.confirm('Log out from every device? You will be signed out here too.')) {
                logoutAll.mutate();
              }
            }}
          >
            Log out all devices
          </Button>
        }
      />

      {/* Security score banner */}
      <Card>
        <CardBody className="flex flex-wrap items-center gap-6">
          {overview.isLoading ? (
            <Loader className="py-8" />
          ) : overview.isError ? (
            <ErrorState
              message="Couldn't load your security overview."
              onRetry={() => void overview.refetch()}
            />
          ) : (
            <>
              <div
                className={cn(
                  'grid h-24 w-24 shrink-0 place-items-center rounded-full border-4',
                  overview.data!.securityScore >= 80
                    ? 'border-emerald-500/40 bg-emerald-500/10'
                    : overview.data!.securityScore >= 50
                      ? 'border-amber-500/40 bg-amber-500/10'
                      : 'border-rose-500/40 bg-rose-500/10',
                )}
              >
                <div className="text-center">
                  <p className={cn('text-3xl font-bold', scoreColor(overview.data!.securityScore))}>
                    {overview.data!.securityScore}
                  </p>
                  <p className="text-xs text-gray-400">/ 100</p>
                </div>
              </div>

              <div className="min-w-0 flex-1 space-y-3">
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                    Security score
                  </h3>
                  <p className="text-sm text-gray-500 dark:text-gray-400">
                    {overview.data!.securityScore >= 80
                      ? 'Great shape — keep it up.'
                      : overview.data!.securityScore >= 50
                        ? 'Decent, but a few easy wins remain.'
                        : 'Your account needs attention.'}
                  </p>
                </div>

                <div className="flex flex-wrap gap-2 text-xs">
                  <StatusPill
                    ok={overview.data!.emailVerified}
                    okLabel="Email verified"
                    badLabel="Email not verified"
                  />
                  <StatusPill
                    ok={Boolean(overview.data!.twoFactorEnabled)}
                    okLabel="2FA on"
                    badLabel="2FA off"
                  />
                  <span className="rounded-full bg-gray-100 px-2.5 py-1 font-medium text-gray-600 dark:bg-gray-800 dark:text-gray-300">
                    {overview.data!.activeSessionCount} active session{overview.data!.activeSessionCount === 1 ? '' : 's'}
                  </span>
                  <span className="rounded-full bg-gray-100 px-2.5 py-1 font-medium text-gray-600 dark:bg-gray-800 dark:text-gray-300">
                    {overview.data!.trustedDeviceCount} trusted device{overview.data!.trustedDeviceCount === 1 ? '' : 's'}
                  </span>
                  {overview.data!.failedLoginsLast7Days > 0 && (
                    <span className="rounded-full bg-rose-100 px-2.5 py-1 font-medium text-rose-700 dark:bg-rose-500/15 dark:text-rose-300">
                      {overview.data!.failedLoginsLast7Days} failed attempt{overview.data!.failedLoginsLast7Days === 1 ? '' : 's'} this week
                    </span>
                  )}
                </div>
              </div>

              <div className="hidden text-sm text-gray-500 md:block dark:text-gray-400">
                <p>
                  Last login:{' '}
                  <span className="font-medium text-gray-700 dark:text-gray-200">
                    {overview.data!.lastLoginAt ? formatRelativeTime(overview.data!.lastLoginAt) : '—'}
                  </span>
                </p>
                <p>
                  Password changed:{' '}
                  <span className="font-medium text-gray-700 dark:text-gray-200">
                    {overview.data!.passwordChangedAt ? formatRelativeTime(overview.data!.passwordChangedAt) : 'never'}
                  </span>
                </p>
              </div>
            </>
          )}
        </CardBody>
      </Card>

      {/* Tabs */}
      <div className="flex gap-1 overflow-x-auto rounded-xl border border-gray-200/80 bg-white p-1 dark:border-gray-800 dark:bg-gray-900">
        {TABS.map((item) => (
          <button
            key={item.id}
            type="button"
            onClick={() => setTab(item.id)}
            className={cn(
              'flex-1 whitespace-nowrap rounded-lg px-4 py-2 text-sm font-medium transition-colors',
              tab === item.id
                ? 'bg-brand-600 text-white shadow-sm dark:bg-brand-500'
                : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-white',
            )}
          >
            {item.label}
          </button>
        ))}
      </div>

      {tab === 'overview' && (
        <div className="grid gap-6 md:grid-cols-2">
          <Card>
            <CardHeader title="Account protection" description="Things you can turn on or fix." />
            <CardBody className="space-y-4">
              <ProtectionRow
                icon={ShieldCheck}
                title="Email verification"
                detail={overview.data?.emailVerified ? 'Your email is verified.' : 'Verify your email to unlock everything.'}
                ok={Boolean(overview.data?.emailVerified)}
              />
              <ProtectionRow
                icon={Fingerprint}
                title="Two-factor authentication"
                detail={
                  overview.data?.twoFactorEnabled
                    ? 'A code from your authenticator app is required at sign-in.'
                    : 'Add an authenticator app code for stronger protection.'
                }
                ok={Boolean(overview.data?.twoFactorEnabled)}
              />
              <ProtectionRow
                icon={ShieldX}
                title="Password strength"
                detail={
                  overview.data?.passwordChangedAt
                    ? `Changed ${formatRelativeTime(overview.data.passwordChangedAt)}.`
                    : 'Set a strong password from Profile → Change password.'
                }
                ok={Boolean(overview.data?.passwordChangedAt)}
              />
              <ProtectionRow
                icon={BellRing}
                title="Sign-in alerts"
                detail="We email you about new and unknown-device sign-ins automatically."
                ok
              />
            </CardBody>
          </Card>

          <Card>
            <CardHeader title="Recent activity" description="A quick look at what happened lately." />
            <CardBody className="space-y-3">
              <div className="flex items-center justify-between text-sm">
                <span className="text-gray-500 dark:text-gray-400">Sign-ins recorded</span>
                <span className="font-semibold text-gray-900 dark:text-white">{overview.data?.totalLogins ?? '—'}</span>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-gray-500 dark:text-gray-400">Failed attempts (7 days)</span>
                <span className="font-semibold text-gray-900 dark:text-white">{overview.data?.failedLoginsLast7Days ?? '—'}</span>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-gray-500 dark:text-gray-400">Active sessions</span>
                <span className="font-semibold text-gray-900 dark:text-white">{overview.data?.activeSessionCount ?? '—'}</span>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-gray-500 dark:text-gray-400">Trusted devices</span>
                <span className="font-semibold text-gray-900 dark:text-white">{overview.data?.trustedDeviceCount ?? '—'}</span>
              </div>
            </CardBody>
          </Card>
        </div>
      )}

      {tab === 'mfa' && (
        <div className="space-y-6">
          <TwoFactorPanel />
          <PasskeysPanel />
        </div>
      )}

      {tab === 'sessions' && <SessionsTab sessions={sessions.data} isLoading={sessions.isLoading} onEnd={endSession.mutate} />}

      {tab === 'devices' && <DevicesTab devices={trustedDevices.data} isLoading={trustedDevices.isLoading} onRemove={removeTrustedDevice.mutate} />}

      {tab === 'history' && (
        <HistoryTab
          page={historyPage}
          setPage={setHistoryPage}
          data={loginHistory.data}
          isLoading={loginHistory.isLoading}
        />
      )}

      {tab === 'logs' && (
        <LogsTab
          page={logsPage}
          setPage={setLogsPage}
          data={securityLogs.data}
          isLoading={securityLogs.isLoading}
        />
      )}
    </div>
  );
}

// ── Sub-components ─────────────────────────────────────────────────────────

function StatusPill({ ok, okLabel, badLabel }: { ok: boolean; okLabel: string; badLabel: string }) {
  return (
    <span
      className={cn(
        'rounded-full px-2.5 py-1 font-medium',
        ok
          ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300'
          : 'bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300',
      )}
    >
      {ok ? okLabel : badLabel}
    </span>
  );
}

function ProtectionRow({ icon: Icon, title, detail, ok }: { icon: LucideIcon; title: string; detail: string; ok: boolean }) {
  return (
    <div className="flex items-start gap-3">
      <div
        className={cn(
          'grid h-10 w-10 shrink-0 place-items-center rounded-xl',
          ok ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400' : 'bg-gray-100 text-gray-500 dark:bg-gray-800 dark:text-gray-400',
        )}
      >
        <Icon className="h-5 w-5" />
      </div>
      <div className="min-w-0">
        <p className="text-sm font-semibold text-gray-900 dark:text-white">{title}</p>
        <p className="text-xs text-gray-500 dark:text-gray-400">{detail}</p>
      </div>
      <span className={cn('ml-auto mt-1 text-xs font-bold', ok ? 'text-emerald-500' : 'text-gray-400')}>
        {ok ? 'ON' : '—'}
      </span>
    </div>
  );
}

function SessionsTab({
  sessions,
  isLoading,
  onEnd,
}: {
  sessions: SessionInfo[] | undefined;
  isLoading: boolean;
  onEnd: (sessionId: string) => void;
}) {
  if (isLoading) {
    return <Loader className="py-16" />;
  }
  if (!sessions || sessions.length === 0) {
    return <EmptyState icon={<Monitor className="h-7 w-7" />} title="No active sessions" description="Sessions appear here when you sign in from a device." />;
  }
  return (
    <div className="space-y-3">
      {sessions.map((session) => (
        <Card key={session.sessionId}>
          <CardBody className="flex items-center gap-4">
            <DeviceIcon type={session.deviceType} />
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <p className="font-semibold text-gray-900 dark:text-white">{session.deviceName}</p>
                {session.current && (
                  <span className="bg-brand-600/10 text-brand-700 dark:bg-brand-500/15 dark:text-brand-300 rounded-full px-2 py-0.5 text-[11px] font-semibold">
                    This device
                  </span>
                )}
                {session.trusted && (
                  <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-[11px] font-semibold text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300">
                    Trusted
                  </span>
                )}
              </div>
              <p className="mt-0.5 truncate text-xs text-gray-500 dark:text-gray-400">
                {[session.ipAddress, session.location].filter(Boolean).join(' · ')}
              </p>
              <p className="text-xs text-gray-400 dark:text-gray-500">
                Signed in {formatRelativeTime(session.loginTime)} · Active {formatRelativeTime(session.lastActive)}
              </p>
            </div>
            {!session.current && (
              <Button
                variant="ghost"
                size="sm"
                leftIcon={<UserX className="h-4 w-4" />}
                onClick={() => onEnd(session.sessionId)}
              >
                End session
              </Button>
            )}
          </CardBody>
        </Card>
      ))}
    </div>
  );
}

function DevicesTab({
  devices,
  isLoading,
  onRemove,
}: {
  devices: TrustedDeviceInfo[] | undefined;
  isLoading: boolean;
  onRemove: (id: number) => void;
}) {
  if (isLoading) {
    return <Loader className="py-16" />;
  }
  if (!devices || devices.length === 0) {
    return (
      <EmptyState
        icon={<ShieldCheck className="h-7 w-7" />}
        title="No trusted devices"
        description='Tick "Remember this device" at sign-in to skip the email code next time.'
      />
    );
  }
  return (
    <div className="space-y-3">
      {devices.map((device) => (
        <Card key={device.id}>
          <CardBody className="flex items-center gap-4">
            <DeviceIcon type="OTHER" />
            <div className="min-w-0 flex-1">
              <p className="font-semibold text-gray-900 dark:text-white">{device.deviceName}</p>
              <p className="mt-0.5 truncate text-xs text-gray-500 dark:text-gray-400">
                {device.browser} · {device.os} · {device.ipAddress ?? 'Unknown IP'}
              </p>
              <p className="text-xs text-gray-400 dark:text-gray-500">
                Last used {formatRelativeTime(device.lastUsedAt)}
              </p>
            </div>
            <Button
              variant="ghost"
              size="sm"
              leftIcon={<Trash2 className="h-4 w-4" />}
              onClick={() => {
                if (window.confirm(`Stop trusting "${device.deviceName}"? It will need an email code again.`)) {
                  onRemove(device.id);
                }
              }}
            >
              Remove
            </Button>
          </CardBody>
        </Card>
      ))}
    </div>
  );
}

function HistoryTab({
  page,
  setPage,
  data,
  isLoading,
}: {
  page: number;
  setPage: (page: number) => void;
  data: Awaited<ReturnType<typeof useLoginHistory>>['data'];
  isLoading: boolean;
}) {
  if (isLoading) {
    return <Loader className="py-16" />;
  }
  if (!data || data.content.length === 0) {
    return <EmptyState icon={<BellRing className="h-7 w-7" />} title="No sign-ins recorded yet" description="Every successful and failed sign-in will show up here." />;
  }
  return (
    <Card>
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-gray-100 text-xs text-gray-400 uppercase dark:border-gray-800">
              <th className="px-6 py-3 font-semibold">Result</th>
              <th className="px-6 py-3 font-semibold">Device</th>
              <th className="px-6 py-3 font-semibold">Location</th>
              <th className="px-6 py-3 font-semibold">IP address</th>
              <th className="px-6 py-3 font-semibold">Time</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
            {data.content.map((entry) => (
              <tr key={entry.id} className="hover:bg-gray-50 dark:hover:bg-gray-800/40">
                <td className="px-6 py-3">
                  {entry.success ? (
                    <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-semibold text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300">
                      Success
                    </span>
                  ) : (
                    <span className="rounded-full bg-rose-100 px-2 py-0.5 text-xs font-semibold text-rose-700 dark:bg-rose-500/15 dark:text-rose-300">
                      Failed{entry.failureReason ? ` · ${entry.failureReason}` : ''}
                    </span>
                  )}
                </td>
                <td className="px-6 py-3 text-gray-700 dark:text-gray-200">{entry.deviceName}</td>
                <td className="px-6 py-3 text-gray-500 dark:text-gray-400">{entry.location}</td>
                <td className="px-6 py-3 font-mono text-xs text-gray-500 dark:text-gray-400">{entry.ipAddress ?? '—'}</td>
                <td className="px-6 py-3 text-gray-500 dark:text-gray-400">{formatRelativeTime(entry.loginTime)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination page={page} totalPages={data.totalPages} setPage={setPage} />
    </Card>
  );
}

function LogsTab({
  page,
  setPage,
  data,
  isLoading,
}: {
  page: number;
  setPage: (page: number) => void;
  data: Awaited<ReturnType<typeof useSecurityLogs>>['data'];
  isLoading: boolean;
}) {
  if (isLoading) {
    return <Loader className="py-16" />;
  }
  if (!data || data.content.length === 0) {
    return <EmptyState icon={<ShieldCheck className="h-7 w-7" />} title="No security events yet" description="Security-relevant actions will appear here." />;
  }
  return (
    <Card>
      <div className="divide-y divide-gray-100 dark:divide-gray-800">
        {data.content.map((entry) => (
          <div key={entry.id} className="flex items-start gap-3 px-6 py-4">
            <div className="bg-gray-100 text-gray-500 dark:bg-gray-800 dark:text-gray-400 mt-0.5 grid h-9 w-9 shrink-0 place-items-center rounded-lg">
              <ShieldCheck className="h-4.5 w-4.5" />
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold text-gray-900 dark:text-white">{formatAction(entry.action)}</p>
              {entry.details && <p className="text-xs text-gray-500 dark:text-gray-400">{entry.details}</p>}
            </div>
            <div className="text-right text-xs text-gray-400 dark:text-gray-500">
              <p>{formatRelativeTime(entry.createdAt)}</p>
              <p className="font-mono">{entry.ipAddress ?? ''}</p>
            </div>
          </div>
        ))}
      </div>
      <Pagination page={page} totalPages={data.totalPages} setPage={setPage} />
    </Card>
  );
}

function Pagination({ page, totalPages, setPage }: { page: number; totalPages: number; setPage: (page: number) => void }) {
  return (
    <div className="flex items-center justify-between border-t border-gray-100 px-6 py-3 dark:border-gray-800">
      <p className="text-xs text-gray-400">
        Page {page + 1} of {Math.max(totalPages, 1)}
      </p>
      <div className="flex gap-2">
        <Button variant="outline" size="sm" disabled={page <= 0} onClick={() => setPage(page - 1)}>
          Previous
        </Button>
        <Button variant="outline" size="sm" disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>
          Next
        </Button>
      </div>
    </div>
  );
}

function formatAction(action: string): string {
  const labels: Record<string, string> = {
    LOGIN_SUCCESS: 'Signed in',
    LOGIN_FAILED: 'Failed sign-in',
    LOGIN_LOCKED: 'Sign-in blocked (locked)',
    OTP_VERIFIED: 'Email code verified',
    PASSWORD_CHANGED: 'Password changed',
    PASSWORD_RESET: 'Password reset',
    LOGOUT: 'Signed out',
    LOGOUT_ALL: 'Signed out everywhere',
    SESSION_ENDED: 'Session ended remotely',
    DEVICE_TRUSTED: 'Device marked trusted',
    DEVICE_UNTRUSTED: 'Trusted device removed',
    ACCOUNT_ACTIVATED: 'Account activated',
    ACCOUNT_LOCKED: 'Account locked after failed attempts',
    // ── Phase 6: 2FA & passkeys ───────────────────────────────────────────
    '2FA_ENABLED': 'Two-factor authentication enabled',
    '2FA_DISABLED': 'Two-factor authentication disabled',
    '2FA_VERIFIED': 'Two-factor code verified',
    BACKUP_CODES_REGENERATED: 'Backup codes regenerated',
    PASSKEY_REGISTERED: 'Passkey registered',
    PASSKEY_REMOVED: 'Passkey removed',
  };
  return labels[action] ?? action.replaceAll('_', ' ');
}
