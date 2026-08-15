import { useEffect, useRef, useState } from 'react';
import { motion } from 'framer-motion';
import { Ban, CircleCheck, CircleX, Pause, Play, RefreshCw, TriangleAlert, X } from 'lucide-react';

import type { UploadTask } from '@/hooks/useFileUpload';
import { cn } from '@/utils/cn';
import { formatBytes, formatEta, formatUploadSpeed } from '@/utils/format';
import { FileIcon } from './FileIcon';

export interface UploadProgressProps {
  task: UploadTask;
  onCancel: (id: string) => void;
  onRemove: (id: string) => void;
  onRetry: (id: string) => void;
  onPause?: (id: string) => void;
  onResume?: (id: string) => void;
}

const iconButtonClasses =
  'grid h-7 w-7 shrink-0 place-items-center rounded-lg text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-700 dark:hover:bg-gray-800 dark:hover:text-gray-200';

/**
 * Per-file upload row with an animated progress bar, live speed / ETA and
 * status actions.
 */
export function UploadProgress({
  task,
  onCancel,
  onRemove,
  onRetry,
  onPause,
  onResume,
}: UploadProgressProps) {
  // Instantaneous speed + ETA, computed from progress deltas between ticks.
  const lastTickRef = useRef<{ progress: number; time: number } | null>(null);
  const [speed, setSpeed] = useState(0);
  const [etaSeconds, setEtaSeconds] = useState(0);

  useEffect(() => {
    if (task.status !== 'uploading') {
      lastTickRef.current = null;
      return;
    }
    const now = performance.now();
    const prev = lastTickRef.current;
    let nextSpeed = 0;
    let nextEta = 0;
    if (prev && task.progress > prev.progress) {
      const deltaBytes = ((task.progress - prev.progress) / 100) * task.file.size;
      const deltaSeconds = (now - prev.time) / 1000;
      if (deltaSeconds > 0) {
        nextSpeed = deltaBytes / deltaSeconds;
        nextEta = ((task.file.size * (100 - task.progress)) / 100) / Math.max(nextSpeed, 1);
      }
    }
    lastTickRef.current = { progress: task.progress, time: now };
    // Publish the measurement on the next frame so state is never written
    // synchronously from the effect body (avoids cascading renders).
    const frame = window.requestAnimationFrame(() => {
      setSpeed(nextSpeed);
      setEtaSeconds(nextEta);
    });
    return () => window.cancelAnimationFrame(frame);
  }, [task.status, task.progress, task.file.size]);

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
              <p className="mt-1 flex items-center gap-2 text-[11px] text-gray-400 tabular-nums dark:text-gray-500">
                <span>Uploading… {task.progress}%</span>
                {speed > 0 && (
                  <>
                    <span aria-hidden="true" className="text-gray-300 dark:text-gray-600">
                      ·
                    </span>
                    <span>{formatUploadSpeed(speed)}</span>
                    <span aria-hidden="true" className="text-gray-300 dark:text-gray-600">
                      ·
                    </span>
                    <span>~{formatEta(etaSeconds)} left</span>
                  </>
                )}
              </p>
            </div>
          )}

          {task.status === 'queued' && (
            <p className="mt-1 text-[11px] text-gray-400 dark:text-gray-500">Queued…</p>
          )}

          {task.status === 'paused' && (
            <p className="mt-1 flex items-center gap-1 text-[11px] font-medium text-amber-600 dark:text-amber-400">
              <Pause className="h-3.5 w-3.5" /> Paused — resume to continue
            </p>
          )}

          {task.status === 'duplicate' && (
            <p className="mt-1 flex items-center gap-1 text-[11px] font-medium text-amber-600 dark:text-amber-400">
              <TriangleAlert className="h-3.5 w-3.5" /> Duplicate content found
            </p>
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
          {task.status === 'uploading' && onPause && (
            <button
              type="button"
              onClick={() => onPause(task.id)}
              aria-label="Pause upload"
              title="Pause"
              className={cn(iconButtonClasses, 'hover:text-brand-600 dark:hover:text-brand-400')}
            >
              <Pause className="h-4 w-4" />
            </button>
          )}
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
          {task.status === 'paused' && onResume && (
            <button
              type="button"
              onClick={() => onResume(task.id)}
              aria-label="Resume upload"
              title="Resume"
              className={cn(iconButtonClasses, 'hover:text-emerald-600 dark:hover:text-emerald-400')}
            >
              <Play className="h-4 w-4" />
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
