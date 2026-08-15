import { useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { CloudUpload, Download, FileClock, History, RotateCcw, Trash2 } from 'lucide-react';

import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import {
  useDeleteVersionMutation,
  useDownloadVersionMutation,
  useFileVersionsQuery,
  useRestoreVersionMutation,
  useUploadVersionMutation,
} from '@/hooks/useFileVersions';
import type { FileItem } from '@/types';
import { cn } from '@/utils/cn';
import { formatBytes, formatRelativeTime } from '@/utils/format';
import { FileIcon } from './FileIcon';

export interface VersionHistoryDialogProps {
  file: FileItem | null;
  open: boolean;
  onClose: () => void;
}

const iconButtonClasses =
  'grid h-8 w-8 place-items-center rounded-lg text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-700 dark:hover:bg-gray-800 dark:hover:text-gray-200';

/**
 * Version history dialog — every upload archives the previous content as a
 * version. Users can upload a new version, restore an older one, delete a
 * version, or download any snapshot.
 */
export function VersionHistoryDialog({ file, open, onClose }: VersionHistoryDialogProps) {
  const fileId = open && file ? file.id : null;
  const { data: versions = [], isLoading } = useFileVersionsQuery(fileId, open);
  const uploadMutation = useUploadVersionMutation();
  const restoreMutation = useRestoreVersionMutation();
  const deleteMutation = useDeleteVersionMutation();
  const downloadMutation = useDownloadVersionMutation();

  const fileInputRef = useRef<HTMLInputElement>(null);
  const [restoreTarget, setRestoreTarget] = useState<number | null>(null);

  const handleUpload = (selected: FileList | null) => {
    if (!file || !selected || selected.length === 0) {
      return;
    }
    const [uploaded] = Array.from(selected);
    uploadMutation.mutate({ id: file.id, file: uploaded });
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Version history"
      description={file ? `Previous versions of "${file.originalFileName}"` : undefined}
      size="md"
    >
      <div className="space-y-4">
        {/* Current version summary + upload new version */}
        <div className="flex items-center justify-between gap-3 rounded-xl border border-gray-100 bg-gray-50/60 p-3 dark:border-gray-800 dark:bg-gray-900/60">
          <div className="flex min-w-0 items-center gap-3">
            <span className="bg-brand-500/10 text-brand-600 dark:text-brand-400 grid h-9 w-9 shrink-0 place-items-center rounded-lg">
              <FileClock className="h-5 w-5" />
            </span>
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-gray-900 dark:text-white">
                Current version
              </p>
              <p className="text-xs text-gray-400 dark:text-gray-500">
                {file ? `${formatBytes(file.fileSize)} · updated ${formatRelativeTime(file.createdAt)}` : '—'}
              </p>
            </div>
          </div>
          <input
            ref={fileInputRef}
            type="file"
            className="hidden"
            onChange={(event) => handleUpload(event.target.files)}
          />
          <Button
            variant="primary"
            size="sm"
            onClick={() => fileInputRef.current?.click()}
            isLoading={uploadMutation.isPending}
            leftIcon={<CloudUpload className="h-4 w-4" />}
          >
            Upload version
          </Button>
        </div>

        {/* Version list */}
        {isLoading ? (
          <div className="space-y-2">
            {[0, 1].map((index) => (
              <div
                key={index}
                className="h-14 animate-pulse rounded-xl bg-gray-100 dark:bg-gray-800"
              />
            ))}
          </div>
        ) : versions.length === 0 ? (
          <div className="flex flex-col items-center gap-2 rounded-xl border border-dashed border-gray-200 py-10 text-center dark:border-gray-700">
            <History className="h-8 w-8 text-gray-300 dark:text-gray-600" />
            <p className="text-sm text-gray-500 dark:text-gray-400">
              No archived versions yet — uploads create a snapshot of the previous content.
            </p>
          </div>
        ) : (
          <motion.ul layout className="max-h-80 space-y-2 overflow-y-auto pr-1">
            <AnimatePresence initial={false}>
              {versions.map((version) => (
                <motion.li
                  key={version.id}
                  layout
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, x: 16 }}
                  className="flex items-center gap-3 rounded-xl border border-gray-100 bg-white p-3 dark:border-gray-800 dark:bg-gray-900"
                >
                  <FileIcon
                    file={{ originalFileName: file?.originalFileName ?? 'file', fileType: version.contentType ?? '' }}
                    size="sm"
                  />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <p className="truncate text-sm font-medium text-gray-900 dark:text-white">
                        Version {version.versionNumber}
                      </p>
                      {version.id === restoreTarget && (
                        <span className="bg-amber-500/10 text-amber-600 dark:text-amber-400 rounded-full px-1.5 py-0.5 text-[9px] font-bold tracking-wide uppercase">
                          Restoring…
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-gray-400 tabular-nums dark:text-gray-500">
                      {formatBytes(version.fileSize)} · {formatRelativeTime(version.createdAt)}
                      {version.uploadedBy ? ` · by user #${version.uploadedBy}` : ''}
                    </p>
                  </div>

                  <div className="flex shrink-0 items-center gap-0.5">
                    <button
                      type="button"
                      aria-label={`Download version ${version.versionNumber}`}
                      title="Download"
                      className={iconButtonClasses}
                      onClick={() =>
                        downloadMutation.mutate({
                          id: file!.id,
                          versionId: version.id,
                          fileName: `${file!.originalFileName}.v${version.versionNumber}`,
                        })
                      }
                    >
                      <Download className="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      aria-label={`Restore version ${version.versionNumber}`}
                      title="Restore this version"
                      className={cn(
                        iconButtonClasses,
                        'hover:text-emerald-600 dark:hover:text-emerald-400',
                      )}
                      disabled={restoreMutation.isPending}
                      onClick={() => {
                        setRestoreTarget(version.id);
                        restoreMutation.mutate(
                          { id: file!.id, versionId: version.id },
                          { onSettled: () => setRestoreTarget(null) },
                        );
                      }}
                    >
                      <RotateCcw className="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      aria-label={`Delete version ${version.versionNumber}`}
                      title="Delete version"
                      className={cn(
                        iconButtonClasses,
                        'hover:bg-rose-500/10 hover:text-rose-500',
                      )}
                      onClick={() => deleteMutation.mutate({ id: file!.id, versionId: version.id })}
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </motion.li>
              ))}
            </AnimatePresence>
          </motion.ul>
        )}

        <p className="text-xs text-gray-400 dark:text-gray-500">
          Restoring a version archives the current content first — nothing is ever lost.
        </p>
      </div>
    </Modal>
  );
}
