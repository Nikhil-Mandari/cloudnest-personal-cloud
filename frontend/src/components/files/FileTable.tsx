import type { MouseEvent as ReactMouseEvent } from 'react';
import { ArrowDown, ArrowUp, ArrowUpDown } from 'lucide-react';

import type { FileItem, SortDirection, SortKey } from '@/types';
import { cn } from '@/utils/cn';
import { FileRow } from './FileRow';

export interface FileTableProps {
  files: FileItem[];
  selectedIds: number[];
  sortKey: SortKey;
  sortDirection: SortDirection;
  onSortChange: (key: SortKey) => void;
  ownerName: string;
  /** Active search term — matched text is highlighted in file names. */
  searchQuery?: string;
  /** Ids of freshly uploaded files — their rows pulse. */
  highlightedIds?: number[];
  /** Dense rows used by compact view. */
  compact?: boolean;
  onSelect: (file: FileItem, event?: ReactMouseEvent) => void;
  onToggleFavorite: (file: FileItem) => void;
  onOpenMenu: (file: FileItem, x: number, y: number) => void;
  onPreview: (file: FileItem) => void;
}

interface SortableHeaderProps {
  label: string;
  column: SortKey;
  activeKey: SortKey;
  direction: SortDirection;
  onSortChange: (key: SortKey) => void;
  className?: string;
  align?: 'left' | 'right';
}

function SortableHeader({
  label,
  column,
  activeKey,
  direction,
  onSortChange,
  className,
  align = 'left',
}: SortableHeaderProps) {
  const active = activeKey === column;

  return (
    <th
      scope="col"
      className={cn(
        'px-3 py-3 text-xs font-semibold tracking-wide text-gray-500 uppercase dark:text-gray-400',
        align === 'right' ? 'text-right' : 'text-left',
        className,
      )}
    >
      <button
        type="button"
        onClick={() => onSortChange(column)}
        className={cn(
          'group/sort inline-flex items-center gap-1 uppercase transition-colors hover:text-gray-900 dark:hover:text-white',
          align === 'right' && 'flex-row-reverse',
          active && 'text-brand-600 dark:text-brand-400',
        )}
      >
        {label}
        <span className="grid h-3 w-3 place-items-center">
          {active ? (
            direction === 'asc' ? (
              <ArrowUp className="h-3 w-3" />
            ) : (
              <ArrowDown className="h-3 w-3" />
            )
          ) : (
            <ArrowUpDown className="h-3 w-3 opacity-0 transition-opacity group-hover/sort:opacity-60" />
          )}
        </span>
      </button>
    </th>
  );
}

/** Sortable, responsive table used by list and compact views. */
export function FileTable({
  files,
  selectedIds,
  sortKey,
  sortDirection,
  onSortChange,
  ownerName,
  searchQuery,
  highlightedIds = [],
  compact = false,
  onSelect,
  onToggleFavorite,
  onOpenMenu,
  onPreview,
}: FileTableProps) {
  return (
    <div className="overflow-hidden rounded-2xl border border-gray-200/80 bg-white shadow-sm shadow-gray-900/[0.03] dark:border-gray-800 dark:bg-gray-900">
      <div className="overflow-x-auto">
        <table className="w-full border-collapse">
          <thead>
            <tr className="border-b border-gray-100 dark:border-gray-800">
              <th
                scope="col"
                className={cn('w-12 py-3 pr-0 pl-4', compact && 'hidden')}
                aria-label="Select"
              />
              <SortableHeader
                label="Name"
                column="name"
                activeKey={sortKey}
                direction={sortDirection}
                onSortChange={onSortChange}
              />
              <SortableHeader
                label="Type"
                column="type"
                activeKey={sortKey}
                direction={sortDirection}
                onSortChange={onSortChange}
                className="hidden md:table-cell"
              />
              <SortableHeader
                label="Size"
                column="size"
                activeKey={sortKey}
                direction={sortDirection}
                onSortChange={onSortChange}
                className="text-right sm:text-left"
              />
              <SortableHeader
                label="Modified"
                column="date"
                activeKey={sortKey}
                direction={sortDirection}
                onSortChange={onSortChange}
                className="hidden lg:table-cell"
              />
              <th
                scope="col"
                className="hidden py-3 pr-3 text-xs font-semibold tracking-wide text-gray-500 uppercase xl:table-cell dark:text-gray-400"
              >
                Owner
              </th>
              <th scope="col" className="w-20 py-3 pr-4" aria-label="Actions" />
            </tr>
          </thead>
          <tbody>
            {files.map((file) => (
              <FileRow
                key={file.id}
                file={file}
                selected={selectedIds.includes(file.id)}
                ownerName={ownerName}
                searchQuery={searchQuery}
                highlighted={highlightedIds.includes(file.id)}
                compact={compact}
                onSelect={onSelect}
                onToggleFavorite={onToggleFavorite}
                onOpenMenu={onOpenMenu}
                onPreview={onPreview}
              />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
