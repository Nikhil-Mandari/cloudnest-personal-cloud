import { cn } from '@/utils/cn';
import { Spinner } from '@/components/ui/Spinner';

export interface LoaderProps {
  label?: string;
  /** Renders a fixed full-viewport overlay. */
  fullScreen?: boolean;
  className?: string;
}

export function Loader({ label = 'Loading…', fullScreen = false, className }: LoaderProps) {
  return (
    <div
      role="status"
      className={cn(
        'flex flex-col items-center justify-center gap-3 text-gray-500 dark:text-gray-400',
        fullScreen
          ? 'fixed inset-0 z-50 bg-white/80 backdrop-blur-sm dark:bg-gray-950/80'
          : 'py-20',
        className,
      )}
    >
      <Spinner size="lg" />
      {label && <p className="text-sm font-medium">{label}</p>}
    </div>
  );
}
