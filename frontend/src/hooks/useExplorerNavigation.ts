import { useCallback, useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import { folderService } from '@/services/folder.service';
import { HOME_CRUMB, useExplorerStore, type ExplorerCrumb } from '@/store/explorerStore';
import type { Folder } from '@/types';

const FOLDER_PARAM = 'folder';
const MAX_TRAIL_DEPTH = 20;

/**
 * Shared folder navigation for the Files and Folders explorers.
 *
 * Owns:
 *  - the current folder + breadcrumb trail (explorerStore),
 *  - the `?folder=<uuid>` URL deep-link — initialised once on mount and kept
 *    in sync afterwards with `replace: true`, so back/forward history stays
 *    clean while the folder is still shareable / reloadable.
 *
 *  - `openFolder(folder)`  — click a folder: append to the trail + update URL
 *  - `goToCrumb(crumb)`    — click a breadcrumb entry: truncate the trail
 *  - `goUp()`              — the "up one level" back button
 *  - page reload with `?folder=` — the trail is resolved from the folder
 *    hierarchy (single flat fetch) and the explorer opens there.
 */
export function useExplorerNavigation() {
  const currentFolderId = useExplorerStore((state) => state.currentFolderId);
  const trail = useExplorerStore((state) => state.trail);
  const setCurrentFolder = useExplorerStore((state) => state.setCurrentFolder);
  const setTrail = useExplorerStore((state) => state.setTrail);

  const [searchParams, setSearchParams] = useSearchParams();
  // Without a `?folder=` param there is nothing to resolve — ready immediately.
  const [isReady, setIsReady] = useState(() => !searchParams.get(FOLDER_PARAM));
  const [isResolving, setIsResolving] = useState(false);
  const initializedRef = useRef(false);

  // ── 1. Initialise once from the URL (deep-link / reload support) ──────────
  useEffect(() => {
    if (initializedRef.current) {
      return;
    }
    initializedRef.current = true;

    const folderParam = searchParams.get(FOLDER_PARAM);
    if (!folderParam) {
      // Store defaults to the root, but a previous page visit may have left a
      // deeper trail — reset it. (`isReady` is already true from the initial state.)
      setTrail([HOME_CRUMB]);
      setCurrentFolder(null);
      return;
    }

    let cancelled = false;
    void (async () => {
      setIsResolving(true);
      try {
        const crumbs = await resolveTrail(folderParam);
        if (!cancelled) {
          setTrail(crumbs);
          setCurrentFolder(crumbs[crumbs.length - 1]?.id ?? null);
        }
      } catch {
        if (!cancelled) {
          // Unknown / inaccessible folder — fall back to the root.
          setTrail([HOME_CRUMB]);
          setCurrentFolder(null);
        }
      } finally {
        if (!cancelled) {
          setIsResolving(false);
          setIsReady(true);
        }
      }
    })();

    return () => {
      // StrictMode runs setup → cleanup → setup; reset the flag so the second
      // setup re-runs the (idempotent) initialisation instead of deadlocking.
      cancelled = true;
      initializedRef.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ── 2. Mirror the current folder into the URL (after initialisation, so a
  //       deep-link param is never wiped before it has been resolved) ─────────
  useEffect(() => {
    if (!isReady) {
      return;
    }
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        if (currentFolderId) {
          next.set(FOLDER_PARAM, currentFolderId);
        } else {
          next.delete(FOLDER_PARAM);
        }
        return next;
      },
      { replace: true },
    );
  }, [currentFolderId, isReady, setSearchParams]);

  // ── 3. Navigation actions ──────────────────────────────────────────────────

  /** Opens a folder: appends it to the trail and updates the URL. */
  const openFolder = useCallback(
    (folder: Pick<Folder, 'id' | 'name'>) => {
      const current = useExplorerStore.getState().trail;
      setTrail([...current, { id: folder.id, name: folder.name }].slice(-MAX_TRAIL_DEPTH));
      setCurrentFolder(folder.id);
    },
    [setCurrentFolder, setTrail],
  );

  /** Jumps to a breadcrumb entry, truncating everything below it. */
  const goToCrumb = useCallback(
    (crumb: ExplorerCrumb) => {
      const current = useExplorerStore.getState().trail;
      const index = current.findIndex((entry) => entry.id === crumb.id);
      setTrail(index === -1 ? [HOME_CRUMB, crumb] : current.slice(0, index + 1));
      setCurrentFolder(crumb.id);
    },
    [setCurrentFolder, setTrail],
  );

  /** Navigates up one level (no-op at the root). */
  const goUp = useCallback(() => {
    const current = useExplorerStore.getState().trail;
    if (current.length <= 1) {
      return;
    }
    const next = current.slice(0, -1);
    setTrail(next);
    setCurrentFolder(next[next.length - 1]?.id ?? null);
  }, [setCurrentFolder, setTrail]);

  /** Returns to the root ("My Files"). */
  const goHome = useCallback(() => {
    setTrail([HOME_CRUMB]);
    setCurrentFolder(null);
  }, [setCurrentFolder, setTrail]);

  return {
    currentFolderId,
    trail,
    isResolving,
    openFolder,
    goToCrumb,
    goUp,
    goHome,
  };
}

/**
 * Resolves the breadcrumb trail for a folder UUID by walking the parent chain.
 *
 * Uses a single flat fetch of the user's folders to build an id→folder map,
 * then walks up from the target folder. Returns `[HOME_CRUMB]` (root) when the
 * folder is missing, deleted, or not owned by the user.
 */
async function resolveTrail(folderId: string): Promise<ExplorerCrumb[]> {
  const { data } = await folderService.getFolders();
  const byId = new Map((data.data ?? []).map((folder) => [folder.id, folder]));

  const crumbs: ExplorerCrumb[] = [HOME_CRUMB];
  const seen = new Set<string>();
  let currentId: string | null = folderId;

  while (currentId && crumbs.length < MAX_TRAIL_DEPTH) {
    if (seen.has(currentId)) {
      break;
    }
    seen.add(currentId);
    const folder = byId.get(currentId);
    if (!folder) {
      break;
    }
    crumbs.push({ id: folder.id, name: folder.name });
    currentId = folder.parentFolderId ?? null;
  }

  return crumbs;
}
