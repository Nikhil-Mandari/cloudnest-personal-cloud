import { useCallback, useEffect, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';

import {
  MAX_FILE_SIZE_BYTES,
  MAX_FILE_SIZE_MB,
  MAX_FILES_PER_BATCH,
  UPLOAD_CONCURRENCY,
} from '@/constants/files';
import { fileService } from '@/services/file.service';
import type { DuplicateAction, DuplicateFileInfo, UploadResult } from '@/types';
import { getErrorMessage } from '@/utils/error';
import { FILES_QUERY_KEY } from './useFiles';

/** Cache scope used by `useFilesQuery` for the root level. */
const ROOT_SCOPE = 'root';

/** Cache scope used by `useFilesQuery` for the dashboard / global view. */
const ALL_SCOPE = 'all';

export type UploadStatus =
  | 'queued'
  | 'uploading'
  | 'paused'
  | 'duplicate'
  | 'done'
  | 'error';

export interface UploadTask {
  id: string;
  file: File;
  progress: number;
  status: UploadStatus;
  error?: string;
  /** Server-side id of the created file (set once the upload resolves). */
  serverId?: number;
  /** Existing file with identical content, when the server flagged a duplicate. */
  duplicateOf?: DuplicateFileInfo | null;
}

/** A duplicate-content upload awaiting the user's decision. */
export interface DuplicatePrompt {
  taskId: string;
  fileName: string;
  duplicateOf: DuplicateFileInfo;
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
 * cancellation, pause/resume, retry, SHA-256 duplicate detection (the server
 * computes the checksum and may ask the user what to do) and React Query
 * invalidation on completion.
 */
export function useFileUpload({ folderId }: UseFileUploadOptions = {}) {
  const queryClient = useQueryClient();
  const [tasks, setTasks] = useState<UploadTask[]>([]);
  const [duplicatePrompt, setDuplicatePrompt] = useState<DuplicatePrompt | null>(null);

  const tasksRef = useRef<UploadTask[]>([]);
  const controllersRef = useRef(new Map<string, AbortController>());
  const batchRunningRef = useRef(false);
  /** Per-task duplicate action chosen by the user for the next upload attempt. */
  const retryActionRef = useRef(new Map<string, DuplicateAction>());

  // Latest destination folder, read at upload time so auto-started tasks
  // (dropped files / initialFiles) never upload with a stale mount-time value.
  const folderIdRef = useRef(folderId);
  useEffect(() => {
    folderIdRef.current = folderId;
  }, [folderId]);

  const syncTasks = useCallback((updater: (prev: UploadTask[]) => UploadTask[]) => {
    // Compute from the ref directly (not inside the setState updater) so
    // `tasksRef.current` is current the moment this returns. The duplicate
    // guard in `addFiles` reads the ref, and two adds in the same tick (drag &
    // drop + dialog pick, a double-click, or a StrictMode double-firing of the
    // initial-files effect) would otherwise both see the stale empty list and
    // queue the same file twice — creating two uploads and two server records.
    const next = updater(tasksRef.current);
    tasksRef.current = next;
    setTasks(next);
  }, []);

  const patchTask = useCallback(
    (
      id: string,
      patch: Partial<UploadTask> | ((current: UploadTask) => Partial<UploadTask>),
    ) => {
      syncTasks((prev) =>
        prev.map((task) =>
          task.id === id
            ? { ...task, ...(typeof patch === 'function' ? patch(task) : patch) }
            : task,
        ),
      );
    },
    [syncTasks],
  );

  /**
   * Handles the server's upload result. On a duplicate flagged with ASK the
   * task is parked in the `duplicate` state and a prompt is raised; otherwise
   * the task completes (or the file record is linked from the result).
   */
  const handleUploadResult = useCallback(
    (task: UploadTask, result: UploadResult) => {
      if (result.duplicate && result.actionTaken === 'ASK' && result.duplicateOf) {
        patchTask(task.id, { status: 'duplicate', duplicateOf: result.duplicateOf });
        setDuplicatePrompt({
          taskId: task.id,
          fileName: task.file.name,
          duplicateOf: result.duplicateOf,
        });
        return;
      }

      if (result.file?.id !== undefined) {
        patchTask(task.id, { status: 'done', progress: 100, serverId: result.file.id });
      } else {
        // SKIP / REPLACE handled server-side — the file record may be absent.
        patchTask(task.id, { status: 'done', progress: 100 });
      }
    },
    [patchTask],
  );

  /** Uploads a single task to completion (or failure / cancellation). */
  const uploadOne = useCallback(
    async (task: UploadTask) => {
      const controller = new AbortController();
      controllersRef.current.set(task.id, controller);

      const action = retryActionRef.current.get(task.id) ?? ('ASK' as DuplicateAction);

      patchTask(task.id, { status: 'uploading', progress: 0, error: undefined });
      try {
        const { data } = await fileService.uploadFile(task.file, {
          folderId: folderIdRef.current,
          signal: controller.signal,
          onDuplicate: action,
          onProgress: (percent) => patchTask(task.id, { progress: percent }),
        });

        // Invoke the invalidation AFTER handling duplicates so skipped /
        // pending-decision uploads still refresh the folder view.
        const scope = folderIdRef.current ? String(folderIdRef.current) : ROOT_SCOPE;
        void queryClient.invalidateQueries({ queryKey: [...FILES_QUERY_KEY, scope] });
        void queryClient.invalidateQueries({ queryKey: [...FILES_QUERY_KEY, ALL_SCOPE] });

        handleUploadResult(task, data.data);
      } catch (error) {
        if (controller.signal.aborted) {
          // Pause / remove already own the task state — only a plain abort with
          // no follow-up (e.g. unmount) is marked as cancelled here.
          patchTask(task.id, (current) =>
            current.status === 'uploading'
              ? { ...current, status: 'error', error: 'Upload cancelled' }
              : current,
          );
        } else {
          const message = getErrorMessage(error, 'Upload failed');
          patchTask(task.id, { status: 'error', error: message });
          toast.error(`"${task.file.name}" failed to upload`);
        }
      } finally {
        retryActionRef.current.delete(task.id);
        controllersRef.current.delete(task.id);
      }
    },
    [patchTask, queryClient, handleUploadResult],
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

      // ── Duplicate upload guard ───────────────────────────────────────────
      // The same physical file can arrive twice (drag & drop onto the page AND
      // picking it in the dialog, a double-click, or a StrictMode double-firing
      // of the initial-files effect). Uploading it twice would create two
      // server records, so identical files still queued or in flight are
      // skipped. Finished/errored tasks are not treated as duplicates — that
      // would block intentionally re-uploading or retrying a file.
      // Files without a modification timestamp (`lastModified === 0`, e.g.
      // programmatically-created File objects) are never deduped, since two
      // genuinely different files could share the same name + size.
      const activeDuplicates = new Set<string>();
      for (const task of tasksRef.current) {
        const active = task.status === 'queued' || task.status === 'uploading';
        if (active && task.file.lastModified > 0) {
          activeDuplicates.add(`${task.file.name}|${task.file.size}|${task.file.lastModified}`);
        }
      }
      const duplicates: string[] = [];

      for (const file of incoming) {
        const fingerprint =
          file.lastModified > 0 ? `${file.name}|${file.size}|${file.lastModified}` : null;
        if (fingerprint && activeDuplicates.has(fingerprint)) {
          duplicates.push(`"${file.name}"`);
        } else if (file.size === 0) {
          rejected.push(`"${file.name}" is empty`);
        } else if (file.size > MAX_FILE_SIZE_BYTES) {
          rejected.push(`"${file.name}" exceeds the ${MAX_FILE_SIZE_MB} MB limit`);
        } else {
          if (fingerprint) {
            activeDuplicates.add(fingerprint);
          }
          accepted.push(file);
        }
      }

      if (duplicates.length > 0) {
        toast.info(
          `${duplicates.length} file${duplicates.length === 1 ? '' : 's'} already in the queue — ` +
            `${duplicates.slice(0, 3).join(', ')}${duplicates.length > 3 ? '…' : ''}`,
        );
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

  /**
   * Resolves a duplicate-content prompt. SKIP finishes the task without
   * uploading; KEEP_BOTH and REPLACE re-upload with the chosen action.
   */
  const resolveDuplicate = useCallback(
    (action: Exclude<DuplicateAction, 'ASK'>) => {
      const prompt = duplicatePrompt;
      if (!prompt) {
        return;
      }
      setDuplicatePrompt(null);

      if (action === 'SKIP') {
        patchTask(prompt.taskId, { status: 'done', progress: 100 });
        toast.info(`Skipped duplicate — no changes made`);
        return;
      }

      retryActionRef.current.set(prompt.taskId, action);
      patchTask(prompt.taskId, {
        status: 'queued',
        progress: 0,
        duplicateOf: undefined,
        error: undefined,
      });
      window.setTimeout(() => start(), 0);
    },
    // `start` is intentionally re-created each render — see `addFiles` above.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [duplicatePrompt, patchTask],
  );

  /** Closes the duplicate prompt and parks the task as an error. */
  const dismissDuplicate = useCallback(() => {
    const prompt = duplicatePrompt;
    if (!prompt) {
      return;
    }
    setDuplicatePrompt(null);
    patchTask(prompt.taskId, { status: 'error', error: 'Duplicate not resolved' });
  }, [duplicatePrompt, patchTask]);

  /** Aborts an in-flight upload and removes the task from the queue. */
  const removeTask = useCallback(
    (id: string) => {
      controllersRef.current.get(id)?.abort();
      controllersRef.current.delete(id);
      retryActionRef.current.delete(id);
      syncTasks((prev) => prev.filter((task) => task.id !== id));
    },
    [syncTasks],
  );

  /** Aborts an in-flight upload, keeping the task for a later retry. */
  const cancelTask = useCallback((id: string) => {
    controllersRef.current.get(id)?.abort();
  }, []);

  /** Aborts an in-flight upload and parks the task as paused (kept in queue). */
  const pauseTask = useCallback(
    (id: string) => {
      controllersRef.current.get(id)?.abort();
      controllersRef.current.delete(id);
      patchTask(id, { status: 'paused', error: undefined });
    },
    [patchTask],
  );

  /** Re-queues a paused task so the next batch picks it up again. */
  const resumeTask = useCallback(
    (id: string) => {
      patchTask(id, { status: 'queued', progress: 0, error: undefined });
      window.setTimeout(() => start(), 0);
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [patchTask],
  );

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

  return {
    tasks,
    duplicatePrompt,
    addFiles,
    start,
    removeTask,
    cancelTask,
    pauseTask,
    resumeTask,
    retryTask,
    clearFinished,
    resolveDuplicate,
    dismissDuplicate,
  };
}
