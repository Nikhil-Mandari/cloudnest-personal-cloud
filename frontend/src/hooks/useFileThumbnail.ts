import { useCallback, useEffect, useRef, useState } from 'react';
import { GlobalWorkerOptions, getDocument } from 'pdfjs-dist';
import pdfWorkerUrl from 'pdfjs-dist/build/pdf.worker.min.mjs?url';

import { fileService } from '@/services/file.service';
import type { FileItem } from '@/types';
import {
  getFileTypeCategory,
  isImageFileName,
  isPdfFile,
  isVideoFileName,
} from '@/utils/file';

// Configure the pdf.js worker once (Vite resolves the ?url asset).
GlobalWorkerOptions.workerSrc = pdfWorkerUrl;

export type ThumbnailStatus = 'idle' | 'loading' | 'ready' | 'unsupported' | 'error';

export interface ThumbnailState {
  status: ThumbnailStatus;
  url?: string;
}

/** Size guards — never stream huge blobs just for a tile. */
const MAX_IMAGE_BYTES = 60 * 1024 * 1024;
const MAX_VIDEO_BYTES = 80 * 1024 * 1024;
const MAX_PDF_BYTES = 40 * 1024 * 1024;

/**
 * Module-level thumbnail cache (keyed by the internal file id). Shared across
 * every card/row so scrolling never refetches. Blob URLs are revoked when
 * entries are evicted (LRU).
 */
const cache = new Map<number, ThumbnailState>();
const MAX_CACHE_SIZE = 120;

/** Components waiting on a shared, in-flight generation for a file id. */
const listeners = new Map<number, Set<() => void>>();

function notify(fileId: number): void {
  listeners.get(fileId)?.forEach((listener) => listener());
}

function remember(id: number, state: ThumbnailState): void {
  cache.set(id, state);
  notify(id);
  if (cache.size <= MAX_CACHE_SIZE) {
    return;
  }
  // Evict the oldest entry nothing is currently displaying — revoking a blob
  // URL that is still on screen (e.g. the details panel) would break it.
  for (const key of cache.keys()) {
    if ((listeners.get(key)?.size ?? 0) === 0) {
      const evicted = cache.get(key);
      cache.delete(key);
      listeners.delete(key);
      if (evicted?.url && !evicted.url.startsWith('data:')) {
        URL.revokeObjectURL(evicted.url);
      }
      break;
    }
  }
}

/** Draws the first frame of a video blob onto a canvas and returns a JPEG URL. */
async function extractVideoFrame(blob: Blob): Promise<string> {
  const objectUrl = URL.createObjectURL(blob);
  try {
    const video = document.createElement('video');
    video.muted = true;
    video.playsInline = true;
    video.preload = 'auto';
    video.src = objectUrl;

    await new Promise<void>((resolve, reject) => {
      video.onloadeddata = () => resolve();
      video.onerror = () => reject(new Error('Failed to load the video'));
      // Safety net in case the media never fires onloadeddata.
      window.setTimeout(() => resolve(), 8000);
    });

    video.currentTime = 0.1;
    await new Promise<void>((resolve) => {
      const done = () => resolve();
      video.onseeked = done;
      video.ontimeupdate = done;
      window.setTimeout(done, 2000);
    });

    const canvas = document.createElement('canvas');
    const targetWidth = 320;
    const width = video.videoWidth || targetWidth;
    const height = video.videoHeight || Math.round(targetWidth * 0.75);
    const scale = targetWidth / width;
    canvas.width = targetWidth;
    canvas.height = Math.max(1, Math.round(height * scale));

    const ctx = canvas.getContext('2d');
    if (!ctx) {
      throw new Error('Canvas 2D context unavailable');
    }
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    return canvas.toDataURL('image/jpeg', 0.8);
  } finally {
    URL.revokeObjectURL(objectUrl);
  }
}

/** Renders the first page of a PDF blob onto a canvas and returns a JPEG URL. */
async function renderPdfPage(blob: Blob): Promise<string> {
  const data = await blob.arrayBuffer();
  const loadingTask = getDocument({ data });
  const doc = await loadingTask.promise;
  try {
    const page = await doc.getPage(1);
    const baseViewport = page.getViewport({ scale: 1 });
    const scale = Math.min(1, 300 / baseViewport.width);
    const viewport = page.getViewport({ scale });

    const canvas = document.createElement('canvas');
    canvas.width = viewport.width;
    canvas.height = viewport.height;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      throw new Error('Canvas 2D context unavailable');
    }
    await page.render({ canvas, canvasContext: ctx, viewport }).promise;
    return canvas.toDataURL('image/jpeg', 0.85);
  } finally {
    void loadingTask.destroy();
  }
}

/** Builds the thumbnail for a file (fetch → render) or reports unsupported. */
async function generateThumbnail(file: FileItem): Promise<ThumbnailState> {
  const category = getFileTypeCategory(file);

  if (category === 'image' || isImageFileName(file.originalFileName)) {
    if (file.fileSize > MAX_IMAGE_BYTES) {
      return { status: 'unsupported' };
    }
    const { data } = await fileService.downloadFile(file.id);
    return { status: 'ready', url: URL.createObjectURL(data) };
  }

  if (isVideoFileName(file.originalFileName) || category === 'video') {
    if (file.fileSize > MAX_VIDEO_BYTES) {
      return { status: 'unsupported' };
    }
    const { data } = await fileService.downloadFile(file.id);
    const frame = await extractVideoFrame(data);
    return { status: 'ready', url: frame };
  }

  if (isPdfFile(file)) {
    if (file.fileSize > MAX_PDF_BYTES) {
      return { status: 'unsupported' };
    }
    const { data } = await fileService.downloadFile(file.id);
    const page = await renderPdfPage(data);
    return { status: 'ready', url: page };
  }

  return { status: 'unsupported' };
}

/**
 * Lazily produces a cached thumbnail for a file.
 *
 * - `enabled` gates the fetch (callers only flip it on once the tile scrolls
 *   into view), so thousands of cards never fire thousands of requests.
 * - Images render directly; videos use a canvas-extracted first frame; PDFs
 *   render page one via pdf.js. Everything else reports `unsupported` and the
 *   caller falls back to `FileIcon`.
 * - Results are pushed to every subscriber, so the same file shown in two
 *   places (grid card + details panel) only triggers one generation.
 */
export function useFileThumbnail(file: FileItem | null, enabled = true) {
  const fileId = file?.id ?? null;
  const [state, setState] = useState<ThumbnailState>(() =>
    fileId !== null ? (cache.get(fileId) ?? { status: 'idle' }) : { status: 'idle' },
  );
  const [attempt, setAttempt] = useState(0);
  const previousIdRef = useRef<number | null>(null);

  // Render-time sync: reset state whenever the target file changes (the
  // sanctioned "adjusting state during render" pattern).
  if (fileId !== previousIdRef.current) {
    previousIdRef.current = fileId;
    setState(fileId !== null ? (cache.get(fileId) ?? { status: 'idle' }) : { status: 'idle' });
  }

  // Subscribe to cache updates so a shared generation resolves for everyone.
  useEffect(() => {
    if (fileId === null) {
      return;
    }
    const listener = () => {
      const next = cache.get(fileId);
      if (next) {
        setState(next);
      }
    };
    if (!listeners.has(fileId)) {
      listeners.set(fileId, new Set());
    }
    listeners.get(fileId)?.add(listener);
    return () => {
      listeners.get(fileId)?.delete(listener);
    };
  }, [fileId]);

  useEffect(() => {
    if (file === null || fileId === null || !enabled) {
      return;
    }
    const cached = cache.get(fileId);
    // A finished result is already synced via render-time / subscription.
    if (cached && cached.status !== 'loading') {
      return;
    }
    // Another instance is generating it — the subscription delivers the result.
    if (cached?.status === 'loading') {
      return;
    }

    cache.set(fileId, { status: 'loading' });

    void generateThumbnail(file)
      .then((result) => remember(fileId, result))
      .catch(() => remember(fileId, { status: 'error' }));

    return () => {
      // Drop the in-flight marker so a future mount retries cleanly. If the
      // generation still resolves afterwards, `remember` simply overwrites it
      // and notifies any remaining subscribers (e.g. the details panel).
      if (cache.get(fileId)?.status === 'loading') {
        cache.delete(fileId);
      }
    };
  }, [file, fileId, enabled, attempt]);

  const retry = useCallback(() => {
    if (fileId !== null) {
      cache.delete(fileId);
      setAttempt((value) => value + 1);
    }
  }, [fileId]);

  return { state, retry };
}
