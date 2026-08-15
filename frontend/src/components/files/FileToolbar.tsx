import { LayoutGrid, List, PanelRight, Rows3, Upload } from 'lucide-react';

import { Button } from '@/components/ui/Button';
import { useFilesStore } from '@/store/filesStore';
import type { FileItem, FileViewMode } from '@/types';
import { cn } from '@/utils/cn';
import { FilesSearchBar } from './SearchBar';
import { FilterDropdown } from './FilterDropdown';
import { SortDropdown } from './SortDropdown';

export interface FileToolbarProps {
  /** Full (unfiltered) file list — used for filter counts. */
  files: FileItem[];
  /** Ids of files the user has created a share link for (shared filter). */
  sharedFileIds?: ReadonlySet<number>;
  /** Number of files currently visible after search + filter. */
  resultCount: number;
  /** Whether the right-hand details panel is open. */
  detailsOpen: boolean;
  onToggleDetails: () => void;
  onUpload: () => void;
}

const VIEW_ICONS: Record<FileViewMode, typeof LayoutGrid> = {
  grid: LayoutGrid,
  list: List,
  compact: Rows3,
};

/** Toolbar: upload, instant search, type filter, sort and view toggle. */
export function FileToolbar({
  files,
  sharedFileIds,
  resultCount,
  detailsOpen,
  onToggleDetails,
  onUpload,
}: FileToolbarProps) {
  const viewMode = useFilesStore((state) => state.viewMode);
  const setViewMode = useFilesStore((state) => state.setViewMode);
  const searchQuery = useFilesStore((state) => state.searchQuery);
  const setSearchQuery = useFilesStore((state) => state.setSearchQuery);
  const filter = useFilesStore((state) => state.filter);
  const setFilter = useFilesStore((state) => state.setFilter);
  const sortKey = useFilesStore((state) => state.sortKey);
  const setSortKey = useFilesStore((state) => state.setSortKey);
  const sortDirection = useFilesStore((state) => state.sortDirection);
  const setSortDirection = useFilesStore((state) => state.setSortDirection);

  const viewModes: FileViewMode[] = ['grid', 'list', 'compact'];

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-2">
        <Button variant="primary" onClick={onUpload} leftIcon={<Upload className="h-4 w-4" />}>
          Upload
        </Button>

        <FilesSearchBar
          value={searchQuery}
          onChange={setSearchQuery}
          placeholder="Search files…"
          className="min-w-40 flex-1"
        />

        <div className="ml-auto flex flex-wrap items-center gap-2">
          <FilterDropdown
            value={filter}
            onChange={setFilter}
            files={files}
            sharedFileIds={sharedFileIds}
          />
          <SortDropdown
            sortKey={sortKey}
            sortDirection={sortDirection}
            onSortKeyChange={setSortKey}
            onSortDirectionChange={setSortDirection}
          />

          <div
            role="group"
            aria-label="View mode"
            className="flex overflow-hidden rounded-lg border border-gray-300 bg-white shadow-sm dark:border-gray-700 dark:bg-gray-900"
          >
            {viewModes.map((mode) => {
              const Icon = VIEW_ICONS[mode];
              return (
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
                  <Icon className="h-4 w-4" />
                </button>
              );
            })}
            {/* Details panel toggle (Google Drive style) */}
            <button
              type="button"
              onClick={onToggleDetails}
              aria-label="Toggle details panel"
              aria-pressed={detailsOpen}
              title="Details"
              className={cn(
                'grid h-10 w-10 place-items-center border-l border-gray-200 transition-colors dark:border-gray-700',
                detailsOpen
                  ? 'bg-brand-500/10 text-brand-600 dark:text-brand-400'
                  : 'text-gray-400 hover:bg-gray-50 hover:text-gray-600 dark:hover:bg-gray-800 dark:hover:text-gray-300',
              )}
            >
              <PanelRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>

      <p className="text-xs text-gray-400 dark:text-gray-500">
        {resultCount} file{resultCount === 1 ? '' : 's'}
        {searchQuery.trim() && ` · matching “${searchQuery.trim()}”`}
      </p>
    </div>
  );
}
