import { Check, EllipsisVertical, Star } from 'lucide-react';

import type { FileItem } from '@/types';
import { cn } from '@/utils/cn';
import { formatFileDate, getFileExtension, getFileTypeCategory } from '@/utils/file';
import { formatBytes } from '@/utils/format';
import { FileIcon } from './FileIcon';

export interface FileRowProps {
  file: FileItem;
  selected: boolean;
  ownerName: string;
  onSelect: (id: number) => void;
  onToggleFavorite: (file: FileItem) => void;
  onOpenMenu: (file: FileItem, x: number, y: number) => void;
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
  onSelect,
  onToggleFavorite,
  onOpenMenu,
}: FileRowProps) {
  return (
    <tr
      onClick={() => onSelect(file.id)}
      onContextMenu={(event) => {
        event.preventDefault();
        onOpenMenu(file, event.clientX, event.clientY);
      }}
      className={cn(
        'group cursor-pointer border-b border-gray-100 transition-colors last:border-0 dark:border-gray-800/70',
        selected
          ? 'bg-brand-500/[0.07] dark:bg-brand-500/[0.09]'
          : 'hover:bg-gray-50 dark:hover:bg-gray-800/40',
      )}
    >
      {/* Selection checkbox */}
      <td className="w-12 py-2.5 pr-0 pl-4">
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
      <td className="py-2.5 pr-3">
        <div className="flex min-w-0 items-center gap-3">
          <FileIcon file={file} size="sm" />
          <span
            title={file.originalFileName}
            className="min-w-0 truncate text-sm font-medium text-gray-900 dark:text-white"
          >
            {file.originalFileName}
          </span>
          {file.isFavorite && (
            <Star className="h-3.5 w-3.5 shrink-0 fill-amber-400 text-amber-400" />
          )}
        </div>
      </td>

      {/* Type */}
      <td className="hidden py-2.5 pr-3 text-sm text-gray-500 md:table-cell dark:text-gray-400">
        {formatFileType(file)}
      </td>

      {/* Size */}
      <td className="py-2.5 pr-3 text-right text-sm text-gray-500 tabular-nums sm:text-left dark:text-gray-400">
        {formatBytes(file.fileSize)}
      </td>

      {/* Modified */}
      <td className="hidden py-2.5 pr-3 text-sm text-gray-500 lg:table-cell dark:text-gray-400">
        {formatFileDate(file.createdAt)}
      </td>

      {/* Owner */}
      <td className="hidden py-2.5 pr-3 text-sm text-gray-500 xl:table-cell dark:text-gray-400">
        {ownerName}
      </td>

      {/* Actions */}
      <td className="w-20 py-2.5 pr-4">
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
