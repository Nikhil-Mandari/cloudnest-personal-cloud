import { create } from 'zustand';
import { persist } from 'zustand/middleware';

import { STORAGE_KEYS } from '@/constants/storage';
import type { FileTypeFilter, FileViewMode, SortDirection, SortKey } from '@/types';

/**
 * File explorer UI state.
 *
 * Layout preferences (view mode, sort, filter) are persisted; search text and
 * the current selection are intentionally transient.
 */
interface FilesState {
  viewMode: FileViewMode;
  sortKey: SortKey;
  sortDirection: SortDirection;
  filter: FileTypeFilter;
  searchQuery: string;
  selectedIds: number[];

  setViewMode: (mode: FileViewMode) => void;
  setSortKey: (key: SortKey) => void;
  setSortDirection: (direction: SortDirection) => void;
  setFilter: (filter: FileTypeFilter) => void;
  setSearchQuery: (query: string) => void;
  toggleSelect: (id: number) => void;
  selectOnly: (ids: number[]) => void;
  clearSelection: () => void;
}

export const useFilesStore = create<FilesState>()(
  persist(
    (set) => ({
      viewMode: 'grid',
      sortKey: 'date',
      sortDirection: 'desc',
      filter: 'all',
      searchQuery: '',
      selectedIds: [],

      setViewMode: (viewMode) => set({ viewMode }),
      setSortKey: (sortKey) => set({ sortKey }),
      setSortDirection: (sortDirection) => set({ sortDirection }),
      setFilter: (filter) => set({ filter }),
      setSearchQuery: (searchQuery) => set({ searchQuery }),
      toggleSelect: (id) =>
        set((state) => ({
          selectedIds: state.selectedIds.includes(id)
            ? state.selectedIds.filter((selected) => selected !== id)
            : [...state.selectedIds, id],
        })),
      selectOnly: (ids) => set({ selectedIds: ids }),
      clearSelection: () => set({ selectedIds: [] }),
    }),
    {
      name: STORAGE_KEYS.files,
      partialize: (state) => ({
        viewMode: state.viewMode,
        sortKey: state.sortKey,
        sortDirection: state.sortDirection,
        filter: state.filter,
      }),
    },
  ),
);
