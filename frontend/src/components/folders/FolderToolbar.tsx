import { ArrowDown, ArrowUp, ArrowUpDown, Clock, CloudUpload, FolderPlus, LayoutGrid, List } from 'lucide-react';

import { DropdownMenu, type DropdownOption } from '@/components/files/DropdownMenu';
import { FilesSearchBar } from '@/components/files/SearchBar';
import { Button } from '@/components/ui/Button';
import { useFoldersStore } from '@/store/foldersStore';
import type { FileViewMode, FolderSortKey } from '@/types';
import { cn } from '@/utils/cn';

export interface FolderToolbarProps {
  /** Number of folders currently visible after search. */
  resultCount: number;
  onCreateFolder: () => void;
  /** Optional: show an "Upload files" action (uploads into the open folder). */
  onUpload?: () => void;
}

const SORT_OPTIONS: ReadonlyArray<DropdownOption<FolderSortKey>> = [
  { value: 'name', label: 'Name', icon: <ArrowUpDown className="h-4 w-4" /> },
  { value: 'date', label: 'Date modified', icon: <Clock className="h-4 w-4" /> },
];

const KEY_LABELS: Record<FolderSortKey, string> = {
  name: 'Name',
  date: 'Date modified',
};

/** Toolbar: upload files, new folder, instant search, sort and view toggle. */
export function FolderToolbar({ resultCount, onCreateFolder, onUpload }: FolderToolbarProps) {
  const viewMode = useFoldersStore((state) => state.viewMode);
  const setViewMode = useFoldersStore((state) => state.setViewMode);
  const searchQuery = useFoldersStore((state) => state.searchQuery);
  const setSearchQuery = useFoldersStore((state) => state.setSearchQuery);
  const sortKey = useFoldersStore((state) => state.sortKey);
  const setSortKey = useFoldersStore((state) => state.setSortKey);
  const sortDirection = useFoldersStore((state) => state.sortDirection);
  const setSortDirection = useFoldersStore((state) => state.setSortDirection);

  const viewModes: FileViewMode[] = ['grid', 'list'];
  const DirectionIcon = sortDirection === 'asc' ? ArrowUp : ArrowDown;

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-2">
        {onUpload && (
          <Button
            variant="outline"
            onClick={onUpload}
            leftIcon={<CloudUpload className="h-4 w-4" />}
          >
            Upload files
          </Button>
        )}

        <Button
          variant="primary"
          onClick={onCreateFolder}
          leftIcon={<FolderPlus className="h-4 w-4" />}
        >
          New folder
        </Button>

        <FilesSearchBar
          value={searchQuery}
          onChange={setSearchQuery}
          placeholder="Search folders…"
          className="min-w-40 flex-1"
        />

        <div className="ml-auto flex flex-wrap items-center gap-2">
          <div className="flex items-center gap-1.5">
            <DropdownMenu<FolderSortKey>
              value={sortKey}
              options={SORT_OPTIONS}
              onChange={setSortKey}
              label="Sort folders"
              triggerContent={<span className="text-gray-500 dark:text-gray-400">Sort: </span>}
            />

            <button
              type="button"
              onClick={() => setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc')}
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

          <div
            role="group"
            aria-label="View mode"
            className="flex overflow-hidden rounded-lg border border-gray-300 bg-white shadow-sm dark:border-gray-700 dark:bg-gray-900"
          >
            {viewModes.map((mode) => (
              <button
                key={mode}
                type="button"
                onClick={() => setViewMode(mode)}
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
        {resultCount} folder{resultCount === 1 ? '' : 's'}
        {searchQuery.trim() && ` · matching “${searchQuery.trim()}”`}
      </p>
    </div>
  );
}
