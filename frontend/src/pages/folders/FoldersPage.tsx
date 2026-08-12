import { useMemo, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { ArrowLeft, CloudUpload, FolderOpen, FolderPlus, Home, Pencil, Trash2, X } from 'lucide-react';

import { Breadcrumb } from '@/components/common/Breadcrumb';
import { ErrorState } from '@/components/common/ErrorState';
import { PageHeader } from '@/components/common/PageHeader';
import { DeleteFolderDialog } from '@/components/folders/DeleteFolderDialog';
import { FoldersEmptyState } from '@/components/folders/FolderEmptyState';
import { FolderGrid } from '@/components/folders/FolderGrid';
import {
  FolderGridSkeleton,
  FolderTableSkeleton,
} from '@/components/folders/FolderSkeletons';
import { FolderTable } from '@/components/folders/FolderTable';
import { FolderToolbar } from '@/components/folders/FolderToolbar';
import { NewFolderDialog } from '@/components/folders/NewFolderDialog';
import { RenameFolderDialog } from '@/components/folders/RenameFolderDialog';
import {
  FileContextMenu,
  type ContextMenuItem,
  type ContextMenuPosition,
} from '@/components/files/FileContextMenu';
import { UploadModal } from '@/components/files/UploadModal';
import { Button } from '@/components/ui/Button';
import { useAuth } from '@/hooks/useAuth';
import { useExplorerNavigation } from '@/hooks/useExplorerNavigation';
import { useFileMutations } from '@/hooks/useFiles';
import { useFolderContentsQuery, useFolderMutations } from '@/hooks/useFolders';
import type { ExplorerCrumb } from '@/store/explorerStore';
import { useFoldersStore } from '@/store/foldersStore';
import type { Folder, FolderSortKey } from '@/types';
import { cn } from '@/utils/cn';
import { getErrorMessage } from '@/utils/error';
import { filterFolders, sortFolders } from '@/utils/folder';

export function FoldersPage() {
  const { user } = useAuth();
  const { currentFolderId, trail, openFolder, goToCrumb, goUp, isResolving } =
    useExplorerNavigation();
  const {
    data: folders = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useFolderContentsQuery(currentFolderId);
  const mutations = useFolderMutations();
  const fileMutations = useFileMutations();

  // Explorer UI state (zustand).
  const viewMode = useFoldersStore((state) => state.viewMode);
  const sortKey = useFoldersStore((state) => state.sortKey);
  const sortDirection = useFoldersStore((state) => state.sortDirection);
  const searchQuery = useFoldersStore((state) => state.searchQuery);
  const selectedIds = useFoldersStore((state) => state.selectedIds);
  const setSortKey = useFoldersStore((state) => state.setSortKey);
  const setSortDirection = useFoldersStore((state) => state.setSortDirection);
  const toggleSelect = useFoldersStore((state) => state.toggleSelect);
  const selectOnly = useFoldersStore((state) => state.selectOnly);
  const clearSelection = useFoldersStore((state) => state.clearSelection);
  const setSearchQuery = useFoldersStore((state) => state.setSearchQuery);

  // Local dialog / overlay state.
  const [newFolderOpen, setNewFolderOpen] = useState(false);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [contextMenu, setContextMenu] = useState<{
    folder: Folder;
    position: ContextMenuPosition;
  } | null>(null);
  const [renameTarget, setRenameTarget] = useState<Folder | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Folder | null>(null);

  const ownerName = user?.displayName ?? user?.username ?? 'You';

  const visibleFolders = useMemo(
    () => sortFolders(filterFolders(folders, searchQuery), sortKey, sortDirection),
    [folders, searchQuery, sortKey, sortDirection],
  );

  // ── Handlers ──────────────────────────────────────────────────────────────

  const handleOpenMenu = (folder: Folder, x: number, y: number) => {
    if (!selectedIds.includes(folder.id)) {
      selectOnly([folder.id]);
    }
    setContextMenu({ folder, position: { x, y } });
  };

  const handleSortChange = (key: FolderSortKey) => {
    if (key === sortKey) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortKey(key);
      setSortDirection(key === 'date' ? 'desc' : 'asc');
    }
  };

  const handleCreateFolder = (name: string) => {
    // Create inside the folder currently being viewed (root when none).
    mutations.createFolder.mutate(
      { name, parentFolderId: currentFolderId ?? undefined },
      { onSettled: () => setNewFolderOpen(false) },
    );
  };

  // ── Folder navigation ──────────────────────────────────────────────────────

  /** Single click on a folder card / row → navigate into it. */
  const handleOpenFolder = (folder: Folder) => {
    clearSelection();
    openFolder(folder);
  };

  /** Breadcrumb / back-navigation target. */
  const handleGoToCrumb = (crumb: ExplorerCrumb) => {
    clearSelection();
    goToCrumb(crumb);
  };

  const handleGoUp = () => {
    clearSelection();
    goUp();
  };

  const handleRename = (folder: Folder, name: string) => {
    mutations.renameFolder.mutate(
      { id: folder.id, name },
      { onSettled: () => setRenameTarget(null) },
    );
  };

  const handleDelete = (folder: Folder) => {
    clearSelection();
    mutations.deleteFolder.mutate(folder.id, { onSettled: () => setDeleteTarget(null) });
  };

  // ── Context menu items ────────────────────────────────────────────────────

  const contextItems = useMemo<ContextMenuItem[]>(() => {
    const folder = contextMenu?.folder;
    if (!folder) {
      return [];
    }
    return [
      {
        key: 'rename',
        label: 'Rename',
        icon: <Pencil className="h-4 w-4" />,
        onClick: () => {
          setContextMenu(null);
          setRenameTarget(folder);
        },
      },
      { key: 'separator', label: '', separator: true },
      {
        key: 'delete',
        label: 'Delete',
        icon: <Trash2 className="h-4 w-4" />,
        danger: true,
        onClick: () => {
          setContextMenu(null);
          setDeleteTarget(folder);
        },
      },
    ];
  }, [contextMenu]);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Folders"
        description="Keep your files tidy with folders and sub-folders."
      />

      {/* Folder navigation: up button + breadcrumb trail */}
      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={handleGoUp}
          disabled={!currentFolderId}
          aria-label="Go up one level"
          title="Up one level"
          className={cn(
            'grid h-8 w-8 shrink-0 place-items-center rounded-lg border border-gray-300 bg-white text-gray-600 shadow-sm transition-colors',
            'hover:bg-gray-50 hover:text-gray-900 focus-visible:ring-brand-500/50 focus-visible:ring-2 focus-visible:outline-none',
            'dark:border-gray-700 dark:bg-gray-900 dark:text-gray-300 dark:hover:bg-gray-800/70',
            !currentFolderId && 'pointer-events-none opacity-40',
          )}
        >
          <ArrowLeft className="h-4 w-4" />
        </button>
        <Breadcrumb
          items={trail.map((crumb) => ({
            label: crumb.name,
            icon: crumb.id ? <FolderOpen className="h-4 w-4" /> : <Home className="h-4 w-4" />,
            onClick: () => handleGoToCrumb(crumb),
          }))}
        />
      </div>

      <FolderToolbar
        resultCount={visibleFolders.length}
        onCreateFolder={() => setNewFolderOpen(true)}
        onUpload={() => setUploadOpen(true)}
      />

      {/* Content: skeleton → error → empty → grid/list */}
      {isLoading || isResolving ? (
        viewMode === 'grid' ? (
          <FolderGridSkeleton />
        ) : (
          <FolderTableSkeleton />
        )
      ) : isError ? (
        <ErrorState
          message={getErrorMessage(error, 'Failed to load your folders.')}
          onRetry={() => void refetch()}
        />
      ) : visibleFolders.length === 0 ? (
        currentFolderId && !searchQuery.trim() ? (
          <div className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-gray-300 bg-white/60 px-6 py-14 text-center dark:border-gray-700 dark:bg-gray-900/40">
            <div className="bg-brand-500/10 text-brand-500 grid h-14 w-14 place-items-center rounded-2xl">
              <FolderOpen className="h-7 w-7" />
            </div>
            <div>
              <p className="text-sm font-medium text-gray-900 dark:text-white">
                This folder is empty
              </p>
              <p className="mt-1 text-sm text-gray-400 dark:text-gray-500">
                Upload files or create a sub-folder to keep organising.
              </p>
            </div>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setUploadOpen(true)}
                leftIcon={<CloudUpload className="h-3.5 w-3.5" />}
              >
                Upload files
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={() => setNewFolderOpen(true)}
                leftIcon={<FolderPlus className="h-3.5 w-3.5" />}
              >
                New folder
              </Button>
            </div>
          </div>
        ) : (
          <FoldersEmptyState
            variant={folders.length === 0 ? 'no-folders' : 'no-search'}
            searchQuery={searchQuery}
            onCreateFolder={() => setNewFolderOpen(true)}
            onClearSearch={() => setSearchQuery('')}
          />
        )
      ) : viewMode === 'grid' ? (
        <FolderGrid
          folders={visibleFolders}
          selectedIds={selectedIds}
          ownerName={ownerName}
          onOpen={handleOpenFolder}
          onSelect={toggleSelect}
          onOpenMenu={handleOpenMenu}
        />
      ) : (
        <FolderTable
          folders={visibleFolders}
          selectedIds={selectedIds}
          sortKey={sortKey}
          sortDirection={sortDirection}
          onSortChange={handleSortChange}
          ownerName={ownerName}
          onOpen={handleOpenFolder}
          onSelect={toggleSelect}
          onOpenMenu={handleOpenMenu}
        />
      )}

      {/* Selection bar */}
      <AnimatePresence>
        {selectedIds.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 12 }}
            transition={{ duration: 0.2, ease: 'easeOut' }}
            className="bg-brand-500/10 dark:bg-brand-500/15 border-brand-200 dark:border-brand-500/30 flex items-center justify-between rounded-xl border px-4 py-2.5"
          >
            <p className="text-brand-700 dark:text-brand-300 text-sm font-medium">
              {selectedIds.length} folder{selectedIds.length === 1 ? '' : 's'} selected
            </p>
            <button
              type="button"
              onClick={clearSelection}
              className="flex items-center gap-1 text-sm text-gray-500 transition-colors hover:text-gray-900 dark:text-gray-400 dark:hover:text-white"
            >
              <X className="h-4 w-4" /> Clear
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Context menu */}
      <FileContextMenu
        open={contextMenu !== null}
        position={contextMenu?.position ?? { x: 0, y: 0 }}
        items={contextItems}
        onClose={() => setContextMenu(null)}
      />

      {/* Dialogs */}
      <UploadModal
        open={uploadOpen}
        onClose={() => setUploadOpen(false)}
        folderId={currentFolderId}
        onUploadComplete={fileMutations.invalidateFiles}
      />
      <NewFolderDialog
        open={newFolderOpen}
        onClose={() => setNewFolderOpen(false)}
        onConfirm={handleCreateFolder}
        isLoading={mutations.createFolder.isPending}
      />
      <RenameFolderDialog
        folder={renameTarget}
        open={renameTarget !== null}
        onClose={() => setRenameTarget(null)}
        onConfirm={handleRename}
        isLoading={mutations.renameFolder.isPending}
      />
      <DeleteFolderDialog
        folder={deleteTarget}
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        isLoading={mutations.deleteFolder.isPending}
      />
    </div>
  );
}
