import { AnimatePresence, motion } from 'framer-motion';
import {
  CalendarDays,
  Download,
  Eye,
  FileClock,
  HardDrive,
  Pencil,
  Share2,
  Star,
  Trash2,
  User,
  X,
} from 'lucide-react';

import { Button } from '@/components/ui/Button';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import type { FileItem } from '@/types';
import { cn } from '@/utils/cn';
import { formatFileDate, getFileTypeCategory } from '@/utils/file';
import { formatBytes } from '@/utils/format';
import { FileThumbnail } from './FileThumbnail';

export interface DetailsPanelProps {
  file: FileItem | null;
  open: boolean;
  onClose: () => void;
  ownerName: string;
  onDownload: (file: FileItem) => void;
  onPreview: (file: FileItem) => void;
  onShare: (file: FileItem) => void;
  onRename: (file: FileItem) => void;
  onDelete: (file: FileItem) => void;
  onToggleFavorite: (file: FileItem) => void;
}

interface MetaRowProps {
  icon: React.ReactNode;
  label: string;
  value: string;
}

function MetaRow({ icon, label, value }: MetaRowProps) {
  return (
    <div className="flex items-center gap-2.5 py-2">
      <span className="text-gray-400 dark:text-gray-500">{icon}</span>
      <div className="min-w-0">
        <p className="text-[11px] font-medium tracking-wide text-gray-400 uppercase dark:text-gray-500">
          {label}
        </p>
        <p className="truncate text-sm text-gray-800 dark:text-gray-200" title={value}>
          {value}
        </p>
      </div>
    </div>
  );
}

const actionButtonClasses =
  'inline-flex items-center justify-center gap-1.5 rounded-lg border border-gray-200 bg-white px-3 py-2 text-xs font-medium text-gray-700 shadow-sm transition-colors hover:bg-gray-50 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-200 dark:hover:bg-gray-800';

/**
 * Right-hand details panel (Google Drive "details" pane style). Shows the
 * real thumbnail, metadata and quick actions for the currently selected file.
 * Renders inline on desktop and as a slide-over drawer on small screens.
 */
export function DetailsPanel({
  file,
  open,
  onClose,
  ownerName,
  onDownload,
  onPreview,
  onShare,
  onRename,
  onDelete,
  onToggleFavorite,
}: DetailsPanelProps) {
  const isDesktop = useMediaQuery('(min-width: 1024px)');

  return (
    <AnimatePresence>
      {open && file && (
        <motion.aside
          key="details-panel"
          initial={{ opacity: 0, x: isDesktop ? 24 : 64 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: isDesktop ? 24 : 64 }}
          transition={{ duration: 0.22, ease: 'easeOut' }}
          className={cn(
            'flex w-80 shrink-0 flex-col overflow-hidden',
            isDesktop
              ? 'rounded-2xl border border-gray-200/80 bg-white shadow-sm shadow-gray-900/[0.03] dark:border-gray-800 dark:bg-gray-900'
              : 'fixed inset-y-0 right-0 z-50 w-80 border-l border-gray-200 bg-white shadow-2xl dark:border-gray-800 dark:bg-gray-900',
          )}
          aria-label="File details"
        >
          <div className="flex items-center justify-between border-b border-gray-100 px-4 py-3 dark:border-gray-800">
            <h2 className="text-sm font-semibold text-gray-900 dark:text-white">Details</h2>
            <button
              type="button"
              onClick={onClose}
              aria-label="Close details"
              className="grid h-8 w-8 place-items-center rounded-lg text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-700 dark:hover:bg-gray-800 dark:hover:text-gray-200"
            >
              <X className="h-4 w-4" />
            </button>
          </div>

          <div className="flex-1 overflow-y-auto px-4 py-5">
            {/* Thumbnail */}
            <div className="flex flex-col items-center gap-3">
              <FileThumbnail file={file} size="lg" className="h-28 w-28 rounded-2xl" />
              <p
                className="max-w-full truncate text-sm font-semibold text-gray-900 dark:text-white"
                title={file.originalFileName}
              >
                {file.originalFileName}
              </p>
              <span className="rounded-full bg-gray-100 px-2.5 py-1 text-[11px] font-semibold tracking-wide text-gray-600 uppercase dark:bg-gray-800 dark:text-gray-300">
                {getFileTypeCategory(file)}
              </span>
            </div>

            {/* Metadata */}
            <div className="mt-5 divide-y divide-gray-100 border-t border-gray-100 dark:divide-gray-800 dark:border-gray-800">
              <MetaRow
                icon={<HardDrive className="h-4 w-4" />}
                label="Size"
                value={formatBytes(file.fileSize)}
              />
              <MetaRow
                icon={<CalendarDays className="h-4 w-4" />}
                label="Uploaded"
                value={formatFileDate(file.createdAt)}
              />
              <MetaRow
                icon={<FileClock className="h-4 w-4" />}
                label="Modified"
                value={formatFileDate(file.updatedAt)}
              />
              <MetaRow icon={<User className="h-4 w-4" />} label="Owner" value={ownerName} />
              <MetaRow
                icon={<Star className="h-4 w-4" />}
                label="Favorite"
                value={file.isFavorite ? 'Yes' : 'No'}
              />
            </div>

            {/* Actions */}
            <div className="mt-5 space-y-2">
              <div className="grid grid-cols-2 gap-2">
                <button type="button" className={actionButtonClasses} onClick={() => onDownload(file)}>
                  <Download className="h-3.5 w-3.5" /> Download
                </button>
                <button type="button" className={actionButtonClasses} onClick={() => onPreview(file)}>
                  <Eye className="h-3.5 w-3.5" /> Preview
                </button>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <button type="button" className={actionButtonClasses} onClick={() => onShare(file)}>
                  <Share2 className="h-3.5 w-3.5" /> Share
                </button>
                <button
                  type="button"
                  className={actionButtonClasses}
                  onClick={() => onToggleFavorite(file)}
                >
                  <Star className="h-3.5 w-3.5" />
                  {file.isFavorite ? 'Unfavorite' : 'Favorite'}
                </button>
              </div>
              <Button
                variant="outline"
                size="sm"
                fullWidth
                leftIcon={<Pencil className="h-3.5 w-3.5" />}
                onClick={() => onRename(file)}
              >
                Rename
              </Button>
              <Button
                variant="danger"
                size="sm"
                fullWidth
                leftIcon={<Trash2 className="h-3.5 w-3.5" />}
                onClick={() => onDelete(file)}
              >
                Move to trash
              </Button>
            </div>
          </div>
        </motion.aside>
      )}
    </AnimatePresence>
  );
}
