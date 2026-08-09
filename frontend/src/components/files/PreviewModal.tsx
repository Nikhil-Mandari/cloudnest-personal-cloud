import { useEffect, useMemo, useState } from 'react';
import {
  Download,
  Eye,
  FileWarning,
  Maximize2,
  Minimize2,
  Music,
  RefreshCw,
} from 'lucide-react';

import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { useFilePreview } from '@/hooks/useFilePreview';
import { useFullscreen } from '@/hooks/useFullscreen';
import type { FileItem } from '@/types';
import { cn } from '@/utils/cn';
import { getFileExtension, getFileTypeCategory } from '@/utils/file';
import { formatBytes } from '@/utils/format';
import { ImagePreview } from './ImagePreview';

export interface PreviewModalProps {
  file: FileItem | null;
  open: boolean;
  onClose: () => void;
  onDownload: (file: FileItem) => void;
}

/** Inline preview for images, videos, PDFs and text files (file-service preview endpoint). */
export function PreviewModal({ file, open, onClose, onDownload }: PreviewModalProps) {
  const { state, retry } = useFilePreview(file);

  // Fullscreen is applied to the preview stage itself, so it works for every
  // renderable type (image / video / audio / PDF / text).
  const [previewElement, setPreviewElement] = useState<HTMLDivElement | null>(null);
  const { isFullscreen, supported: fullscreenSupported, toggle: toggleFullscreen } =
    useFullscreen(previewElement);

  const blobType = state.blob?.type ?? '';

  // Object URL for the fetched blob — created once per blob and revoked when
  // it changes or the modal unmounts.
  const objectUrl = useMemo(
    () => (state.blob ? URL.createObjectURL(state.blob) : null),
    [state.blob],
  );

  useEffect(() => {
    return () => {
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [objectUrl]);

  // Decode text blobs for display, keyed to the current blob so a previous
  // file's content never flashes while a new one is loading.
  const [textState, setTextState] = useState<{ blob: Blob; text: string } | null>(null);
  const textContent = textState && state.blob === textState.blob ? textState.text : null;

  useEffect(() => {
    const blob = state.blob;
    if (!blob) {
      return;
    }
    // Decode text-typed blobs and code files (which may arrive as
    // `application/octet-stream`) so they can be shown in the <pre>.
    const isCode = file ? getFileTypeCategory(file) === 'code' : false;
    if (!blob.type.startsWith('text/') && !isCode) {
      return;
    }
    let cancelled = false;
    void blob.text().then((text) => {
      if (!cancelled) {
        setTextState({ blob, text });
      }
    });
    return () => {
      cancelled = true;
    };
  }, [state.blob, file]);

  // While fullscreen, Escape only exits fullscreen (handled by the browser) —
  // swallow the key so the dialog itself stays open.
  useEffect(() => {
    if (!isFullscreen || !open) {
      return;
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.stopPropagation();
      }
    };
    window.addEventListener('keydown', onKeyDown, true);
    return () => window.removeEventListener('keydown', onKeyDown, true);
  }, [isFullscreen, open]);

  // Safety: never leave the browser stuck in fullscreen after the dialog closes.
  useEffect(() => {
    if (!open && document.fullscreenElement) {
      void document.exitFullscreen().catch(() => {});
    }
  }, [open]);

  if (!file) {
    return null;
  }

  const extension = getFileExtension(file.originalFileName);

  return (
    <Modal
      open={open}
      onClose={onClose}
      size="lg"
      title={file.originalFileName}
      description={`${extension ? `${extension.toUpperCase()} · ` : ''}${formatBytes(file.fileSize)}`}
    >
      <div
        ref={(node) => setPreviewElement(node)}
        className={cn(
          'relative flex h-[65vh] min-h-72 w-full items-center justify-center overflow-hidden rounded-2xl bg-gray-50 dark:bg-gray-950/50',
          // Override the stage's own height in fullscreen: without this, the
          // author `h-[65vh]` rule would beat the UA `:fullscreen` height:100%
          // (author origin wins at equal specificity) and leave a 65vh box.
          'fullscreen:h-full fullscreen:bg-black fullscreen:rounded-none',
        )}
      >
        {/* Fullscreen toggle (available once the preview is ready) */}
        {fullscreenSupported && state.status === 'ready' && objectUrl && (
          <button
            type="button"
            onClick={() => void toggleFullscreen()}
            aria-label={isFullscreen ? 'Exit fullscreen' : 'Enter fullscreen'}
            title={isFullscreen ? 'Exit fullscreen (Esc)' : 'Enter fullscreen'}
            className="absolute top-3 right-3 z-10 grid h-9 w-9 place-items-center rounded-lg border border-gray-200 bg-white/90 text-gray-600 shadow-sm backdrop-blur transition-colors hover:bg-white hover:text-gray-900 dark:border-gray-700 dark:bg-gray-900/90 dark:text-gray-300 dark:hover:bg-gray-800 dark:hover:text-white"
          >
            {isFullscreen ? <Minimize2 className="h-4 w-4" /> : <Maximize2 className="h-4 w-4" />}
          </button>
        )}

        {state.status === 'loading' && (
          <div className="flex flex-col items-center gap-3 text-gray-400 dark:text-gray-500">
            <div className="h-16 w-16 animate-pulse rounded-2xl bg-gray-200 dark:bg-gray-800" />
            <p className="text-sm">Loading preview…</p>
          </div>
        )}

        {state.status === 'ready' && objectUrl && blobType.startsWith('image/') && (
          <ImagePreview src={objectUrl} alt={file.originalFileName} />
        )}

        {state.status === 'ready' && objectUrl && blobType.startsWith('video/') && (
          <video
            key={objectUrl}
            src={objectUrl}
            controls
            playsInline
            className="max-h-full max-w-full rounded-lg bg-black shadow-sm"
          >
            Your browser does not support video preview.
          </video>
        )}

        {state.status === 'ready' && objectUrl && blobType.startsWith('audio/') && (
          <div className="flex w-full max-w-md flex-col items-center gap-4 py-2">
            <span className="from-brand-500 to-accent-600 grid h-16 w-16 place-items-center rounded-2xl bg-linear-to-br text-white shadow-lg shadow-brand-500/25">
              <Music className="h-8 w-8" />
            </span>
            <audio src={objectUrl} controls className="w-full">
              Your browser does not support audio preview.
            </audio>
            <p className="text-xs text-gray-400 dark:text-gray-500">
              {formatBytes(file.fileSize)}
            </p>
          </div>
        )}

        {state.status === 'ready' && objectUrl && blobType === 'application/pdf' && (
          <iframe
            src={objectUrl}
            title={`${file.originalFileName} preview`}
            className="h-full w-full rounded-lg border border-gray-200 bg-white dark:border-gray-700"
          />
        )}

        {state.status === 'ready' &&
          objectUrl &&
          (blobType.startsWith('text/') || getFileTypeCategory(file) === 'code') && (
            <pre className="bg-white dark:bg-gray-900 h-[55vh] w-full overflow-auto rounded-lg border border-gray-200 p-4 text-sm whitespace-pre-wrap text-gray-800 dark:border-gray-700 dark:text-gray-100 fullscreen:h-full">
              {textContent ?? 'Loading…'}
            </pre>
          )}

        {state.status === 'unsupported' && (
          <div className="flex flex-col items-center text-center">
            <span className="grid h-14 w-14 place-items-center rounded-2xl bg-amber-500/10 text-amber-600 dark:text-amber-400">
              <FileWarning className="h-7 w-7" />
            </span>
            <h3 className="mt-4 text-sm font-semibold text-gray-900 dark:text-white">
              Preview not available
            </h3>
            <p className="mt-1 max-w-sm text-sm text-gray-500 dark:text-gray-400">
              {state.message ?? 'This file type cannot be previewed inline.'}
            </p>
            <Button
              variant="outline"
              size="sm"
              className="mt-5"
              leftIcon={<Download className="h-3.5 w-3.5" />}
              onClick={() => onDownload(file)}
            >
              Download file
            </Button>
          </div>
        )}

        {state.status === 'error' && (
          <div className="flex flex-col items-center text-center">
            <span className="grid h-14 w-14 place-items-center rounded-2xl bg-rose-500/10 text-rose-600 dark:text-rose-400">
              <Eye className="h-7 w-7" />
            </span>
            <h3 className="mt-4 text-sm font-semibold text-gray-900 dark:text-white">
              Couldn't load the preview
            </h3>
            {state.message && (
              <p className="mt-1 max-w-sm text-sm text-gray-500 dark:text-gray-400">
                {state.message}
              </p>
            )}
            <div className="mt-5 flex gap-2">
              <Button
                variant="outline"
                size="sm"
                leftIcon={<RefreshCw className="h-3.5 w-3.5" />}
                onClick={retry}
              >
                Try again
              </Button>
              <Button
                variant="outline"
                size="sm"
                leftIcon={<Download className="h-3.5 w-3.5" />}
                onClick={() => onDownload(file)}
              >
                Download
              </Button>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
}
