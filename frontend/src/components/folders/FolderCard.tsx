import { motion } from 'framer-motion';
import { Check, EllipsisVertical, FolderOpen } from 'lucide-react';

import type { Folder } from '@/types';
import { cn } from '@/utils/cn';
import { formatFileDate } from '@/utils/file';
import { getInitials } from '@/utils/format';

export interface FolderCardProps {
  folder: Folder;
  selected: boolean;
  ownerName: string;
  /** Opens the folder (navigates into it). Single click on the card. */
  onOpen: (folder: Folder) => void;
  /** Toggles selection (hover checkbox). */
  onSelect: (id: string) => void;
  /** When false the selection checkbox is hidden (e.g. Files explorer). */
  selectable?: boolean;
  /** Context-menu trigger; when omitted the "More actions" button is hidden. */
  onOpenMenu?: (folder: Folder, x: number, y: number) => void;
}

/** Grid tile for a single folder — matches the file explorer card styling. */
export function FolderCard({
  folder,
  selected,
  ownerName,
  onOpen,
  onSelect,
  selectable = true,
  onOpenMenu,
}: FolderCardProps) {
  const handleOpenMenu = onOpenMenu
    ? (event: React.MouseEvent) => {
        event.preventDefault();
        onOpenMenu(folder, event.clientX, event.clientY);
      }
    : undefined;
  return (
    <motion.div
      layout
      role="button"
      tabIndex={0}
      aria-pressed={selected}
      aria-label={`Open ${folder.name}`}
      onClick={() => onOpen(folder)}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          onOpen(folder);
        }
      }}
      onContextMenu={handleOpenMenu}
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
      {selectable && (
        <button
          type="button"
          aria-label={selected ? `Deselect ${folder.name}` : `Select ${folder.name}`}
          onClick={(event) => {
            event.stopPropagation();
            onSelect(folder.id);
          }}
          className={cn(
            'absolute top-2.5 left-2.5 z-10 grid h-5 w-5 cursor-pointer place-items-center rounded-md border transition-all duration-150',
            selected
              ? 'border-brand-500 bg-brand-500 text-white'
              : 'border-gray-300 bg-white opacity-0 group-hover:opacity-100 dark:border-gray-600 dark:bg-gray-900',
          )}
        >
          {selected && <Check className="h-3.5 w-3.5" />}
        </button>
      )}

      {/* More actions (revealed on hover) */}
      {handleOpenMenu && (
        <button
          type="button"
          onClick={(event) => {
            event.stopPropagation();
            handleOpenMenu(event);
          }}
          aria-label={`More actions for ${folder.name}`}
          className="absolute top-2 right-2 z-10 grid h-7 w-7 place-items-center rounded-lg text-gray-400 opacity-0 transition-all duration-150 group-hover:opacity-100 hover:bg-gray-100 hover:text-gray-700 dark:text-gray-500 dark:hover:bg-gray-800 dark:hover:text-gray-200"
        >
          <EllipsisVertical className="h-4 w-4" />
        </button>
      )}

      {/* Icon + name + meta */}
      <div className="mt-6 flex flex-col items-center gap-2.5">
        <div className="bg-brand-500/10 text-brand-600 dark:text-brand-400 grid h-14 w-14 place-items-center rounded-2xl">
          <FolderOpen className="h-7 w-7" />
        </div>
        <div className="w-full text-center">
          <p title={folder.name} className="truncate text-sm font-medium text-gray-900 dark:text-white">
            {folder.name}
          </p>
          <p className="mt-0.5 truncate text-xs text-gray-400 dark:text-gray-500">
            Folder · {formatFileDate(folder.createdAt)}
          </p>
        </div>
      </div>

      {/* Owner */}
      <div className="mt-3 flex items-center gap-1.5 border-t border-gray-100 pt-2.5 dark:border-gray-800">
        <span className="bg-brand-500/10 text-brand-600 dark:text-brand-300 grid h-5 w-5 shrink-0 place-items-center rounded-full text-[9px] font-bold">
          {getInitials(ownerName)}
        </span>
        <span className="truncate text-xs text-gray-500 dark:text-gray-400">{ownerName}</span>
      </div>
    </motion.div>
  );
}
