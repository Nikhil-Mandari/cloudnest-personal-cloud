import { motion } from 'framer-motion';
import { Ban, CircleCheck, CircleX, RefreshCw, X } from 'lucide-react';

import type { UploadTask } from '@/hooks/useFileUpload';
import { cn } from '@/utils/cn';
import { formatBytes } from '@/utils/format';
import { FileIcon } from './FileIcon';

export interface UploadProgressProps {
  task: UploadTask;
  onCancel: (id: string) => void;
  onRemove: (id: string) => void;
  onRetry: (id: string) => void;
}

const iconButtonClasses =
  'grid h-7 w-7 shrink-0 place-items-center rounded-lg text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-700 dark:hover:bg-gray-800 dark:hover:text-gray-200';

/** Per-file upload row with an animated progress bar and status actions. */
export function UploadProgress({ task, onCancel, onRemove, onRetry }: UploadProgressProps) {
  return (
    <motion.li
      layout
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, x: 16 }}
      transition={{ duration: 0.18, ease: 'easeOut' }}
      className="rounded-xl border border-gray-100 bg-gray-50/60 p-3 dark:border-gray-800 dark:bg-gray-900/60"
    >
      <div className="flex items-center gap-3">
        <FileIcon file={{ originalFileName: task.file.name, fileType: task.file.type }} size="sm" />

        <div className="min-w-0 flex-1">
          <div className="flex items-center justify-between gap-2">
            <p className="truncate text-sm font-medium text-gray-900 dark:text-white">
              {task.file.name}
            </p>
            <span className="shrink-0 text-xs text-gray-400 tabular-nums dark:text-gray-500">
              {formatBytes(task.file.size)}
            </span>
          </div>

          {task.status === 'uploading' && (
            <div className="mt-2">
              <div className="h-1.5 w-full overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
                <motion.div
                  className="from-brand-500 to-accent-500 h-full rounded-full bg-linear-to-r"
                  initial={{ width: 0 }}
                  animate={{ width: `${task.progress}%` }}
                  transition={{ ease: 'easeOut', duration: 0.2 }}
                />
              </div>
              <p className="mt-1 text-[11px] text-gray-400 tabular-nums dark:text-gray-500">
                Uploading… {task.progress}%
              </p>
            </div>
          )}

          {task.status === 'queued' && (
            <p className="mt-1 text-[11px] text-gray-400 dark:text-gray-500">Queued…</p>
          )}

          {task.status === 'done' && (
            <p className="mt-1 flex items-center gap-1 text-[11px] font-medium text-emerald-600 dark:text-emerald-400">
              <CircleCheck className="h-3.5 w-3.5" /> Uploaded
            </p>
          )}

          {task.status === 'error' && (
            <p className="mt-1 flex items-center gap-1 text-[11px] font-medium text-rose-600 dark:text-rose-400">
              <CircleX className="h-3.5 w-3.5" />
              <span className="truncate">{task.error ?? 'Upload failed'}</span>
            </p>
          )}
        </div>

        {/* Actions */}
        <div className="flex shrink-0 items-center gap-0.5">
          {task.status === 'uploading' && (
            <button
              type="button"
              onClick={() => onCancel(task.id)}
              aria-label="Cancel upload"
              className={cn(
                iconButtonClasses,
                'text-rose-500 hover:bg-rose-500/10 hover:text-rose-600',
              )}
            >
              <Ban className="h-4 w-4" />
            </button>
          )}
          {task.status === 'error' && (
            <button
              type="button"
              onClick={() => onRetry(task.id)}
              aria-label="Retry upload"
              className={cn(iconButtonClasses, 'hover:text-brand-600 dark:hover:text-brand-400')}
            >
              <RefreshCw className="h-4 w-4" />
            </button>
          )}
          <button
            type="button"
            onClick={() => onRemove(task.id)}
            aria-label="Remove from queue"
            className={iconButtonClasses}
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      </div>
    </motion.li>
  );
}
