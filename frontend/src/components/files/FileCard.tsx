import type { MouseEvent as ReactMouseEvent } from 'react';
import { motion } from 'framer-motion';
import { Check, Download, Play, Star } from 'lucide-react';

import { Highlight } from '@/components/common/Highlight';
import type { FileItem } from '@/types';
import { cn } from '@/utils/cn';
import { formatFileDate, getFileTypeCategory, isPdfFile, isRecentlyUploaded } from '@/utils/file';
import { formatBytes } from '@/utils/format';
import { ScanStatusBadge } from './ScanStatusBadge';
import { FileThumbnail } from './FileThumbnail';

export interface FileCardProps {
  file: FileItem;
  selected: boolean;
  ownerName: string;
  /** Active search term — matched text is highlighted in the name. */
  searchQuery?: string;
  /** Freshly uploaded — pulses with a brand ring so the user spots it. */
  highlighted?: boolean;
  /** Selection callback — the event carries Shift/Ctrl/Cmd modifiers. */
  onSelect: (file: FileItem, event?: ReactMouseEvent) => void;
  onToggleFavorite: (file: FileItem) => void;
  onDownload: (file: FileItem) => void;
  onOpenMenu: (file: FileItem, x: number, y: number) => void;
  /** Opens the inline preview (double-click). */
  onPreview: (file: FileItem) => void;
}

const isVideo = (file: FileItem): boolean => getFileTypeCategory(file) === 'video';

/** Dropbox-style grid tile: the real thumbnail dominates the card. */
export function FileCard({
  file,
  selected,
  ownerName,
  searchQuery,
  highlighted = false,
  onSelect,
  onToggleFavorite,
  onDownload,
  onOpenMenu,
  onPreview,
}: FileCardProps) {
  const isNew = isRecentlyUploaded(file.createdAt);
  const video = isVideo(file);
  const pdf = isPdfFile(file);

  return (
    <motion.div
      layout
      role="button"
      tabIndex={0}
      data-file-id={file.id}
      aria-pressed={selected}
      aria-label={file.originalFileName}
      onClick={(event) => onSelect(file, event)}
      onDoubleClick={() => onPreview(file)}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          onSelect(file);
        }
      }}
      onContextMenu={(event) => {
        event.preventDefault();
        onOpenMenu(file, event.clientX, event.clientY);
      }}
      initial={{ opacity: 0, scale: 0.92 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0, scale: 0.92 }}
      transition={{ duration: 0.18, ease: 'easeOut' }}
      whileHover={{ y: -4 }}
      className={cn(
        'group relative flex cursor-pointer flex-col rounded-xl border bg-white p-2 shadow-sm shadow-gray-900/[0.04]',
        'transition-[box-shadow,border-color] duration-200 hover:shadow-lg hover:shadow-gray-900/[0.08]',
        'focus-visible:ring-brand-500/50 focus-visible:ring-2 focus-visible:ring-offset-1 focus-visible:outline-none',
        selected
          ? 'border-brand-500 ring-brand-500/30 dark:border-brand-500 ring-2'
          : 'border-gray-200 dark:border-gray-800',
        highlighted && 'file-highlight border-brand-400 ring-brand-400/40 ring-2 dark:border-brand-500',
        'dark:bg-gray-900',
      )}
    >
      {/* ── Media block: thumbnail fills ~78% of the card ─────────────────── */}
      <div className="relative aspect-[4/3] w-full overflow-hidden rounded-lg bg-gray-100 dark:bg-gray-800">
        <FileThumbnail
          file={file}
          size="lg"
          className="h-full w-full"
          imgClassName="transition-transform duration-300 ease-out group-hover:scale-[1.05]"
        />

        {/* Soft inner border so thumbnails read as tiles, not cut-off photos */}
        <span className="pointer-events-none absolute inset-0 rounded-[inherit] ring-1 ring-gray-900/5 ring-inset dark:ring-white/10" />

        {/* Selection checkbox (revealed on hover / when selected) */}
        <span
          aria-hidden="true"
          className={cn(
            'absolute top-2 left-2 z-10 grid h-5 w-5 place-items-center rounded-md border transition-all duration-150',
            selected
              ? 'border-brand-500 bg-brand-500 text-white'
              : 'border-gray-300 bg-white opacity-0 group-hover:opacity-100 dark:border-gray-600 dark:bg-gray-900',
          )}
        >
          {selected && <Check className="h-3.5 w-3.5" />}
        </span>

        {/* Favorite star */}
        <button
          type="button"
          onClick={(event) => {
            event.stopPropagation();
            onToggleFavorite(file);
          }}
          aria-label={file.isFavorite ? 'Remove from favorites' : 'Add to favorites'}
          aria-pressed={file.isFavorite}
          className={cn(
            'absolute top-1.5 right-1.5 z-10 grid h-7 w-7 place-items-center rounded-lg transition-all duration-150',
            file.isFavorite
              ? 'bg-black/10 text-amber-400 backdrop-blur-sm dark:bg-black/30'
              : 'text-gray-300 opacity-0 group-hover:opacity-100 hover:text-gray-500 dark:text-gray-600 dark:hover:text-gray-300',
          )}
        >
          <Star className={cn('h-4 w-4', file.isFavorite && 'fill-amber-400')} />
        </button>

        {/* Video play overlay */}
        {video && (
          <span className="pointer-events-none absolute inset-0 z-[5] grid place-items-center">
            <span className="grid h-11 w-11 place-items-center rounded-full bg-black/45 text-white shadow-lg backdrop-blur-[2px] transition-transform duration-200 group-hover:scale-110">
              <Play className="ml-0.5 h-5 w-5 fill-current" />
            </span>
          </span>
        )}

        {/* PDF badge */}
        {pdf && (
          <span className="absolute bottom-2 left-2 z-[5] rounded-md bg-rose-600/90 px-1.5 py-0.5 text-[9px] font-bold tracking-wide text-white uppercase shadow-sm">
            PDF
          </span>
        )}

        {/* Quick download */}
        <button
          type="button"
          onClick={(event) => {
            event.stopPropagation();
            onDownload(file);
          }}
          aria-label={`Download ${file.originalFileName}`}
          className="absolute right-1.5 bottom-1.5 z-10 grid h-7 w-7 place-items-center rounded-lg bg-white/90 text-gray-600 opacity-0 shadow-sm backdrop-blur transition-all duration-150 group-hover:opacity-100 hover:bg-white hover:text-gray-900 dark:bg-gray-900/90 dark:text-gray-300 dark:hover:bg-gray-900 dark:hover:text-white"
        >
          <Download className="h-4 w-4" />
        </button>

        {/* Owner chip (revealed on hover, sits next to the download button) */}
        <span className="absolute right-10 bottom-1.5 z-[5] hidden items-center gap-1 rounded-md bg-white/85 px-1.5 py-0.5 text-[10px] font-medium text-gray-700 opacity-0 shadow-sm backdrop-blur transition-opacity duration-150 group-hover:opacity-100 sm:flex dark:bg-gray-900/85 dark:text-gray-200">
          {ownerName}
        </span>
      </div>

      {/* ── Filename + meta below the image ───────────────────────────────── */}
      <div className="mt-2 flex flex-1 flex-col justify-center px-0.5 pb-0.5">
        <div className="flex min-w-0 items-center gap-1">
          <p
            title={file.originalFileName}
            className="min-w-0 truncate text-[13px] leading-snug font-medium text-gray-900 dark:text-white"
          >
            <Highlight text={file.originalFileName} query={searchQuery} />
          </p>
          {isNew && (
            <span className="bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 shrink-0 rounded-full px-1.5 py-0.5 text-[9px] font-bold tracking-wide uppercase">
              New
            </span>
          )}
        </div>
        <div className="mt-0.5 flex items-center gap-1.5">
          {file.scanStatus && file.scanStatus !== 'CLEAN' && (
            <ScanStatusBadge status={file.scanStatus} compact />
          )}
          <p className="min-w-0 truncate text-[11px] text-gray-400 dark:text-gray-500">
            {formatBytes(file.fileSize)} · {formatFileDate(file.createdAt)}
          </p>
        </div>
      </div>
    </motion.div>
  );
}
