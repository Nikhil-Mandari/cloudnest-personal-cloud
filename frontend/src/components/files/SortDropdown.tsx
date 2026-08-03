import { ArrowDown, ArrowUp, ArrowUpDown, Clock, Files, HardDrive } from 'lucide-react';

import type { SortDirection, SortKey } from '@/types';
import { cn } from '@/utils/cn';
import { DropdownMenu, type DropdownOption } from './DropdownMenu';

export interface SortDropdownProps {
  sortKey: SortKey;
  sortDirection: SortDirection;
  onSortKeyChange: (key: SortKey) => void;
  onSortDirectionChange: (direction: SortDirection) => void;
}

const SORT_OPTIONS: ReadonlyArray<DropdownOption<SortKey>> = [
  { value: 'name', label: 'Name', icon: <ArrowUpDown className="h-4 w-4" /> },
  { value: 'size', label: 'Size', icon: <HardDrive className="h-4 w-4" /> },
  { value: 'date', label: 'Date modified', icon: <Clock className="h-4 w-4" /> },
  { value: 'type', label: 'Type', icon: <Files className="h-4 w-4" /> },
];

const KEY_LABELS: Record<SortKey, string> = {
  name: 'Name',
  size: 'Size',
  date: 'Date modified',
  type: 'Type',
};

/** Sort key picker plus an ascending/descending toggle. */
export function SortDropdown({
  sortKey,
  sortDirection,
  onSortKeyChange,
  onSortDirectionChange,
}: SortDropdownProps) {
  const DirectionIcon = sortDirection === 'asc' ? ArrowUp : ArrowDown;

  return (
    <div className="flex items-center gap-1.5">
      <DropdownMenu<SortKey>
        value={sortKey}
        options={SORT_OPTIONS}
        onChange={onSortKeyChange}
        label="Sort files"
        triggerContent={<span className="text-gray-500 dark:text-gray-400">Sort: </span>}
      />

      <button
        type="button"
        onClick={() => onSortDirectionChange(sortDirection === 'asc' ? 'desc' : 'asc')}
        aria-label={`Sort ${sortDirection === 'asc' ? 'descending' : 'ascending'}`}
        title={`Sort ${sortDirection === 'asc' ? 'descending' : 'ascending'}`}
        className={cn(
          'grid h-10 w-10 place-items-center rounded-lg border border-gray-300 bg-white text-gray-600 shadow-sm transition-colors',
          'focus-visible:ring-brand-500/50 hover:bg-gray-50 focus-visible:ring-2 focus-visible:outline-none',
          'dark:border-gray-700 dark:bg-gray-900 dark:text-gray-300 dark:hover:bg-gray-800/70',
        )}
      >
        <DirectionIcon className="h-4 w-4" />
      </button>

      <span className="hidden text-xs text-gray-400 sm:inline dark:text-gray-500">
        {KEY_LABELS[sortKey]}
      </span>
    </div>
  );
}
