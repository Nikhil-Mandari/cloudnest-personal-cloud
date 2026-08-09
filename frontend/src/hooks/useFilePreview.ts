import { useCallback, useEffect, useRef, useState } from 'react';
import axios from 'axios';

import { fileService } from '@/services/file.service';
import type { FileItem } from '@/types';
import { getErrorMessage } from '@/utils/error';
import { getFileTypeCategory } from '@/utils/file';

export type PreviewStatus = 'loading' | 'ready' | 'unsupported' | 'error';

export interface FilePreviewState {
  status: PreviewStatus;
  blob: Blob | null;
  message?: string;
}

const TEXT_PATTERN = /^text\//;

/** Whether a fetched blob can be rendered inline by the preview modal. */
export function isRenderableBlob(blob: Blob, file: FileItem): boolean {
  const type = blob.type ?? '';
  if (
    type.startsWith('image/') ||
    type.startsWith('video/') ||
    type.startsWith('audio/') ||
    type === 'application/pdf' ||
    TEXT_PATTERN.test(type)
  ) {
    return true;
  }
  // Code files are plain text regardless of how the backend labelled them
  // (`application/octet-stream`, `application/json`, `application/xml`, …) —
  // read them as text unless the blob is genuinely media or a PDF.
  return (
    getFileTypeCategory(file) === 'code' &&
    !type.startsWith('image/') &&
    !type.startsWith('video/') &&
    !type.startsWith('audio/') &&
    type !== 'application/pdf'
  );
}

/**
 * Fetches a file's preview content for the preview modal.
 *
 * Images, PDFs and text use the preview endpoint (`GET /files/{id}/preview`).
 * Videos and audio stream through the download endpoint because the preview
 * endpoint rejects media content types — the blob URL still plays fine.
 *
 * The returned blob's MIME type (plus the file category for code files)
 * decides whether it can be rendered inline; a backend 400 (unsupported
 * type) is surfaced as `unsupported`.
 */
export function useFilePreview(file: FileItem | null) {
  const [state, setState] = useState<FilePreviewState>({ status: 'loading', blob: null });
  const [currentFile, setCurrentFile] = useState<FileItem | null>(null);
  const loadKeyRef = useRef(0);

  // Reset to the loading state whenever the target file changes. This runs
  // during render (React's documented adjust-state-when-props-change pattern),
  // so the effect below only performs the async fetch.
  if (file !== currentFile) {
    setCurrentFile(file);
    setState({ status: 'loading', blob: null });
  }

  const load = useCallback(async (target: FileItem) => {
    const loadKey = ++loadKeyRef.current;

    try {
      const category = getFileTypeCategory(target);
      const streamsMedia = category === 'video' || category === 'audio';
      const { data } = streamsMedia
        ? await fileService.downloadFile(target.id)
        : await fileService.previewFile(target.id);

      if (loadKeyRef.current !== loadKey) {
        return; // superseded by a newer load
      }
      setState({
        status: isRenderableBlob(data, target) ? 'ready' : 'unsupported',
        blob: data,
        message: 'Preview is not available for this file type.',
      });
    } catch (error) {
      if (loadKeyRef.current !== loadKey) {
        return;
      }
      if (axios.isAxiosError(error) && error.response?.status === 400) {
        setState({
          status: 'unsupported',
          blob: null,
          message: 'Preview is not supported for this file type.',
        });
      } else {
        setState({
          status: 'error',
          blob: null,
          message: getErrorMessage(error, 'Failed to load the preview.'),
        });
      }
    }
  }, []);

  // Fetch the preview whenever the target file changes. Deferred to a microtask
  // so the loading reset (applied during render) paints before the async fetch;
  // it also keeps the fetch out of the synchronous effect body.
  useEffect(() => {
    if (file) {
      void Promise.resolve().then(() => load(file));
    }
  }, [file, load]);

  const retry = useCallback(() => {
    if (file) {
      setState({ status: 'loading', blob: null });
      void load(file);
    }
  }, [file, load]);

  return { state, retry };
}
