import { useCallback, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';

import {
  MAX_FILE_SIZE_BYTES,
  MAX_FILE_SIZE_MB,
  MAX_FILES_PER_BATCH,
  UPLOAD_CONCURRENCY,
} from '@/constants/files';
import { fileService } from '@/services/file.service';
import { getErrorMessage } from '@/utils/error';
import { FILES_QUERY_KEY } from './useFiles';

export type UploadStatus = 'queued' | 'uploading' | 'done' | 'error';

export interface UploadTask {
  id: string;
  file: File;
  progress: number;
  status: UploadStatus;
  error?: string;
}

interface UseFileUploadOptions {
  /** Destination folder UUID (omitted = root). */
  folderId?: string | null;
}

function createTaskId(): string {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `upload-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

/**
 * Manages a queue of file uploads with per-file progress, validation,
 * cancellation, retry and React Query invalidation on completion.
 */
export function useFileUpload({ folderId }: UseFileUploadOptions = {}) {
  const queryClient = useQueryClient();
  const [tasks, setTasks] = useState<UploadTask[]>([]);

  const tasksRef = useRef<UploadTask[]>([]);
  const controllersRef = useRef(new Map<string, AbortController>());
  const batchRunningRef = useRef(false);

  const syncTasks = useCallback((updater: (prev: UploadTask[]) => UploadTask[]) => {
    setTasks((prev) => {
      const next = updater(prev);
      tasksRef.current = next;
      return next;
    });
  }, []);

  const patchTask = useCallback(
    (id: string, patch: Partial<UploadTask>) => {
      syncTasks((prev) => prev.map((task) => (task.id === id ? { ...task, ...patch } : task)));
    },
    [syncTasks],
  );

  /** Uploads a single task to completion (or failure / cancellation). */
  const uploadOne = useCallback(
    async (task: UploadTask) => {
      const controller = new AbortController();
      controllersRef.current.set(task.id, controller);

      patchTask(task.id, { status: 'uploading', progress: 0, error: undefined });
      try {
        await fileService.uploadFile(task.file, {
          folderId,
          signal: controller.signal,
          onProgress: (percent) => patchTask(task.id, { progress: percent }),
        });
        patchTask(task.id, { status: 'done', progress: 100 });
        void queryClient.invalidateQueries({ queryKey: FILES_QUERY_KEY });
      } catch (error) {
        if (controller.signal.aborted) {
          patchTask(task.id, { status: 'error', error: 'Upload cancelled' });
        } else {
          const message = getErrorMessage(error, 'Upload failed');
          patchTask(task.id, { status: 'error', error: message });
          toast.error(`"${task.file.name}" failed to upload`);
        }
      } finally {
        controllersRef.current.delete(task.id);
      }
    },
    [folderId, patchTask, queryClient],
  );

  /**
   * Runs every queued task with a bounded pool of concurrent uploads.
   * Re-invoked automatically when tasks are added mid-batch.
   */
  function start() {
    if (batchRunningRef.current) {
      return;
    }

    const queued = tasksRef.current.filter((task) => task.status === 'queued');
    if (queued.length === 0) {
      return;
    }

    batchRunningRef.current = true;
    let index = 0;
    const workerCount = Math.min(UPLOAD_CONCURRENCY, queued.length);

    const workers = Array.from({ length: workerCount }, async () => {
      while (index < queued.length) {
        const task = queued[index];
        index += 1;
        await uploadOne(task);
      }
    });

    void Promise.allSettled(workers).then(() => {
      batchRunningRef.current = false;
      const current = tasksRef.current;
      const succeeded = current.filter((task) => task.status === 'done').length;
      const failed = current.filter((task) => task.status === 'error').length;

      if (succeeded > 0) {
        toast.success(`${succeeded} file${succeeded === 1 ? '' : 's'} uploaded`);
      }
      if (failed > 0) {
        toast.error(`${failed} file${failed === 1 ? '' : 's'} failed to upload`);
      }

      // Pick up anything queued while this batch was running.
      if (tasksRef.current.some((task) => task.status === 'queued')) {
        start();
      }
    });
  }

  /** Validates and queues files; invalid entries are reported via toasts. */
  const addFiles = useCallback(
    (incoming: File[]) => {
      if (incoming.length === 0) {
        return;
      }

      const accepted: File[] = [];
      const rejected: string[] = [];

      for (const file of incoming) {
        if (file.size === 0) {
          rejected.push(`"${file.name}" is empty`);
        } else if (file.size > MAX_FILE_SIZE_BYTES) {
          rejected.push(`"${file.name}" exceeds the ${MAX_FILE_SIZE_MB} MB limit`);
        } else {
          accepted.push(file);
        }
      }

      rejected.forEach((message) => toast.error(message));

      syncTasks((prev) => {
        const room = Math.max(0, MAX_FILES_PER_BATCH - prev.length);
        const toQueue = accepted.slice(0, room);
        const overflow = accepted.length - toQueue.length;
        if (overflow > 0) {
          toast.error(
            `${overflow} file${overflow === 1 ? '' : 's'} not added — batch limit reached`,
          );
        }
        return [
          ...prev,
          ...toQueue.map((file) => ({
            id: createTaskId(),
            file,
            progress: 0,
            status: 'queued' as const,
          })),
        ];
      });

      // Auto-start on the next tick so freshly queued tasks are visible.
      window.setTimeout(() => start(), 0);
    },
    // `start` is intentionally re-created each render; keeping it out of the
    // dependency list avoids re-validating already-queued files.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [syncTasks],
  );

  /** Aborts an in-flight upload and removes the task from the queue. */
  const removeTask = useCallback(
    (id: string) => {
      controllersRef.current.get(id)?.abort();
      controllersRef.current.delete(id);
      syncTasks((prev) => prev.filter((task) => task.id !== id));
    },
    [syncTasks],
  );

  /** Aborts an in-flight upload, keeping the task for a later retry. */
  const cancelTask = useCallback((id: string) => {
    controllersRef.current.get(id)?.abort();
  }, []);

  /** Re-queues a failed/cancelled task and resumes the queue. */
  const retryTask = useCallback(
    (id: string) => {
      patchTask(id, { status: 'queued', progress: 0, error: undefined });
      window.setTimeout(() => start(), 0);
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [patchTask],
  );

  /** Drops finished (done / error) tasks from the queue. */
  const clearFinished = useCallback(() => {
    syncTasks((prev) =>
      prev.filter((task) => task.status === 'queued' || task.status === 'uploading'),
    );
  }, [syncTasks]);

  return { tasks, addFiles, start, removeTask, cancelTask, retryTask, clearFinished };
}
