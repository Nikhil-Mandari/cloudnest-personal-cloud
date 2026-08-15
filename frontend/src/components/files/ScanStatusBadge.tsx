import { CheckCircle2, Loader2, ShieldAlert, ShieldQuestion, XCircle } from 'lucide-react';

import type { ScanStatus } from '@/types';
import { cn } from '@/utils/cn';

const STATUS_META: Record<
  ScanStatus,
  { label: string; icon: typeof ShieldQuestion; className: string }
> = {
  CLEAN: {
    label: 'Scanned · clean',
    icon: CheckCircle2,
    className: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400',
  },
  PENDING: {
    label: 'Scan pending',
    icon: ShieldQuestion,
    className: 'bg-gray-500/10 text-gray-500 dark:text-gray-400',
  },
  SCANNING: {
    label: 'Scanning…',
    icon: Loader2,
    className: 'bg-sky-500/10 text-sky-600 dark:text-sky-400',
  },
  INFECTED: {
    label: 'Blocked · virus',
    icon: XCircle,
    className: 'bg-rose-500/10 text-rose-600 dark:text-rose-400',
  },
  ERROR: {
    label: 'Scan error',
    icon: ShieldAlert,
    className: 'bg-amber-500/10 text-amber-600 dark:text-amber-400',
  },
};

export interface ScanStatusBadgeProps {
  status?: ScanStatus | null;
  /** Compact badge for dense rows. */
  compact?: boolean;
}

/** Colour-coded virus-scan status pill shown next to files. */
export function ScanStatusBadge({ status, compact = false }: ScanStatusBadgeProps) {
  const normalized: ScanStatus = status ?? 'CLEAN';
  const meta = STATUS_META[normalized] ?? STATUS_META.CLEAN;
  const Icon = meta.icon;

  return (
    <span
      title={meta.label}
      className={cn(
        'inline-flex shrink-0 items-center gap-1 rounded-full font-semibold',
        compact ? 'px-1.5 py-0.5 text-[9px]' : 'px-2 py-0.5 text-[10px]',
        meta.className,
      )}
    >
      <Icon
        className={cn(
          compact ? 'h-2.5 w-2.5' : 'h-3 w-3',
          normalized === 'SCANNING' && 'animate-spin',
        )}
      />
      {!compact && meta.label}
    </span>
  );
}
