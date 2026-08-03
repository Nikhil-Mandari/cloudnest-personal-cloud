import { useMemo, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import {
  CloudUpload,
  Download,
  FolderInput,
  Link2,
  Pencil,
  Share2,
  Star,
  Trash2,
  X,
} from 'lucide-react';
import { toast } from 'react-toastify';

import { ErrorState } from '@/components/common/ErrorState';
import { PageHeader } from '@/components/common/PageHeader';
import { DeleteDialog } from '@/components/files/DeleteDialog';
import { FilesEmptyState } from '@/components/files/EmptyState';
import {
  FileContextMenu,
  type ContextMenuItem,
  type ContextMenuPosition,
} from '@/components/files/FileContextMenu';
import { FileGrid } from '@/components/files/FileGrid';
import { FileGridSkeleton, FileTableSkeleton } from '@/components/files/FileSkeletons';
import { FileTable } from '@/components/files/FileTable';
import { FileToolbar } from '@/components/files/FileToolbar';
import { MoveDialog } from '@/components/files/MoveDialog';
import { RenameDialog } from '@/components/files/RenameDialog';
import { ShareDialog } from '@/components/files/ShareDialog';
import { UploadModal } from '@/components/files/UploadModal';
import { useAuth } from '@/hooks/useAuth';
import { useFileMutations, useFilesQuery } from '@/hooks/useFiles';
import { shareService } from '@/services/share.service';
import { useFilesStore } from '@/store/filesStore';
import type { FileItem, FileTypeFilter, SortKey } from '@/types';
import { copyToClipboard } from '@/utils/download';
import { getErrorMessage } from '@/utils/error';
import { buildShareUrl, filterFiles, sortFiles } from '@/utils/file';

const FILTER_LABELS: Record<FileTypeFilter, string> = {
  all: 'matching',
  favorites: 'favorite',
  image: 'image',
  video: 'video',
  audio: 'audio',
  document: 'document',
  archive: 'archive',
  code: 'code',
  other: 'other',
};

export function FilesPage() {
  const { user } = useAuth();
  const { data: files = [], isLoading, isError, error, refetch } = useFilesQuery();
  const mutations = useFileMutations();

  // Explorer UI state (zustand).
  const viewMode = useFilesStore((state) => state.viewMode);
  const sortKey = useFilesStore((state) => state.sortKey);
  const sortDirection = useFilesStore((state) => state.sortDirection);
  const filter = useFilesStore((state) => state.filter);
  const searchQuery = useFilesStore((state) => state.searchQuery);
  const selectedIds = useFilesStore((state) => state.selectedIds);
  const setSortKey = useFilesStore((state) => state.setSortKey);
  const setSortDirection = useFilesStore((state) => state.setSortDirection);
  const toggleSelect = useFilesStore((state) => state.toggleSelect);
  const selectOnly = useFilesStore((state) => state.selectOnly);
  const clearSelection = useFilesStore((state) => state.clearSelection);
  const setFilter = useFilesStore((state) => state.setFilter);
  const setSearchQuery = useFilesStore((state) => state.setSearchQuery);

  // Local dialog / overlay state.
  const [uploadOpen, setUploadOpen] = useState(false);
  const [pendingFiles, setPendingFiles] = useState<File[]>([]);
  const [contextMenu, setContextMenu] = useState<{
    file: FileItem;
    position: ContextMenuPosition;
  } | null>(null);
  const [renameTarget, setRenameTarget] = useState<FileItem | null>(null);
  const [moveTarget, setMoveTarget] = useState<FileItem | null>(null);
  const [shareTarget, setShareTarget] = useState<FileItem | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<FileItem | null>(null);
  const [isDraggingOver, setIsDraggingOver] = useState(false);
  const dragDepth = useRef(0);

  const ownerName = user?.fullName ?? 'You';

  const visibleFiles = useMemo(
    () => sortFiles(filterFiles(files, searchQuery, filter), sortKey, sortDirection),
    [files, searchQuery, filter, sortKey, sortDirection],
  );

  // ── Handlers ──────────────────────────────────────────────────────────────

  const handleOpenMenu = (file: FileItem, x: number, y: number) => {
    if (!selectedIds.includes(file.id)) {
      selectOnly([file.id]);
    }
    setContextMenu({ file, position: { x, y } });
  };

  const handleSortChange = (key: SortKey) => {
    if (key === sortKey) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortKey(key);
      setSortDirection(key === 'date' ? 'desc' : 'asc');
    }
  };

  const handleClearFilters = () => {
    setFilter('all');
    setSearchQuery('');
  };

  const handleRename = (file: FileItem, originalFileName: string) => {
    mutations.renameFile.mutate(
      { id: file.id, originalFileName },
      { onSettled: () => setRenameTarget(null) },
    );
  };

  const handleMove = (file: FileItem, folderId: string | null) => {
    mutations.moveFile.mutate({ id: file.id, folderId }, { onSettled: () => setMoveTarget(null) });
  };

  const handleDelete = (file: FileItem) => {
    clearSelection();
    mutations.deleteFile.mutate(file.id, { onSettled: () => setDeleteTarget(null) });
  };

  const handleCopyLink = async (file: FileItem) => {
    setContextMenu(null);
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

  // ── Context menu items ────────────────────────────────────────────────────

  const contextItems = useMemo<ContextMenuItem[]>(() => {
    const file = contextMenu?.file;
    if (!file) {
      return [];
    }
    return [
      {
        key: 'download',
        label: 'Download',
        icon: <Download className="h-4 w-4" />,
        onClick: () => {
          setContextMenu(null);
          void mutations.downloadFile(file);
        },
      },
      {
        key: 'rename',
        label: 'Rename',
        icon: <Pencil className="h-4 w-4" />,
        onClick: () => {
          setContextMenu(null);
          setRenameTarget(file);
        },
      },
      {
        key: 'move',
        label: 'Move to…',
        icon: <FolderInput className="h-4 w-4" />,
        onClick: () => {
          setContextMenu(null);
          setMoveTarget(file);
        },
      },
      {
        key: 'share',
        label: 'Share',
        icon: <Share2 className="h-4 w-4" />,
        onClick: () => {
          setContextMenu(null);
          setShareTarget(file);
        },
      },
      {
        key: 'favorite',
        label: file.isFavorite ? 'Remove from favorites' : 'Add to favorites',
        icon: <Star className="h-4 w-4" />,
        onClick: () => {
          setContextMenu(null);
          mutations.toggleFavorite.mutate({ id: file.id, favorite: !file.isFavorite });
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
      { key: 'separator', label: '', separator: true },
      {
        key: 'delete',
        label: 'Delete',
        icon: <Trash2 className="h-4 w-4" />,
        danger: true,
        onClick: () => {
          setContextMenu(null);
          setDeleteTarget(file);
        },
      },
    ];
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [contextMenu]);

  // ── Drag & drop (page level) ──────────────────────────────────────────────

  const handlePageDragEnter = (event: React.DragEvent) => {
    event.preventDefault();
    dragDepth.current += 1;
    setIsDraggingOver(true);
  };

  const handlePageDragOver = (event: React.DragEvent) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'copy';
  };

  const handlePageDragLeave = () => {
    dragDepth.current = Math.max(0, dragDepth.current - 1);
    if (dragDepth.current === 0) {
      setIsDraggingOver(false);
    }
  };

  const handlePageDrop = (event: React.DragEvent) => {
    event.preventDefault();
    dragDepth.current = 0;
    setIsDraggingOver(false);
    const dropped = Array.from(event.dataTransfer.files ?? []);
    if (dropped.length > 0) {
      setPendingFiles(dropped);
      setUploadOpen(true);
    }
  };

  // ── Empty state variant ───────────────────────────────────────────────────

  const emptyVariant =
    files.length === 0
      ? 'no-files'
      : searchQuery.trim()
        ? 'no-search'
        : filter === 'favorites'
          ? 'no-favorites'
          : 'no-filter';

  return (
    <div
      className="space-y-6"
      onDragEnter={handlePageDragEnter}
      onDragOver={handlePageDragOver}
      onDragLeave={handlePageDragLeave}
      onDrop={handlePageDrop}
    >
      <PageHeader
        title="My Files"
        description="Upload, organise and manage your files in one place."
      />

      <FileToolbar
        files={files}
        resultCount={visibleFiles.length}
        onUpload={() => setUploadOpen(true)}
      />

      {/* Content: skeleton → error → empty → grid/list */}
      {isLoading ? (
        viewMode === 'grid' ? (
          <FileGridSkeleton />
        ) : (
          <FileTableSkeleton />
        )
      ) : isError ? (
        <ErrorState
          message={getErrorMessage(error, 'Failed to load your files.')}
          onRetry={() => void refetch()}
        />
      ) : visibleFiles.length === 0 ? (
        <FilesEmptyState
          variant={emptyVariant}
          searchQuery={searchQuery}
          filterLabel={FILTER_LABELS[filter]}
          onUpload={() => setUploadOpen(true)}
          onClearFilters={handleClearFilters}
        />
      ) : viewMode === 'grid' ? (
        <FileGrid
          files={visibleFiles}
          selectedIds={selectedIds}
          ownerName={ownerName}
          onSelect={toggleSelect}
          onToggleFavorite={(file) =>
            mutations.toggleFavorite.mutate({ id: file.id, favorite: !file.isFavorite })
          }
          onDownload={(file) => void mutations.downloadFile(file)}
          onOpenMenu={handleOpenMenu}
        />
      ) : (
        <FileTable
          files={visibleFiles}
          selectedIds={selectedIds}
          sortKey={sortKey}
          sortDirection={sortDirection}
          onSortChange={handleSortChange}
          ownerName={ownerName}
          onSelect={toggleSelect}
          onToggleFavorite={(file) =>
            mutations.toggleFavorite.mutate({ id: file.id, favorite: !file.isFavorite })
          }
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
              {selectedIds.length} file{selectedIds.length === 1 ? '' : 's'} selected
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

      {/* Page-level drop overlay */}
      <AnimatePresence>
        {isDraggingOver && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15 }}
            className="pointer-events-none fixed inset-0 z-40 flex items-center justify-center bg-gray-950/30 backdrop-blur-[2px] dark:bg-gray-950/60"
          >
            <motion.div
              initial={{ scale: 0.96 }}
              animate={{ scale: 1 }}
              exit={{ scale: 0.96 }}
              className="border-brand-400 flex flex-col items-center gap-3 rounded-2xl border-2 border-dashed bg-white/95 px-10 py-8 shadow-2xl dark:bg-gray-900/95"
            >
              <CloudUpload className="text-brand-500 h-10 w-10" />
              <p className="text-lg font-semibold text-gray-900 dark:text-white">
                Drop files to upload
              </p>
              <p className="text-sm text-gray-400 dark:text-gray-500">
                You can add multiple files at once
              </p>
            </motion.div>
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
        initialFiles={pendingFiles}
        onInitialFilesConsumed={() => setPendingFiles([])}
      />
      <RenameDialog
        file={renameTarget}
        open={renameTarget !== null}
        onClose={() => setRenameTarget(null)}
        onConfirm={handleRename}
        isLoading={mutations.renameFile.isPending}
      />
      <MoveDialog
        file={moveTarget}
        open={moveTarget !== null}
        onClose={() => setMoveTarget(null)}
        onConfirm={handleMove}
        isLoading={mutations.moveFile.isPending}
      />
      <ShareDialog
        file={shareTarget}
        open={shareTarget !== null}
        onClose={() => setShareTarget(null)}
      />
      <DeleteDialog
        file={deleteTarget}
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        isLoading={mutations.deleteFile.isPending}
      />
    </div>
  );
}
