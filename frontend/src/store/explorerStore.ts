import { create } from 'zustand';

/**
 * Shared file-explorer navigation state.
 *
 * Both the Files page and the Folders page navigate the same folder tree, so
 * the current folder and its breadcrumb trail live here (one store, two
 * views). The trail is rebuilt from the folder hierarchy whenever the current
 * folder changes (see `useExplorerNavigation`).
 *
 * Deliberately NOT persisted — the URL (`?folder=<uuid>`) is the deep-linkable
 * source of truth for a reload.
 */

/** A single breadcrumb entry; `id === null` represents the root (My Files). */
export interface ExplorerCrumb {
  id: string | null;
  name: string;
}

/** Virtual root crumb rendered at the start of every trail. */
export const HOME_CRUMB: ExplorerCrumb = { id: null, name: 'My Files' };

type TrailUpdater = ExplorerCrumb[] | ((prev: ExplorerCrumb[]) => ExplorerCrumb[]);

interface ExplorerState {
  /** Currently open folder (null = root level). */
  currentFolderId: string | null;
  /** Breadcrumb trail from the root down to the current folder. */
  trail: ExplorerCrumb[];

  setCurrentFolder: (folderId: string | null) => void;
  setTrail: (trail: TrailUpdater) => void;
  /** Resets navigation back to the root ("My Files"). */
  reset: () => void;
}

export const useExplorerStore = create<ExplorerState>((set) => ({
  currentFolderId: null,
  trail: [HOME_CRUMB],

  setCurrentFolder: (folderId) =>
    set((state) => (state.currentFolderId === folderId ? state : { currentFolderId: folderId })),
  setTrail: (trail) =>
    set((state) => ({
      trail: typeof trail === 'function' ? trail(state.trail) : trail,
    })),
  reset: () => set({ currentFolderId: null, trail: [HOME_CRUMB] }),
}));
