import { useEffect, useState } from 'react';
import {
  Download,
  Eye,
  FileText,
  FolderOpen,
  Link2,
  Loader2,
  Music,
  ShieldCheck,
  type LucideIcon,
} from 'lucide-react';
import { toast } from 'react-toastify';

import { ExpiryBadge, PermissionBadge } from '@/components/shares/ShareBadges';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { shareService } from '@/services/share.service';
import type { FileTypeCategory, ShareRecord } from '@/types';
import { cn } from '@/utils/cn';
import {
  formatFileDate,
  getFileExtension,
  getFileTypeCategory,
} from '@/utils/file';

export interface PublicShareCardProps {
  share: ShareRecord;
  /** Verified password for protected links (null = open link). */
  password?: string | null;
  downloading: boolean;
  onDownload: () => void;
  onCopyLink: () => void;
  /** Surfaced preview failures (e.g. password no longer accepted). */
  onPreviewError?: (message: string) => void;
}

const CATEGORY_ICONS: Record<FileTypeCategory, LucideIcon> = {
  image: Eye,
  video: FileText,
  audio: FileText,
  document: FileText,
  archive: FileText,
  code: FileText,
  other: FileText,
};

type PreviewKind = 'image' | 'pdf' | 'video' | 'audio' | 'text' | 'unsupported';

/** Browser-unrenderable formats must not attempt inline preview. */
const UNRENDERABLE_EXTENSIONS = new Set(['heic', 'heif']);

const TEXT_EXTENSIONS = new Set([
  'txt',
  'md',
  'json',
  'xml',
  'yml',
  'yaml',
  'csv',
  'log',
  'js',
  'ts',
  'tsx',
  'jsx',
  'html',
  'css',
  'sh',
  'py',
  'java',
  'c',
  'cpp',
  'cs',
  'go',
  'rs',
  'rb',
  'php',
  'sql',
]);

/** Decides how (and whether) a shared file can be previewed in-browser. */
function resolvePreviewKind(fileName: string): PreviewKind {
  const extension = getFileExtension(fileName);
  if (UNRENDERABLE_EXTENSIONS.has(extension)) {
    return 'unsupported';
  }
  const category = getFileTypeCategory({ fileType: '', originalFileName: fileName });
  if (category === 'image') return 'image';
  if (category === 'video') return 'video';
  if (category === 'audio') return 'audio';
  if (extension === 'pdf') return 'pdf';
  if (category === 'code' || TEXT_EXTENSIONS.has(extension)) return 'text';
  return 'unsupported';
}

/**
 * Content card for the public share-link page: resource identity, access
 * metadata, and the Preview / Download / Copy actions. Folders show a note
 * instead of download (link downloads are single-file only).
 */
export function PublicShareCard({
  share,
  password,
  downloading,
  onDownload,
  onCopyLink,
  onPreviewError,
}: PublicShareCardProps) {
  const isFolder = share.resourceType === 'FOLDER';
  const fileName = share.resourceName ?? '';
  const viewOnly = share.permission === 'VIEW';
  const CategoryIcon = isFolder ? FolderOpen : CATEGORY_ICONS[getFileTypeCategory({ fileType: '', originalFileName: fileName })];
  const previewKind = isFolder ? 'unsupported' : resolvePreviewKind(fileName);
  const isPreviewable = previewKind !== 'unsupported';

  // ── In-card preview (streamed through the public preview endpoint) ─────────
  const [previewing, setPreviewing] = useState(false);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [previewError, setPreviewError] = useState<string | null>(null);

  // Decode text previews for display, keyed to the blob so a previous file's
  // content never flashes while a new one loads.
  const [textState, setTextState] = useState<{ blob: Blob; text: string } | null>(null);

  const closePreview = () => {
    setPreviewUrl((current) => {
      if (current) {
        URL.revokeObjectURL(current);
      }
      return null;
    });
    setPreviewError(null);
    setTextState(null);
  };

  const openPreview = async () => {
    setPreviewing(true);
    setPreviewError(null);
    try {
      // The preview endpoint never increments the owner's download counter.
      const { data } = await shareService.previewPublicShare(share.shareToken, password ?? undefined);
      setPreviewUrl(URL.createObjectURL(data));
    } catch {
      const message = 'Could not load the preview. Please try downloading instead.';
      setPreviewError(message);
      toast.error(message);
      onPreviewError?.(message);
    } finally {
      setPreviewing(false);
    }
  };

  // Decode text-typed blobs for the <pre> view.
  useEffect(() => {
    if (!previewUrl || previewKind !== 'text') {
      return;
    }
    let cancelled = false;
    void fetch(previewUrl)
      .then((response) => response.blob())
      .then((blob) => blob.text())
      .then((text) => {
        if (!cancelled) {
          setTextState({ blob: new Blob([text], { type: 'text/plain' }), text });
        }
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [previewUrl, previewKind]);

  return (
    <div className="space-y-5">
      {/* Resource identity */}
      <div className="flex flex-col items-center text-center">
        <div
          className={cn(
            'mb-4 grid h-16 w-16 place-items-center rounded-2xl',
            isFolder
              ? 'bg-amber-500/10 text-amber-600 dark:text-amber-400'
              : 'bg-brand-500/10 text-brand-600 dark:text-brand-400',
          )}
        >
          <CategoryIcon className="h-8 w-8" />
        </div>
        <h2
          title={fileName}
          className="max-w-full truncate text-lg font-semibold text-gray-900 dark:text-white"
        >
          {fileName || (isFolder ? 'Shared folder' : 'Shared file')}
        </h2>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Shared with you through CloudNest
        </p>
        {share.hasPassword && (
          <span className="mt-2 inline-flex items-center gap-1 rounded-full bg-amber-500/10 px-2.5 py-0.5 text-xs font-medium text-amber-600 dark:text-amber-400">
            <ShieldCheck className="h-3.5 w-3.5" /> Password protected
          </span>
        )}
      </div>

      {/* Access metadata */}
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
        <div className="rounded-lg bg-gray-50 p-2.5 dark:bg-gray-900">
          <p className="text-[10px] tracking-wide text-gray-400 uppercase">Type</p>
          <p className="mt-0.5 truncate text-sm font-medium text-gray-900 dark:text-white">
            {isFolder ? 'Folder' : 'File'}
          </p>
        </div>
        <div className="rounded-lg bg-gray-50 p-2.5 dark:bg-gray-900">
          <p className="text-[10px] tracking-wide text-gray-400 uppercase">Permission</p>
          <div className="mt-0.5">
            <PermissionBadge permission={share.permission} />
          </div>
        </div>
        <div className="rounded-lg bg-gray-50 p-2.5 dark:bg-gray-900">
          <p className="text-[10px] tracking-wide text-gray-400 uppercase">Expires</p>
          <div className="mt-0.5">
            <ExpiryBadge share={share} />
          </div>
        </div>
        <div className="rounded-lg bg-gray-50 p-2.5 dark:bg-gray-900">
          <p className="text-[10px] tracking-wide text-gray-400 uppercase">Shared</p>
          <p className="mt-0.5 truncate text-sm font-medium text-gray-900 dark:text-white">
            {formatFileDate(share.createdAt)}
          </p>
        </div>
      </div>

      {/* Actions */}
      <div className="flex flex-col items-stretch gap-2.5 border-t border-gray-100 pt-5 sm:flex-row dark:border-gray-800">
        {isFolder ? (
          <p className="flex items-center justify-center gap-2 rounded-lg bg-gray-50 px-3 py-2.5 text-center text-xs text-gray-500 sm:flex-1 dark:bg-gray-900 dark:text-gray-400">
            <FolderOpen className="h-4 w-4 shrink-0" />
            Folder shares open inside CloudNest
          </p>
        ) : (
          <>
            {isPreviewable ? (
              <Button
                variant="outline"
                onClick={() => void openPreview()}
                isLoading={previewing}
                leftIcon={<Eye className="h-4 w-4" />}
              >
                Preview
              </Button>
            ) : (
              <p className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-gray-50 px-3 py-2.5 text-center text-xs text-gray-500 dark:bg-gray-900 dark:text-gray-400">
                <Eye className="h-4 w-4 shrink-0" />
                Preview is not supported for this file type.
              </p>
            )}
            {!viewOnly && (
              <Button
                variant="primary"
                className="flex-1"
                onClick={onDownload}
                isLoading={downloading}
                leftIcon={<Download className="h-4 w-4" />}
              >
                Download
              </Button>
            )}
            {viewOnly && (
              <p className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-gray-50 px-3 py-2.5 text-center text-xs text-gray-500 dark:bg-gray-900 dark:text-gray-400">
                <ShieldCheck className="h-4 w-4 shrink-0" />
                This link is view-only — downloading is disabled
              </p>
            )}
          </>
        )}
        <Button variant="ghost" onClick={onCopyLink} leftIcon={<Link2 className="h-4 w-4" />}>
          Copy link
        </Button>
      </div>

      {/* In-page preview modal */}
      <Modal
        open={previewUrl !== null}
        onClose={closePreview}
        title={fileName}
        description={
          previewKind === 'unsupported'
            ? 'Preview is not supported for this file type'
            : 'Streamed preview of the shared file'
        }
        size="lg"
      >
        <div className="flex min-h-72 items-center justify-center overflow-hidden rounded-xl bg-gray-50 p-4 dark:bg-gray-950">
          {previewError ? (
            <div className="flex flex-col items-center gap-3 text-center">
              <p className="max-w-sm text-sm text-rose-600 dark:text-rose-400">{previewError}</p>
              {!viewOnly && (
                <Button
                  variant="outline"
                  size="sm"
                  leftIcon={<Download className="h-3.5 w-3.5" />}
                  onClick={onDownload}
                >
                  Download file
                </Button>
              )}
            </div>
          ) : !previewUrl ? (
            <div className="grid h-40 w-full place-items-center text-gray-400">
              <Loader2 className="h-6 w-6 animate-spin" />
            </div>
          ) : previewKind === 'image' ? (
            <img
              src={previewUrl}
              alt={fileName}
              className="max-h-[65vh] w-full rounded-lg object-contain"
            />
          ) : previewKind === 'pdf' ? (
            <iframe
              src={previewUrl}
              title={`${fileName} preview`}
              className="h-[65vh] w-full rounded-lg border border-gray-200 bg-white dark:border-gray-700"
            />
          ) : previewKind === 'video' ? (
            <video
              key={previewUrl}
              src={previewUrl}
              controls
              playsInline
              className="max-h-[65vh] max-w-full rounded-lg bg-black"
            >
              Your browser does not support video preview.
            </video>
          ) : previewKind === 'audio' ? (
            <div className="flex w-full max-w-md flex-col items-center gap-4 py-2">
              <span className="from-brand-500 to-accent-600 grid h-16 w-16 place-items-center rounded-2xl bg-linear-to-br text-white shadow-lg shadow-brand-500/25">
                <Music className="h-8 w-8" />
              </span>
              <audio src={previewUrl} controls className="w-full">
                Your browser does not support audio preview.
              </audio>
            </div>
          ) : previewKind === 'text' ? (
            <pre className="bg-white dark:bg-gray-900 h-[55vh] w-full overflow-auto rounded-lg border border-gray-200 p-4 text-sm whitespace-pre-wrap text-gray-800 dark:border-gray-700 dark:text-gray-100">
              {textState?.text ?? 'Loading…'}
            </pre>
          ) : null}
        </div>
        <p className="mt-4 truncate text-center text-xs text-gray-500 dark:text-gray-400">
          {fileName} · use Download to save the original file
        </p>
      </Modal>
    </div>
  );
}
