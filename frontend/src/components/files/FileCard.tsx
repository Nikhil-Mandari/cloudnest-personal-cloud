import { motion } from 'framer-motion';
import { Check, Download, Star } from 'lucide-react';

import type { FileItem } from '@/types';
import { cn } from '@/utils/cn';
import { formatFileDate } from '@/utils/file';
import { formatBytes, getInitials } from '@/utils/format';
import { FileIcon } from './FileIcon';

export interface FileCardProps {
  file: FileItem;
  selected: boolean;
  ownerName: string;
  onSelect: (id: number) => void;
  onToggleFavorite: (file: FileItem) => void;
  onDownload: (file: FileItem) => void;
  onOpenMenu: (file: FileItem, x: number, y: number) => void;
}

/** Grid tile for a single file — Dropbox-inspired with hover actions. */
export function FileCard({
  file,
  selected,
  ownerName,
  onSelect,
  onToggleFavorite,
  onDownload,
  onOpenMenu,
}: FileCardProps) {
  return (
    <motion.div
      layout
      role="button"
      tabIndex={0}
      aria-pressed={selected}
      aria-label={file.originalFileName}
      onClick={() => onSelect(file.id)}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          onSelect(file.id);
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
        'group relative flex cursor-pointer flex-col rounded-2xl border bg-white p-3 shadow-sm shadow-gray-900/[0.03]',
        'transition-shadow duration-200 hover:shadow-lg hover:shadow-gray-900/[0.06]',
        'focus-visible:ring-brand-500/50 focus-visible:ring-2 focus-visible:ring-offset-1 focus-visible:outline-none',
        selected
          ? 'border-brand-500 ring-brand-500/30 dark:border-brand-500 ring-2'
          : 'border-gray-200 dark:border-gray-800',
        'dark:bg-gray-900',
      )}
    >
      {/* Selection checkbox (revealed on hover / when selected) */}
      <span
        aria-hidden="true"
        className={cn(
          'absolute top-2.5 left-2.5 z-10 grid h-5 w-5 place-items-center rounded-md border transition-all duration-150',
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
          'absolute top-2 right-2 z-10 grid h-7 w-7 place-items-center rounded-lg transition-all duration-150',
          file.isFavorite
            ? 'text-amber-400'
            : 'text-gray-300 opacity-0 group-hover:opacity-100 hover:text-gray-500 dark:text-gray-600 dark:hover:text-gray-300',
        )}
      >
        <Star className={cn('h-4 w-4', file.isFavorite && 'fill-amber-400')} />
      </button>

      {/* Icon + name + meta */}
      <div className="mt-6 flex flex-col items-center gap-2.5">
        <FileIcon file={file} size="lg" showExtension />
        <div className="w-full text-center">
          <p
            title={file.originalFileName}
            className="truncate text-sm font-medium text-gray-900 dark:text-white"
          >
            {file.originalFileName}
          </p>
          <p className="mt-0.5 truncate text-xs text-gray-400 dark:text-gray-500">
            {formatBytes(file.fileSize)} · {formatFileDate(file.createdAt)}
          </p>
        </div>
      </div>

      {/* Owner + quick download */}
      <div className="mt-3 flex items-center gap-1.5 border-t border-gray-100 pt-2.5 dark:border-gray-800">
        <span className="bg-brand-500/10 text-brand-600 dark:text-brand-300 grid h-5 w-5 shrink-0 place-items-center rounded-full text-[9px] font-bold">
          {getInitials(ownerName)}
        </span>
        <span className="truncate text-xs text-gray-500 dark:text-gray-400">{ownerName}</span>
        <button
          type="button"
          onClick={(event) => {
            event.stopPropagation();
            onDownload(file);
          }}
          aria-label={`Download ${file.originalFileName}`}
          className="ml-auto grid h-7 w-7 shrink-0 place-items-center rounded-lg text-gray-400 opacity-0 transition-all group-hover:opacity-100 hover:bg-gray-100 hover:text-gray-700 dark:text-gray-500 dark:hover:bg-gray-800 dark:hover:text-gray-200"
        >
          <Download className="h-4 w-4" />
        </button>
      </div>
    </motion.div>
  );
}
