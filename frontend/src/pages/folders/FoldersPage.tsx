import { useMemo, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import {
  ArrowLeft,
  CloudUpload,
  Download,
  Eye,
  FolderInput,
  FolderOpen,
  FolderPlus,
  Home,
  Info,
  Link2,
  Pencil,
  Share2,
  Star,
  Trash2,
  X,
} from 'lucide-react';
import { toast } from 'react-toastify';

import { Breadcrumb } from '@/components/common/Breadcrumb';
import { ErrorState } from '@/components/common/ErrorState';
import { PageHeader } from '@/components/common/PageHeader';
import { DeleteDialog } from '@/components/files/DeleteDialog';
import { DetailsPanel } from '@/components/files/DetailsPanel';
import {
  FileContextMenu,
  type ContextMenuItem,
  type ContextMenuPosition,
} from '@/components/files/FileContextMenu';
import { FileGrid } from '@/components/files/FileGrid';
import { FileGridSkeleton, FileTableSkeleton } from '@/components/files/FileSkeletons';
import { FileTable } from '@/components/files/FileTable';
import { MoveDialog } from '@/components/files/MoveDialog';
import { PreviewModal } from '@/components/files/PreviewModal';
import { RenameDialog } from '@/components/files/RenameDialog';
import { ShareDialog } from '@/components/files/ShareDialog';
import { UploadModal } from '@/components/files/UploadModal';
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
import { Button } from '@/components/ui/Button';
import { useAuth } from '@/hooks/useAuth';
import { useExplorerNavigation } from '@/hooks/useExplorerNavigation';
import { useFileMutations, useFilesQuery } from '@/hooks/useFiles';
import { useFolderContentsQuery, useFolderMutations } from '@/hooks/useFolders';
import { useMySharesQuery } from '@/hooks/useShare';
import { shareService } from '@/services/share.service';
import type { ExplorerCrumb } from '@/store/explorerStore';
import { useFilesStore } from '@/store/filesStore';
import { useFoldersStore } from '@/store/foldersStore';
import type { FileItem, Folder, FolderSortKey, SortKey } from '@/types';
import { cn } from '@/utils/cn';
import { copyToClipboard } from '@/utils/download';
import { getErrorMessage } from '@/utils/error';
import { buildShareUrl, filterFiles, sortFiles } from '@/utils/file';
import { filterFolders, sortFolders } from '@/utils/folder';

/** Maps the folder sort key onto the shared file sort key (same columns). */
const FOLDER_TO_FILE_SORT: Record<FolderSortKey, SortKey> = {
  name: 'name',
  date: 'date',
};

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
  const { data: files = [], isLoading: filesLoading } = useFilesQuery(currentFolderId);
  const mutations = useFolderMutations();
  const fileMutations = useFileMutations();

  // Explorer UI state (zustand) — shared by the folder and file sections.
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
  // File selection lives in the files store (numeric ids), so file clicks and
  // the folders store's string selection never collide.
  const selectOnlyFiles = useFilesStore((state) => state.selectOnly);

  // Local dialog / overlay state (folders + files).
  const [newFolderOpen, setNewFolderOpen] = useState(false);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [contextMenu, setContextMenu] = useState<{
    folder: Folder;
    position: ContextMenuPosition;
  } | null>(null);
  const [renameTarget, setRenameTarget] = useState<Folder | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Folder | null>(null);

  // File actions (preview / move / rename / share / delete / details).
  const [fileContextMenu, setFileContextMenu] = useState<{
    file: FileItem;
    position: ContextMenuPosition;
  } | null>(null);
  const [previewTarget, setPreviewTarget] = useState<FileItem | null>(null);
  const [moveTarget, setMoveTarget] = useState<FileItem | null>(null);
  const [shareTarget, setShareTarget] = useState<FileItem | null>(null);
  const [renameFileTarget, setRenameFileTarget] = useState<FileItem | null>(null);
  const [deleteFileTarget, setDeleteFileTarget] = useState<FileItem | null>(null);
  const [detailsFile, setDetailsFile] = useState<FileItem | null>(null);

  // Ids of files the user has created share links for (drives the shared filter).
  const { data: shares } = useMySharesQuery();
  const sharedFileIds = useMemo(() => {
    const ids = new Set<number>();
    for (const share of shares ?? []) {
      const id = Number(share.resourceId);
      if (Number.isFinite(id)) {
        ids.add(id);
      }
    }
    return ids;
  }, [shares]);

  const ownerName = user?.displayName ?? user?.username ?? 'You';

  const visibleFolders = useMemo(
    () => sortFolders(filterFolders(folders, searchQuery), sortKey, sortDirection),
    [folders, searchQuery, sortKey, sortDirection],
  );

  // Files at the current location, filtered by the same search term and sorted
  // with the folder page's sort preference (name / date).
  const visibleFiles = useMemo(
    () =>
      sortFiles(
        filterFiles(files, searchQuery, 'all', sharedFileIds),
        FOLDER_TO_FILE_SORT[sortKey],
        sortDirection,
      ),
    [files, searchQuery, sortKey, sortDirection, sharedFileIds],
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

  // ── File handlers ──────────────────────────────────────────────────────────

  const handleFileSelect = (file: FileItem) => {
    selectOnlyFiles([file.id]);
    setDetailsFile(file);
  };

  const handleFileOpenMenu = (file: FileItem, x: number, y: number) => {
    setFileContextMenu({ file, position: { x, y } });
  };

  const handleMove = (file: FileItem, folderId: string | null) => {
    fileMutations.moveFile.mutate({ id: file.id, folderId }, { onSettled: () => setMoveTarget(null) });
  };

  const handleDeleteFile = (file: FileItem) => {
    clearSelection();
    fileMutations.deleteFile.mutate(file.id, { onSettled: () => setDeleteFileTarget(null) });
  };

  const handleCopyLink = async (file: FileItem) => {
    setFileContextMenu(null);
    try {
      const { data } = await shareService.getMyShares();
      const share = data.data.find((record) => record.resourceId === String(file.id));
      if (!share) {
        toast.info('No link yet — share this file first');
        setShareTarget(file);
        return;
      }
      const ok = await copyToClipboard(buildShareUrl(share.shareToken));
      if (ok) {
        toast.success('Link copied to clipboard');
      } else {
        toast.error('Could not copy the link');
      }
    } catch (copyError) {
      toast.error(getErrorMessage(copyError, 'Failed to copy the link'));
    }
  };

  const handleUploadComplete = (ids: number[]) => {
    if (ids.length > 0 && ids.length === 1) {
      const uploaded = files.find((item) => item.id === ids[0]);
      if (uploaded) {
        setDetailsFile(uploaded);
      }
    }
  };

  // ── Context menu items (folders) ──────────────────────────────────────────

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

  // ── Context menu items (files) ────────────────────────────────────────────

  const fileContextItems = useMemo<ContextMenuItem[]>(() => {
    const file = fileContextMenu?.file;
    if (!file) {
      return [];
    }
    return [
      {
        key: 'preview',
        label: 'Preview',
        icon: <Eye className="h-4 w-4" />,
        onClick: () => {
          setFileContextMenu(null);
          setPreviewTarget(file);
        },
      },
      {
        key: 'download',
        label: 'Download',
        icon: <Download className="h-4 w-4" />,
        onClick: () => {
          setFileContextMenu(null);
          void fileMutations.downloadFile(file);
        },
      },
      {
        key: 'rename',
        label: 'Rename',
        icon: <Pencil className="h-4 w-4" />,
        onClick: () => {
          setFileContextMenu(null);
          setRenameFileTarget(file);
        },
      },
      {
        key: 'move',
        label: 'Move to…',
        icon: <FolderInput className="h-4 w-4" />,
        onClick: () => {
          setFileContextMenu(null);
          setMoveTarget(file);
        },
      },
      {
        key: 'share',
        label: 'Share',
        icon: <Share2 className="h-4 w-4" />,
        onClick: () => {
          setFileContextMenu(null);
          setShareTarget(file);
        },
      },
      {
        key: 'favorite',
        label: file.isFavorite ? 'Remove from favorites' : 'Add to favorites',
        icon: <Star className="h-4 w-4" />,
        onClick: () => {
          setFileContextMenu(null);
          fileMutations.toggleFavorite.mutate({ id: file.id, favorite: !file.isFavorite });
        },
      },
      {
        key: 'copy-link',
        label: 'Copy link',
        icon: <Link2 className="h-4 w-4" />,
        onClick: () => {
          void handleCopyLink(file);
        },
      },
      {
        key: 'details',
        label: 'Details',
        icon: <Info className="h-4 w-4" />,
        onClick: () => {
          setFileContextMenu(null);
          selectOnlyFiles([file.id]);
          setDetailsFile(file);
        },
      },
      { key: 'separator', label: '', separator: true },
      {
        key: 'delete',
        label: 'Delete',
        icon: <Trash2 className="h-4 w-4" />,
        danger: true,
        onClick: () => {
          setFileContextMenu(null);
          setDeleteFileTarget(file);
        },
      },
    ];
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fileContextMenu]);

  const folderEmpty = visibleFolders.length === 0;
  const fileEmpty = visibleFiles.length === 0;

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
        resultCount={visibleFolders.length + visibleFiles.length}
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
      ) : folderEmpty && fileEmpty ? (
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
      ) : (
        <div className="space-y-6">
          {/* Sub-folders at the current location */}
          {visibleFolders.length > 0 && (
            <section aria-label="Folders" className="space-y-2">
              <h2 className="text-xs font-semibold tracking-wide text-gray-400 uppercase dark:text-gray-500">
                Folders
              </h2>
              {viewMode === 'grid' ? (
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
            </section>
          )}

          {/* Files inside the current folder */}
          {filesLoading ? (
            viewMode === 'grid' ? (
              <FileGridSkeleton />
            ) : (
              <FileTableSkeleton />
            )
          ) : visibleFiles.length > 0 ? (
            <section aria-label="Files" className="space-y-2">
              <h2 className="text-xs font-semibold tracking-wide text-gray-400 uppercase dark:text-gray-500">
                Files
              </h2>
              {viewMode === 'grid' ? (
                <FileGrid
                  files={visibleFiles}
                  selectedIds={[]}
                  ownerName={ownerName}
                  searchQuery={searchQuery}
                  onSelect={handleFileSelect}
                  onToggleFavorite={(file) =>
                    fileMutations.toggleFavorite.mutate({
                      id: file.id,
                      favorite: !file.isFavorite,
                    })
                  }
                  onDownload={(file) => void fileMutations.downloadFile(file)}
                  onOpenMenu={handleFileOpenMenu}
                  onPreview={setPreviewTarget}
                />
              ) : (
                <FileTable
                  files={visibleFiles}
                  selectedIds={[]}
                  sortKey={FOLDER_TO_FILE_SORT[sortKey]}
                  sortDirection={sortDirection}
                  onSortChange={(key) => handleSortChange(key === 'name' ? 'name' : 'date')}
                  ownerName={ownerName}
                  searchQuery={searchQuery}
                  onSelect={handleFileSelect}
                  onToggleFavorite={(file) =>
                    fileMutations.toggleFavorite.mutate({
                      id: file.id,
                      favorite: !file.isFavorite,
                    })
                  }
                  onOpenMenu={handleFileOpenMenu}
                  onPreview={setPreviewTarget}
                />
              )}
            </section>
          ) : (
            visibleFolders.length === 0 && (
              <FoldersEmptyState
                variant={folders.length === 0 ? 'no-folders' : 'no-search'}
                searchQuery={searchQuery}
                onCreateFolder={() => setNewFolderOpen(true)}
                onClearSearch={() => setSearchQuery('')}
              />
            )
          )}
        </div>
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

      {/* Folder context menu */}
      <FileContextMenu
        open={contextMenu !== null}
        position={contextMenu?.position ?? { x: 0, y: 0 }}
        items={contextItems}
        onClose={() => setContextMenu(null)}
      />

      {/* File context menu */}
      <FileContextMenu
        open={fileContextMenu !== null}
        position={fileContextMenu?.position ?? { x: 0, y: 0 }}
        items={fileContextItems}
        onClose={() => setFileContextMenu(null)}
      />

      {/* Dialogs */}
      <UploadModal
        open={uploadOpen}
        onClose={() => setUploadOpen(false)}
        folderId={currentFolderId}
        onUploadComplete={handleUploadComplete}
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
      <RenameDialog
        file={renameFileTarget}
        open={renameFileTarget !== null}
        onClose={() => setRenameFileTarget(null)}
        onConfirm={(file, originalFileName) => {
          fileMutations.renameFile.mutate(
            { id: file.id, originalFileName },
            { onSettled: () => setRenameFileTarget(null) },
          );
        }}
        isLoading={fileMutations.renameFile.isPending}
      />
      <MoveDialog
        file={moveTarget}
        open={moveTarget !== null}
        onClose={() => setMoveTarget(null)}
        onConfirm={handleMove}
        isLoading={fileMutations.moveFile.isPending}
      />
      <ShareDialog
        file={shareTarget}
        open={shareTarget !== null}
        onClose={() => setShareTarget(null)}
      />
      <DeleteDialog
        file={deleteFileTarget}
        open={deleteFileTarget !== null}
        onClose={() => setDeleteFileTarget(null)}
        onConfirm={handleDeleteFile}
        isLoading={fileMutations.deleteFile.isPending}
      />
      <PreviewModal
        file={previewTarget}
        open={previewTarget !== null}
        onClose={() => setPreviewTarget(null)}
        onDownload={(file) => void fileMutations.downloadFile(file)}
      />
      <DetailsPanel
        file={detailsFile}
        open={detailsFile !== null}
        onClose={() => setDetailsFile(null)}
        ownerName={ownerName}
        onDownload={(file) => void fileMutations.downloadFile(file)}
        onPreview={setPreviewTarget}
        onShare={setShareTarget}
        onRename={setRenameFileTarget}
        onDelete={setDeleteFileTarget}
        onToggleFavorite={(file) =>
          fileMutations.toggleFavorite.mutate({ id: file.id, favorite: !file.isFavorite })
        }
      />
    </div>
  );
}
