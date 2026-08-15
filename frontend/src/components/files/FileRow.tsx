import type { MouseEvent as ReactMouseEvent } from 'react';
import { Check, EllipsisVertical, Star } from 'lucide-react';

import { Highlight } from '@/components/common/Highlight';
import type { FileItem } from '@/types';
import { cn } from '@/utils/cn';
import {
  formatFileDate,
  getFileExtension,
  getFileTypeCategory,
  isRecentlyUploaded,
} from '@/utils/file';
import { formatBytes } from '@/utils/format';
import { ScanStatusBadge } from './ScanStatusBadge';
import { FileThumbnail } from './FileThumbnail';

export interface FileRowProps {
  file: FileItem;
  selected: boolean;
  ownerName: string;
  /** Active search term — matched text is highlighted in the name. */
  searchQuery?: string;
  /** Freshly uploaded — the row pulses so the user spots it. */
  highlighted?: boolean;
  /** Dense row used by compact view. */
  compact?: boolean;
  /** Selection callback — the event carries Shift/Ctrl/Cmd modifiers. */
  onSelect: (file: FileItem, event?: ReactMouseEvent) => void;
  onToggleFavorite: (file: FileItem) => void;
  onOpenMenu: (file: FileItem, x: number, y: number) => void;
  /** Opens the inline preview (double-click). */
  onPreview: (file: FileItem) => void;
}

function formatFileType(file: FileItem): string {
  const ext = getFileExtension(file.originalFileName);
  if (ext) {
    return ext.toUpperCase();
  }
  return getFileTypeCategory(file).toUpperCase();
}

/** Table row for list view with sortable-column-agnostic cells. */
export function FileRow({
  file,
  selected,
  ownerName,
  searchQuery,
  highlighted = false,
  compact = false,
  onSelect,
  onToggleFavorite,
  onOpenMenu,
  onPreview,
}: FileRowProps) {
  const isNew = isRecentlyUploaded(file.createdAt);

  const cellPadding = compact ? 'py-1.5' : 'py-2.5';

  return (
    <tr
      data-file-id={file.id}
      onClick={(event) => onSelect(file, event)}
      onDoubleClick={() => onPreview(file)}
      onContextMenu={(event) => {
        event.preventDefault();
        onOpenMenu(file, event.clientX, event.clientY);
      }}
      className={cn(
        'group cursor-pointer border-b border-gray-100 transition-colors last:border-0 dark:border-gray-800/70',
        selected
          ? 'bg-brand-500/[0.07] dark:bg-brand-500/[0.09]'
          : 'hover:bg-gray-50 dark:hover:bg-gray-800/40',
        highlighted && 'file-highlight',
      )}
    >
      {/* Selection checkbox */}
      <td className={cn('w-12 pr-0 pl-4', cellPadding, compact && 'hidden')}>
        <span
          aria-hidden="true"
          className={cn(
            'grid h-5 w-5 place-items-center rounded-md border transition-all duration-150',
            selected
              ? 'border-brand-500 bg-brand-500 text-white'
              : 'border-gray-300 bg-white opacity-0 group-hover:opacity-100 dark:border-gray-600 dark:bg-gray-900',
          )}
        >
          {selected && <Check className="h-3.5 w-3.5" />}
        </span>
      </td>

      {/* Name */}
      <td className={cn('pr-3', cellPadding)}>
        <div className="flex min-w-0 items-center gap-3">
          <FileThumbnail file={file} size="sm" />
          <span
            title={file.originalFileName}
            className="min-w-0 truncate text-sm font-medium text-gray-900 dark:text-white"
          >
            <Highlight text={file.originalFileName} query={searchQuery} />
          </span>
          {isNew && !compact && (
            <span className="bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 shrink-0 rounded-full px-1.5 py-0.5 text-[9px] font-bold tracking-wide uppercase">
              New
            </span>
          )}
          {file.scanStatus && file.scanStatus !== 'CLEAN' && (
            <ScanStatusBadge status={file.scanStatus} compact />
          )}
          {file.isFavorite && (
            <Star className="h-3.5 w-3.5 shrink-0 fill-amber-400 text-amber-400" />
          )}
        </div>
      </td>

      {/* Type */}
      <td
        className={cn(
          'hidden pr-3 text-sm text-gray-500 md:table-cell dark:text-gray-400',
          cellPadding,
        )}
      >
        {formatFileType(file)}
      </td>

      {/* Size */}
      <td
        className={cn(
          'pr-3 text-right text-sm text-gray-500 tabular-nums sm:text-left dark:text-gray-400',
          cellPadding,
        )}
      >
        {formatBytes(file.fileSize)}
      </td>

      {/* Modified */}
      <td
        className={cn(
          'hidden pr-3 text-sm text-gray-500 lg:table-cell dark:text-gray-400',
          cellPadding,
        )}
      >
        {formatFileDate(file.createdAt)}
      </td>

      {/* Owner */}
      <td
        className={cn(
          'hidden pr-3 text-sm text-gray-500 xl:table-cell dark:text-gray-400',
          cellPadding,
        )}
      >
        {ownerName}
      </td>

      {/* Actions */}
      <td className={cn('w-20 pr-4', cellPadding)}>
        <div className="flex items-center justify-end gap-0.5">
          <button
            type="button"
            onClick={(event) => {
              event.stopPropagation();
              onToggleFavorite(file);
            }}
            aria-label={file.isFavorite ? 'Remove from favorites' : 'Add to favorites'}
            aria-pressed={file.isFavorite}
            className={cn(
              'grid h-7 w-7 place-items-center rounded-lg transition-colors',
              file.isFavorite
                ? 'text-amber-400'
                : 'text-gray-300 hover:bg-gray-100 hover:text-gray-500 dark:text-gray-600 dark:hover:bg-gray-800 dark:hover:text-gray-300',
            )}
          >
            <Star className={cn('h-4 w-4', file.isFavorite && 'fill-amber-400')} />
          </button>
          <button
            type="button"
            onClick={(event) => {
              event.stopPropagation();
              onOpenMenu(file, event.clientX, event.clientY);
            }}
            aria-label={`More actions for ${file.originalFileName}`}
            className="grid h-7 w-7 place-items-center rounded-lg text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-700 md:opacity-0 md:group-hover:opacity-100 dark:text-gray-500 dark:hover:bg-gray-800 dark:hover:text-gray-200"
          >
            <EllipsisVertical className="h-4 w-4" />
          </button>
        </div>
      </td>
    </tr>
  );
}
