import type { ReactNode } from 'react';

import { cn } from '@/utils/cn';

export interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
  className?: string;
}

export function EmptyState({ icon, title, description, action, className }: EmptyStateProps) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center rounded-2xl border border-dashed border-gray-300/80 px-6 py-14 text-center dark:border-gray-700',
        className,
      )}
    >
      {icon && (
        <div className="bg-brand-500/10 text-brand-600 dark:bg-brand-400/10 dark:text-brand-300 mb-4 grid h-14 w-14 place-items-center rounded-2xl">
          {icon}
        </div>
      )}
      <h3 className="text-sm font-semibold text-gray-900 dark:text-white">{title}</h3>
      {description && (
        <p className="mt-1.5 max-w-sm text-sm text-gray-500 dark:text-gray-400">{description}</p>
      )}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}
