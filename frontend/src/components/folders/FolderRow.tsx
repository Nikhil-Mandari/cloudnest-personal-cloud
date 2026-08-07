import { Check, EllipsisVertical, FolderOpen } from 'lucide-react';

import type { Folder } from '@/types';
import { cn } from '@/utils/cn';
import { formatFileDate } from '@/utils/file';

export interface FolderRowProps {
  folder: Folder;
  selected: boolean;
  ownerName: string;
  /** Opens the folder (navigates into it). Single click on the row. */
  onOpen: (folder: Folder) => void;
  /** Toggles selection (checkbox). */
  onSelect: (id: string) => void;
  /** When false the selection checkbox is hidden (e.g. Files explorer). */
  selectable?: boolean;
  /** Context-menu trigger; when omitted the "More actions" button is hidden. */
  onOpenMenu?: (folder: Folder, x: number, y: number) => void;
}

/** Table row for list view with sortable-column-agnostic cells. */
export function FolderRow({
  folder,
  selected,
  ownerName,
  onOpen,
  onSelect,
  selectable = true,
  onOpenMenu,
}: FolderRowProps) {
  const handleOpenMenu = onOpenMenu
    ? (event: React.MouseEvent) => {
        event.preventDefault();
        onOpenMenu(folder, event.clientX, event.clientY);
      }
    : undefined;

  return (
    <tr
      onClick={() => onOpen(folder)}
      onContextMenu={handleOpenMenu}
      className={cn(
        'group cursor-pointer border-b border-gray-100 transition-colors last:border-0 dark:border-gray-800/70',
        selected
          ? 'bg-brand-500/[0.07] dark:bg-brand-500/[0.09]'
          : 'hover:bg-gray-50 dark:hover:bg-gray-800/40',
      )}
    >
      {/* Selection checkbox */}
      <td className="w-12 py-2.5 pr-0 pl-4">
        {selectable ? (
          <button
            type="button"
            aria-label={selected ? `Deselect ${folder.name}` : `Select ${folder.name}`}
            onClick={(event) => {
              event.stopPropagation();
              onSelect(folder.id);
            }}
            className={cn(
              'grid h-5 w-5 cursor-pointer place-items-center rounded-md border transition-all duration-150',
              selected
                ? 'border-brand-500 bg-brand-500 text-white'
                : 'border-gray-300 bg-white opacity-0 group-hover:opacity-100 dark:border-gray-600 dark:bg-gray-900',
            )}
          >
            {selected && <Check className="h-3.5 w-3.5" />}
          </button>
        ) : null}
      </td>

      {/* Name */}
      <td className="py-2.5 pr-3">
        <div className="flex min-w-0 items-center gap-3">
          <div className="bg-brand-500/10 text-brand-600 dark:text-brand-400 grid h-8 w-8 shrink-0 place-items-center rounded-lg">
            <FolderOpen className="h-4 w-4" />
          </div>
          <span
            title={folder.name}
            className="min-w-0 truncate text-sm font-medium text-gray-900 dark:text-white"
          >
            {folder.name}
          </span>
        </div>
      </td>

      {/* Type */}
      <td className="hidden py-2.5 pr-3 text-sm text-gray-500 md:table-cell dark:text-gray-400">
        Folder
      </td>

      {/* Modified */}
      <td className="hidden py-2.5 pr-3 text-sm text-gray-500 lg:table-cell dark:text-gray-400">
        {formatFileDate(folder.createdAt)}
      </td>

      {/* Owner */}
      <td className="hidden py-2.5 pr-3 text-sm text-gray-500 xl:table-cell dark:text-gray-400">
        {ownerName}
      </td>

      {/* Actions */}
      <td className="w-20 py-2.5 pr-4">
        <div className="flex items-center justify-end gap-0.5">
          {handleOpenMenu && (
            <button
              type="button"
              onClick={(event) => {
                event.stopPropagation();
                handleOpenMenu(event);
              }}
              aria-label={`More actions for ${folder.name}`}
              className="grid h-7 w-7 place-items-center rounded-lg text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-700 md:opacity-0 md:group-hover:opacity-100 dark:text-gray-500 dark:hover:bg-gray-800 dark:hover:text-gray-200"
            >
              <EllipsisVertical className="h-4 w-4" />
            </button>
          )}
        </div>
      </td>
    </tr>
  );
}
