import { useState } from 'react';
import {
  Download,
  Eye,
  FileText,
  FolderOpen,
  Link2,
  Loader2,
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
import { formatFileDate, getFileTypeCategory, isShareableImageName } from '@/utils/file';

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
  const isImage = !isFolder && isShareableImageName(fileName);
  const viewOnly = share.permission === 'VIEW';
  const CategoryIcon = isFolder ? FolderOpen : CATEGORY_ICONS[getFileTypeCategory({ fileType: '', originalFileName: fileName })];
  const isPreviewable = isImage;

  // ── In-card image preview (streamed through the public download endpoint) ──
  const [previewing, setPreviewing] = useState(false);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  const closePreview = () => {
    setPreviewUrl((current) => {
      if (current) {
        URL.revokeObjectURL(current);
      }
      return null;
    });
  };

  const openPreview = async () => {
    setPreviewing(true);
    try {
      // The preview endpoint never increments the owner's download counter.
      const { data } = await shareService.previewPublicShare(share.shareToken, password ?? undefined);
      setPreviewUrl(URL.createObjectURL(data));
    } catch {
      const message = 'Could not load the preview. Please try downloading instead.';
      toast.error(message);
      onPreviewError?.(message);
    } finally {
      setPreviewing(false);
    }
  };

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
        ) : viewOnly ? (
          <>
            {isPreviewable && (
              <Button
                variant="outline"
                className="flex-1"
                onClick={() => void openPreview()}
                isLoading={previewing}
                leftIcon={<Eye className="h-4 w-4" />}
              >
                Preview
              </Button>
            )}
            {!isPreviewable && (
              <p className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-gray-50 px-3 py-2.5 text-center text-xs text-gray-500 dark:bg-gray-900 dark:text-gray-400">
                <Eye className="h-4 w-4 shrink-0" />
                This link is view-only — downloading is disabled
              </p>
            )}
          </>
        ) : (
          <>
            {isPreviewable && (
              <Button
                variant="outline"
                onClick={() => void openPreview()}
                isLoading={previewing}
                leftIcon={<Eye className="h-4 w-4" />}
              >
                Preview
              </Button>
            )}
            <Button
              variant="primary"
              className="flex-1"
              onClick={onDownload}
              isLoading={downloading}
              leftIcon={<Download className="h-4 w-4" />}
            >
              Download
            </Button>
          </>
        )}
        <Button variant="ghost" onClick={onCopyLink} leftIcon={<Link2 className="h-4 w-4" />}>
          Copy link
        </Button>
      </div>

      {/* Image preview modal */}
      <Modal
        open={previewUrl !== null}
        onClose={closePreview}
        title={fileName}
        description="Streamed preview of the shared file"
        size="lg"
      >
        <div className="flex items-center justify-center rounded-xl bg-gray-50 p-4 dark:bg-gray-950">
          {previewUrl ? (
            <img
              src={previewUrl}
              alt={fileName}
              className="max-h-[65vh] w-full rounded-lg object-contain"
            />
          ) : (
            <div className="grid h-40 w-full place-items-center text-gray-400">
              <Loader2 className="h-6 w-6 animate-spin" />
            </div>
          )}
        </div>
        <p className="mt-4 truncate text-center text-xs text-gray-500 dark:text-gray-400">
          {fileName} · use Download to save the original file
        </p>
      </Modal>
    </div>
  );
}
