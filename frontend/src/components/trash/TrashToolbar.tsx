import { ArrowDown, ArrowUp, ArrowUpDown, Clock, RotateCcw, Trash2 } from 'lucide-react';

import { DropdownMenu, type DropdownOption } from '@/components/files/DropdownMenu';
import { FilesSearchBar } from '@/components/files/SearchBar';
import { Button } from '@/components/ui/Button';
import type { SortDirection } from '@/types';
import { cn } from '@/utils/cn';
import type { TrashSortKey } from '@/utils/trash';

export interface TrashToolbarProps {
  /** Number of items currently visible after search. */
  resultCount: number;
  /** Total items in the trash (drives the empty-trash button). */
  totalCount: number;
  searchQuery: string;
  onSearchChange: (query: string) => void;
  sortKey: TrashSortKey;
  sortDirection: SortDirection;
  onSortChange: (key: TrashSortKey) => void;
  onSortDirectionChange: () => void;
  onRestoreAll: () => void;
  onEmptyTrash: () => void;
  isRestoring: boolean;
  isEmptying: boolean;
}

const SORT_OPTIONS: ReadonlyArray<DropdownOption<TrashSortKey>> = [
  { value: 'name', label: 'Name', icon: <ArrowUpDown className="h-4 w-4" /> },
  { value: 'date', label: 'Deleted', icon: <Clock className="h-4 w-4" /> },
];

const KEY_LABELS: Record<TrashSortKey, string> = {
  name: 'Name',
  date: 'Deleted',
};

/** Toolbar: search, sort + direction, and the empty-trash action. */
export function TrashToolbar({
  resultCount,
  totalCount,
  searchQuery,
  onSearchChange,
  sortKey,
  sortDirection,
  onSortChange,
  onSortDirectionChange,
  onRestoreAll,
  onEmptyTrash,
  isRestoring,
  isEmptying,
}: TrashToolbarProps) {
  const DirectionIcon = sortDirection === 'asc' ? ArrowUp : ArrowDown;

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-2">
        <FilesSearchBar
          value={searchQuery}
          onChange={onSearchChange}
          placeholder="Search trash…"
          className="min-w-40 flex-1"
        />

        <div className="ml-auto flex flex-wrap items-center gap-2">
          <div className="flex items-center gap-1.5">
            <DropdownMenu<TrashSortKey>
              value={sortKey}
              options={SORT_OPTIONS}
              onChange={onSortChange}
              label="Sort trash"
              triggerContent={<span className="text-gray-500 dark:text-gray-400">Sort: </span>}
            />

            <button
              type="button"
              onClick={onSortDirectionChange}
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

          <Button
            variant="outline"
            leftIcon={<RotateCcw className="h-4 w-4" />}
            disabled={totalCount === 0 || isRestoring || isEmptying}
            isLoading={isRestoring}
            onClick={onRestoreAll}
            className="text-emerald-600 hover:bg-emerald-50 hover:text-emerald-700 dark:text-emerald-400 dark:hover:bg-emerald-500/10"
          >
            Restore all
          </Button>

          <Button
            variant="outline"
            leftIcon={<Trash2 className="h-4 w-4" />}
            disabled={totalCount === 0 || isRestoring || isEmptying}
            isLoading={isEmptying}
            onClick={onEmptyTrash}
            className="text-rose-600 hover:bg-rose-50 hover:text-rose-700 dark:text-rose-400 dark:hover:bg-rose-500/10"
          >
            Empty trash
          </Button>
        </div>
      </div>

      <p className="text-xs text-gray-400 dark:text-gray-500">
        {resultCount} item{resultCount === 1 ? '' : 's'} in trash
        {searchQuery.trim() && ` · matching “${searchQuery.trim()}”`}
      </p>
    </div>
  );
}
