import logoUrl from '@/assets/logo.svg';
import { cn } from '@/utils/cn';

export interface BrandProps {
  /** Icons only — hides the wordmark (collapsed sidebar). */
  compact?: boolean;
  /** Use light text for use on dark/gradient backgrounds. */
  inverted?: boolean;
  className?: string;
}

export function Brand({ compact = false, inverted = false, className }: BrandProps) {
  return (
    <div className={cn('flex items-center gap-2.5', className)}>
      <img
        src={logoUrl}
        alt="CloudNest logo"
        className="shadow-brand-600/20 h-9 w-9 shrink-0 rounded-xl shadow-md"
      />
      {!compact && (
        <div className="leading-tight">
          <p
            className={cn(
              'text-base font-bold tracking-tight',
              inverted ? 'text-white' : 'text-gray-900 dark:text-white',
            )}
          >
            CloudNest
          </p>
          <p
            className={cn(
              'text-xs',
              inverted ? 'text-white/70' : 'text-gray-500 dark:text-gray-400',
            )}
          >
            Personal Cloud
          </p>
        </div>
      )}
    </div>
  );
}
