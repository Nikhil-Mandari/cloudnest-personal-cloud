import { useState } from 'react';
import {
  BellRing,
  Laptop,
  Loader2,
  LocateFixed,
  LogOut,
  MapPin,
  Monitor,
  RefreshCw,
  ShieldCheck,
  Smartphone,
  Tablet,
  UserX,
  type LucideIcon,
} from 'lucide-react';

import { EmptyState } from '@/components/common/EmptyState';
import { ErrorState } from '@/components/common/ErrorState';
import { PageHeader } from '@/components/common/PageHeader';
import { Loader } from '@/components/common/Loader';
import { Button } from '@/components/ui/Button';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import {
  useLoginHistory,
  useSecurityLogs,
  useSecurityMutations,
  useSecurityOverview,
  useSessions,
} from '@/hooks/useSecurity';
import { useLocationStore } from '@/store/locationStore';
import type { SessionInfo } from '@/types';
import { cn } from '@/utils/cn';
import { formatRelativeTime } from '@/utils/format';

type Tab = 'sessions' | 'history' | 'logs';

const TABS: readonly { id: Tab; label: string }[] = [
  { id: 'sessions', label: 'Active sessions' },
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

const formatCoordinate = (value: number, positive: string, negative: string): string =>
  `${Math.abs(value).toFixed(6)}° ${value >= 0 ? positive : negative}`;

export function SecurityPage() {
  const [tab, setTab] = useState<Tab>('sessions');
  const [historyPage, setHistoryPage] = useState(0);
  const [logsPage, setLogsPage] = useState(0);

  const sessions = useSessions();
  const loginHistory = useLoginHistory(historyPage);
  const securityLogs = useSecurityLogs(logsPage);
  const { endSession, logoutAll } = useSecurityMutations();

  return (
    <div className="space-y-6">
      <PageHeader
        title="Security"
        description="Review your sign-ins, current location and security activity."
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

      {/* Current sign-in + location */}
      <CurrentSignInCard />

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

      {tab === 'sessions' && <SessionsTab sessions={sessions.data} isLoading={sessions.isLoading} onEnd={endSession.mutate} />}

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

// ── Current sign-in ─────────────────────────────────────────────────────────

function CurrentSignInCard() {
  const overview = useSecurityOverview();
  const sessions = useSessions();
  const current = sessions.data?.find((session) => session.current);

  return (
    <Card>
      <CardHeader
        title="Current sign-in"
        description="Where you're signed in right now, plus your current location."
        action={<ShieldCheck className="h-5 w-5 text-gray-400" />}
      />
      <CardBody>
        <div className="grid gap-8 lg:grid-cols-2">
          <div className="space-y-4">
            {overview.isLoading ? (
              <Loader className="py-6" />
            ) : overview.isError ? (
              <ErrorState
                message="Couldn't load your security overview."
                onRetry={() => void overview.refetch()}
              />
            ) : (
              <>
                <div className="flex flex-wrap items-center gap-2">
                  <StatusPill
                    ok={Boolean(overview.data!.emailVerified)}
                    okLabel="Email verified"
                    badLabel="Email not verified"
                  />
                  {overview.data!.failedLoginsLast7Days > 0 && (
                    <span className="rounded-full bg-rose-100 px-2.5 py-1 font-medium text-rose-700 dark:bg-rose-500/15 dark:text-rose-300">
                      {overview.data!.failedLoginsLast7Days} failed attempt
                      {overview.data!.failedLoginsLast7Days === 1 ? '' : 's'} this week
                    </span>
                  )}
                </div>

                {current ? (
                  <div className="flex items-center gap-4 rounded-xl border border-gray-100 bg-gray-50/60 p-4 dark:border-gray-800 dark:bg-gray-900/60">
                    <DeviceIcon type={current.deviceType} />
                    <div className="min-w-0 flex-1">
                      <p className="font-semibold text-gray-900 dark:text-white">
                        {current.deviceName}
                      </p>
                      <p className="mt-0.5 truncate text-xs text-gray-500 dark:text-gray-400">
                        {[current.ipAddress, current.location].filter(Boolean).join(' · ')}
                      </p>
                      <p className="mt-0.5 text-xs text-gray-400 dark:text-gray-500">
                        Signed in {formatRelativeTime(current.loginTime)} · Active{' '}
                        {formatRelativeTime(current.lastActive)}
                      </p>
                    </div>
                  </div>
                ) : (
                  <p className="text-sm text-gray-500 dark:text-gray-400">
                    No active session details available.
                  </p>
                )}

                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div className="rounded-lg bg-gray-50 px-3 py-2.5 dark:bg-gray-900">
                    <p className="text-[10px] tracking-wide text-gray-400 uppercase">Last login</p>
                    <p className="mt-0.5 font-medium text-gray-900 dark:text-white">
                      {overview.data!.lastLoginAt
                        ? formatRelativeTime(overview.data!.lastLoginAt)
                        : '—'}
                    </p>
                  </div>
                  <div className="rounded-lg bg-gray-50 px-3 py-2.5 dark:bg-gray-900">
                    <p className="text-[10px] tracking-wide text-gray-400 uppercase">
                      Active sessions
                    </p>
                    <p className="mt-0.5 font-medium text-gray-900 dark:text-white">
                      {overview.data!.activeSessionCount}
                    </p>
                  </div>
                </div>
              </>
            )}
          </div>

          <LocationPanel />
        </div>
      </CardBody>
    </Card>
  );
}

function LocationPanel() {
  const capture = useLocationStore((state) => state.capture);
  const { status, latitude, longitude, accuracy, areaName, error } = useLocationStore();

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between gap-2">
        <p className="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white">
          <MapPin className="text-brand-500 h-4 w-4" />
          Current sign-in location
        </p>
        <Button
          variant="ghost"
          size="sm"
          leftIcon={<RefreshCw className="h-3.5 w-3.5" />}
          onClick={capture}
          disabled={status === 'requesting'}
        >
          Refresh
        </Button>
      </div>

      {status === 'requesting' && (
        <div className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
          <Loader2 className="h-4 w-4 animate-spin" />
          Detecting your location…
        </div>
      )}

      {status === 'ready' && latitude !== null && longitude !== null && (
        <div className="space-y-3">
          {areaName && (
            <p className="text-lg font-semibold text-gray-900 dark:text-white">{areaName}</p>
          )}
          <div className="grid grid-cols-2 gap-2">
            <div className="rounded-lg bg-gray-50 px-3 py-2.5 dark:bg-gray-900">
              <p className="text-[10px] tracking-wide text-gray-400 uppercase">Latitude</p>
              <p className="mt-0.5 font-mono text-sm font-semibold text-gray-900 tabular-nums dark:text-white">
                {formatCoordinate(latitude, 'N', 'S')}
              </p>
            </div>
            <div className="rounded-lg bg-gray-50 px-3 py-2.5 dark:bg-gray-900">
              <p className="text-[10px] tracking-wide text-gray-400 uppercase">Longitude</p>
              <p className="mt-0.5 font-mono text-sm font-semibold text-gray-900 tabular-nums dark:text-white">
                {formatCoordinate(longitude, 'E', 'W')}
              </p>
            </div>
          </div>
          {typeof accuracy === 'number' && (
            <p className="text-xs text-gray-400 dark:text-gray-500">
              Accurate to about {Math.round(accuracy)} m
            </p>
          )}
        </div>
      )}

      {status === 'denied' && (
        <p className="text-sm text-amber-600 dark:text-amber-400">{error}</p>
      )}

      {(status === 'unavailable' || status === 'timeout') && (
        <div className="space-y-3">
          <p className="text-sm text-rose-600 dark:text-rose-400">{error}</p>
          <Button variant="outline" size="sm" onClick={capture} leftIcon={<LocateFixed className="h-4 w-4" />}>
            Try again
          </Button>
        </div>
      )}

      {status === 'unsupported' && (
        <p className="text-sm text-amber-600 dark:text-amber-400">{error}</p>
      )}

      {status === 'idle' && (
        <div className="space-y-3">
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Location is captured automatically after sign-in.
          </p>
          <Button variant="outline" size="sm" onClick={capture} leftIcon={<LocateFixed className="h-4 w-4" />}>
            Detect location
          </Button>
        </div>
      )}
    </div>
  );
}

// ── Shared bits ─────────────────────────────────────────────────────────────

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
    return (
      <EmptyState
        icon={<Monitor className="h-7 w-7" />}
        title="No active sessions"
        description="Sessions appear here when you sign in from a device."
      />
    );
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
                Signed in {formatRelativeTime(session.loginTime)} · Active{' '}
                {formatRelativeTime(session.lastActive)}
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
    return (
      <EmptyState
        icon={<BellRing className="h-7 w-7" />}
        title="No sign-ins recorded yet"
        description="Every successful and failed sign-in will show up here."
      />
    );
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
                <td className="px-6 py-3 font-mono text-xs text-gray-500 dark:text-gray-400">
                  {entry.ipAddress ?? '—'}
                </td>
                <td className="px-6 py-3 text-gray-500 dark:text-gray-400">
                  {formatRelativeTime(entry.loginTime)}
                </td>
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
    return (
      <EmptyState
        icon={<ShieldCheck className="h-7 w-7" />}
        title="No security events yet"
        description="Security-relevant actions will appear here."
      />
    );
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
              <p className="text-sm font-semibold text-gray-900 dark:text-white">
                {formatAction(entry.action)}
              </p>
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
    '2FA_ENABLED': 'Two-factor authentication enabled',
    '2FA_DISABLED': 'Two-factor authentication disabled',
    '2FA_VERIFIED': 'Two-factor code verified',
    BACKUP_CODES_REGENERATED: 'Backup codes regenerated',
    PASSKEY_REGISTERED: 'Passkey registered',
    PASSKEY_REMOVED: 'Passkey removed',
  };
  return labels[action] ?? action.replaceAll('_', ' ');
}
