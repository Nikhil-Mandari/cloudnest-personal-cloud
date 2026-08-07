import { useEffect, useState } from 'react';
import { CheckCircle2, Code2, Database, ExternalLink, HardDrive, RefreshCw, Server, XCircle } from 'lucide-react';

import { Brand } from '@/components/common/Brand';
import { PageHeader } from '@/components/common/PageHeader';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import {
  APP_DEVELOPER,
  APP_GITHUB_URL,
  APP_NAME,
  APP_TAGLINE,
  APP_TECH_STACK,
  APP_VERSION,
  BACKEND_VERSION,
} from '@/constants/app';
import { fetchGatewayHealth, type SystemHealth } from '@/services/system.service';
import { cn } from '@/utils/cn';
import { formatRelativeTime } from '@/utils/format';

interface StatusRowProps {
  label: string;
  status: 'up' | 'down' | 'unknown' | 'checking';
  detail?: string;
}

function StatusDot({ status }: Pick<StatusRowProps, 'status'>) {
  return (
    <span
      className={cn(
        'relative flex h-2.5 w-2.5',
        status === 'up' && 'text-emerald-500',
        status === 'down' && 'text-rose-500',
        (status === 'unknown' || status === 'checking') && 'text-gray-400',
      )}
    >
      {(status === 'up' || status === 'checking') && (
        <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-current opacity-40" />
      )}
      <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-current" />
    </span>
  );
}

function StatusRow({ label, status, detail }: StatusRowProps) {
  return (
    <div className="flex items-center justify-between py-2.5">
      <span className="flex items-center gap-2.5 text-sm text-gray-700 dark:text-gray-200">
        <StatusDot status={status} />
        {label}
      </span>
      <span className="text-sm font-medium text-gray-500 dark:text-gray-400">
        {status === 'up' ? 'Online' : status === 'down' ? 'Offline' : status === 'checking' ? 'Checking…' : detail ?? 'Unknown'}
      </span>
    </div>
  );
}

export function AboutPage() {
  const [health, setHealth] = useState<SystemHealth | null>(null);
  const [checking, setChecking] = useState(true);

  const probe = () => {
    setChecking(true);
    void fetchGatewayHealth().then((result) => {
      setHealth(result);
      setChecking(false);
    });
  };

  useEffect(() => {
    // Initial probe — only asynchronous state updates (no sync setState in the
    // effect body, which would trigger cascading renders).
    void fetchGatewayHealth().then((result) => {
      setHealth(result);
      setChecking(false);
    });
  }, []);

  const gatewayStatus: StatusRowProps['status'] = checking ? 'checking' : health?.status === 'UP' ? 'up' : health?.status === 'DOWN' ? 'down' : 'unknown';

  return (
    <div className="space-y-6">
      <PageHeader title="About" description="Everything you wanted to know about this build." />

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Brand card */}
        <Card className="lg:col-span-2">
          <CardBody className="flex flex-col items-center gap-4 px-8 py-10 text-center">
            <Brand />
            <h2 className="text-xl font-bold tracking-tight text-gray-900 dark:text-white">{APP_NAME}</h2>
            <p className="max-w-md text-sm text-gray-500 dark:text-gray-400">{APP_TAGLINE}</p>

            <div className="mt-2 flex flex-wrap justify-center gap-2">
              <VersionChip label="Frontend" value={`v${APP_VERSION}`} />
              <VersionChip label="Backend" value={`v${BACKEND_VERSION}`} />
              <VersionChip label="License" value="MIT" />
            </div>

            <a
              href={APP_GITHUB_URL}
              target="_blank"
              rel="noreferrer"
              className="text-brand-600 hover:text-brand-700 dark:text-brand-400 mt-2 inline-flex items-center gap-1.5 text-sm font-medium transition-colors hover:underline"
            >
              <Code2 className="h-4 w-4" />
              github.com/cloudnest/cloudnest-personal-cloud
              <ExternalLink className="h-3.5 w-3.5" />
            </a>
          </CardBody>
        </Card>

        {/* System status */}
        <Card>
          <CardHeader
            title="System status"
            action={
              <button
                type="button"
                onClick={probe}
                aria-label="Re-check status"
                className="text-gray-400 transition-colors hover:text-gray-600 dark:hover:text-gray-200"
              >
                <RefreshCw className={cn('h-4 w-4', checking && 'animate-spin')} />
              </button>
            }
          />
          <CardBody className="divide-y divide-gray-100 dark:divide-gray-800">
            <StatusRow label="API Gateway" status={gatewayStatus} detail="No response" />
            <StatusRow
              label="Microservices"
              status={checking ? 'checking' : health && health.services.length > 0 ? 'up' : 'unknown'}
              detail={health ? `${health.services.length} registered` : undefined}
            />
            <StatusRow label="MySQL" status="unknown" detail="Per-service databases" />
            <StatusRow label="MinIO" status="unknown" detail="Object storage" />
            {health?.checkedAt && (
              <p className="pt-3 text-xs text-gray-400">Checked {formatRelativeTime(health.checkedAt)}</p>
            )}
          </CardBody>
        </Card>
      </div>

      {/* Developer info */}
      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader title="Technology stack" description="What powers CloudNest." />
          <CardBody>
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
          </CardBody>
        </Card>

        <Card>
          <CardHeader title="Developer" description="Maintained with care." />
          <CardBody className="space-y-3">
            <div className="flex items-center gap-3">
              <div className="bg-brand-600/10 text-brand-600 dark:bg-brand-500/15 dark:text-brand-300 grid h-12 w-12 place-items-center rounded-xl">
                <Server className="h-6 w-6" />
              </div>
              <div>
                <p className="font-semibold text-gray-900 dark:text-white">{APP_DEVELOPER}</p>
                <p className="text-sm text-gray-500 dark:text-gray-400">Enterprise Cloud Storage Platform</p>
              </div>
            </div>
            <div className="flex items-center gap-3 pt-2">
              <Database className="text-gray-400 h-4 w-4" />
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Spring Boot 3 microservices · React 19 · MySQL · MinIO
              </p>
            </div>
            <div className="flex items-center gap-3">
              <HardDrive className="text-gray-400 h-4 w-4" />
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Released under the MIT license — free to use, modify and share.
              </p>
            </div>
          </CardBody>
        </Card>
      </div>

      {/* Status legend */}
      <div className="flex items-center gap-5 text-xs text-gray-400">
        <span className="flex items-center gap-1.5">
          <CheckCircle2 className="text-emerald-500 h-4 w-4" /> Online
        </span>
        <span className="flex items-center gap-1.5">
          <XCircle className="text-rose-500 h-4 w-4" /> Offline
        </span>
        <span className="flex items-center gap-1.5">
          <Server className="h-4 w-4" /> Unknown (not probed)
        </span>
      </div>
    </div>
  );
}

function VersionChip({ label, value }: { label: string; value: string }) {
  return (
    <span className="rounded-lg border border-gray-200 bg-gray-50 px-3 py-1.5 text-xs font-medium text-gray-600 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-300">
      <span className="text-gray-400">{label}:</span> {value}
    </span>
  );
}
