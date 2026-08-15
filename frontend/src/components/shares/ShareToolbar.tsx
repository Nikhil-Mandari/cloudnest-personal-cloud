import { ArrowDown, ArrowUp, Clock, FileText, FolderOpen, LayoutGrid, List } from 'lucide-react';

import { DropdownMenu, type DropdownOption } from '@/components/files/DropdownMenu';
import { FilesSearchBar } from '@/components/files/SearchBar';
import type {
  FileViewMode,
  ShareSortKey,
  ShareTypeFilter,
  SortDirection,
} from '@/types';
import { cn } from '@/utils/cn';

export interface ShareToolbarProps {
  resultCount: number;
  searchQuery: string;
  onSearchChange: (value: string) => void;
  typeFilter: ShareTypeFilter;
  onTypeFilterChange: (value: ShareTypeFilter) => void;
  sortKey: ShareSortKey;
  sortDirection: SortDirection;
  onSortKeyChange: (value: ShareSortKey) => void;
  onSortDirectionChange: () => void;
  viewMode: FileViewMode;
  onViewModeChange: (mode: FileViewMode) => void;
}

const TYPE_OPTIONS: ReadonlyArray<DropdownOption<ShareTypeFilter>> = [
  { value: 'all', label: 'All', icon: <LayoutGrid className="h-4 w-4" /> },
  { value: 'FILE', label: 'Files', icon: <FileText className="h-4 w-4" /> },
  { value: 'FOLDER', label: 'Folders', icon: <FolderOpen className="h-4 w-4" /> },
];

const SORT_OPTIONS: ReadonlyArray<DropdownOption<ShareSortKey>> = [
  { value: 'date', label: 'Date shared', icon: <Clock className="h-4 w-4" /> },
  { value: 'type', label: 'Type', icon: <FileText className="h-4 w-4" /> },
];

/** Toolbar: search, file/folder filter, sort and view toggle. */
export function ShareToolbar({
  resultCount,
  searchQuery,
  onSearchChange,
  typeFilter,
  onTypeFilterChange,
  sortKey,
  sortDirection,
  onSortKeyChange,
  onSortDirectionChange,
  viewMode,
  onViewModeChange,
}: ShareToolbarProps) {
  const viewModes: FileViewMode[] = ['grid', 'list'];
  const DirectionIcon = sortDirection === 'asc' ? ArrowUp : ArrowDown;

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-2">
        <FilesSearchBar
          value={searchQuery}
          onChange={onSearchChange}
          placeholder="Search shared items…"
          className="min-w-40 flex-1"
        />

        <div className="ml-auto flex flex-wrap items-center gap-2">
          <DropdownMenu<ShareTypeFilter>
            value={typeFilter}
            options={TYPE_OPTIONS}
            onChange={onTypeFilterChange}
            label="Filter shared items"
            icon={<FileText className="h-4 w-4" />}
          />

          <DropdownMenu<ShareSortKey>
            value={sortKey}
            options={SORT_OPTIONS}
            onChange={onSortKeyChange}
            label="Sort shared items"
            icon={<Clock className="h-4 w-4" />}
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

          <div
            role="group"
            aria-label="View mode"
            className="flex overflow-hidden rounded-lg border border-gray-300 bg-white shadow-sm dark:border-gray-700 dark:bg-gray-900"
          >
            {viewModes.map((mode) => (
              <button
                key={mode}
                type="button"
                onClick={() => onViewModeChange(mode)}
                aria-label={`${mode} view`}
                aria-pressed={viewMode === mode}
                className={cn(
                  'grid h-10 w-10 place-items-center transition-colors',
                  viewMode === mode
                    ? 'bg-brand-500/10 text-brand-600 dark:text-brand-400'
                    : 'text-gray-400 hover:bg-gray-50 hover:text-gray-600 dark:hover:bg-gray-800 dark:hover:text-gray-300',
                )}
              >
                {mode === 'grid' ? (
                  <LayoutGrid className="h-4 w-4" />
                ) : (
                  <List className="h-4 w-4" />
                )}
              </button>
            ))}
          </div>
        </div>
      </div>

      <p className="text-xs text-gray-400 dark:text-gray-500">
        {resultCount} shared item{resultCount === 1 ? '' : 's'}
        {searchQuery.trim() && ` · matching “${searchQuery.trim()}”`}
      </p>
    </div>
  );
}
