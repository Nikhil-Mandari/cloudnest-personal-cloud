import { ArrowDown, ArrowUp, ArrowUpDown } from 'lucide-react';

import type { Folder, FolderSortKey, SortDirection } from '@/types';
import { cn } from '@/utils/cn';
import { FolderRow } from './FolderRow';

export interface FolderTableProps {
  folders: Folder[];
  selectedIds: string[];
  sortKey: FolderSortKey;
  sortDirection: SortDirection;
  onSortChange: (key: FolderSortKey) => void;
  ownerName: string;
  /** Opens the folder (navigates into it). */
  onOpen: (folder: Folder) => void;
  /** Toggles selection (checkbox). */
  onSelect: (id: string) => void;
  /** When false the selection checkbox is hidden (e.g. Files explorer). */
  selectable?: boolean;
  /** Context-menu trigger; when omitted the "More actions" button is hidden. */
  onOpenMenu?: (folder: Folder, x: number, y: number) => void;
}

interface SortableHeaderProps {
  label: string;
  column: FolderSortKey;
  activeKey: FolderSortKey;
  direction: SortDirection;
  onSortChange: (key: FolderSortKey) => void;
  className?: string;
}

function SortableHeader({
  label,
  column,
  activeKey,
  direction,
  onSortChange,
  className,
}: SortableHeaderProps) {
  const active = activeKey === column;

  return (
    <th
      scope="col"
      className={cn(
        'px-3 py-3 text-xs font-semibold tracking-wide text-gray-500 uppercase dark:text-gray-400',
        'text-left',
        className,
      )}
    >
      <button
        type="button"
        onClick={() => onSortChange(column)}
        className={cn(
          'group/sort inline-flex items-center gap-1 uppercase transition-colors hover:text-gray-900 dark:hover:text-white',
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

/** Sortable, responsive table used by list view. */
export function FolderTable({
  folders,
  selectedIds,
  sortKey,
  sortDirection,
  onSortChange,
  ownerName,
  onOpen,
  onSelect,
  selectable = true,
  onOpenMenu,
}: FolderTableProps) {
  return (
    <div className="overflow-hidden rounded-2xl border border-gray-200/80 bg-white shadow-sm shadow-gray-900/[0.03] dark:border-gray-800 dark:bg-gray-900">
      <div className="overflow-x-auto">
        <table className="w-full border-collapse">
          <thead>
            <tr className="border-b border-gray-100 dark:border-gray-800">
              <th scope="col" className="w-12 py-3 pr-0 pl-4" aria-label="Select" />
              <SortableHeader
                label="Name"
                column="name"
                activeKey={sortKey}
                direction={sortDirection}
                onSortChange={onSortChange}
              />
              <th
                scope="col"
                className="hidden px-3 py-3 text-xs font-semibold tracking-wide text-gray-500 uppercase md:table-cell dark:text-gray-400"
              >
                Type
              </th>
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
                className="hidden px-3 py-3 text-xs font-semibold tracking-wide text-gray-500 uppercase xl:table-cell dark:text-gray-400"
              >
                Owner
              </th>
              <th scope="col" className="w-20 py-3 pr-4" aria-label="Actions" />
            </tr>
          </thead>
          <tbody>
            {folders.map((folder) => (
              <FolderRow
                key={folder.id}
                folder={folder}
                selected={selectedIds.includes(folder.id)}
                ownerName={ownerName}
                onOpen={onOpen}
                onSelect={onSelect}
                selectable={selectable}
                onOpenMenu={onOpenMenu}
              />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
