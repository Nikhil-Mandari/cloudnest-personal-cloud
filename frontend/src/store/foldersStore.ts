import { create } from 'zustand';
import { persist } from 'zustand/middleware';

import { STORAGE_KEYS } from '@/constants/storage';
import type { FileViewMode, FolderSortKey, SortDirection } from '@/types';

/**
 * Folder explorer UI state.
 *
 * Layout preferences (view mode, sort) are persisted; search text and the
 * current selection are intentionally transient.
 */
interface FoldersState {
  viewMode: FileViewMode;
  sortKey: FolderSortKey;
  sortDirection: SortDirection;
  searchQuery: string;
  selectedIds: string[];

  setViewMode: (mode: FileViewMode) => void;
  setSortKey: (key: FolderSortKey) => void;
  setSortDirection: (direction: SortDirection) => void;
  setSearchQuery: (query: string) => void;
  toggleSelect: (id: string) => void;
  selectOnly: (ids: string[]) => void;
  clearSelection: () => void;
}

export const useFoldersStore = create<FoldersState>()(
  persist(
    (set) => ({
      viewMode: 'grid',
      sortKey: 'name',
      sortDirection: 'asc',
      searchQuery: '',
      selectedIds: [],

      setViewMode: (viewMode) => set({ viewMode }),
      setSortKey: (sortKey) => set({ sortKey }),
      setSortDirection: (sortDirection) => set({ sortDirection }),
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
      name: STORAGE_KEYS.folders,
      partialize: (state) => ({
        viewMode: state.viewMode,
        sortKey: state.sortKey,
        sortDirection: state.sortDirection,
      }),
    },
  ),
);
