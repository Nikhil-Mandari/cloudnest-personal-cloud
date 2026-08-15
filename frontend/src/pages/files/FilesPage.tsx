import { useEffect, useMemo, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import {
  Archive,
  ArrowLeft,
  CloudUpload,
  Download,
  Eye,
  FolderInput,
  FolderOpen,
  History,
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
import { PreviewModal } from '@/components/files/PreviewModal';
import { RenameDialog } from '@/components/files/RenameDialog';
import { ShareDialog } from '@/components/files/ShareDialog';
import { StatusBar } from '@/components/files/StatusBar';
import { UploadModal } from '@/components/files/UploadModal';
import { VersionHistoryDialog } from '@/components/files/VersionHistoryDialog';
import { FolderGrid } from '@/components/folders/FolderGrid';
import { Button } from '@/components/ui/Button';
import { useAuth } from '@/hooks/useAuth';
import { useExplorerKeyboard } from '@/hooks/useExplorerKeyboard';
import { useExplorerNavigation } from '@/hooks/useExplorerNavigation';
import { useFileMutations, useFilesQuery } from '@/hooks/useFiles';
import { useFolderContentsQuery } from '@/hooks/useFolders';
import { useMySharesQuery } from '@/hooks/useShare';
import { fileService } from '@/services/file.service';
import { shareService } from '@/services/share.service';
import type { ExplorerCrumb } from '@/store/explorerStore';
import { useFilesStore } from '@/store/filesStore';
import type { FileItem, FileTypeFilter, Folder, SortKey } from '@/types';
import { cn } from '@/utils/cn';
import { copyToClipboard } from '@/utils/download';
import { getErrorMessage } from '@/utils/error';
import { buildShareUrl, filterFiles, sortFiles } from '@/utils/file';

const FILTER_LABELS: Record<FileTypeFilter, string> = {
  all: 'matching',
  favorites: 'favorite',
  pdf: 'PDF',
  recent: 'recently uploaded',
  shared: 'shared',
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
  const { currentFolderId, trail, openFolder, goToCrumb, goUp, isResolving } =
    useExplorerNavigation();
  const { data: files = [], isLoading, isError, error, refetch } =
    useFilesQuery(currentFolderId);
  const { data: subfolders = [] } = useFolderContentsQuery(currentFolderId);
  const mutations = useFileMutations();

  // Explorer UI state (zustand).
  const viewMode = useFilesStore((state) => state.viewMode);
  const sortKey = useFilesStore((state) => state.sortKey);
  const sortDirection = useFilesStore((state) => state.sortDirection);
  const filter = useFilesStore((state) => state.filter);
  const searchQuery = useFilesStore((state) => state.searchQuery);
  const selectedIds = useFilesStore((state) => state.selectedIds);
  const detailsOpen = useFilesStore((state) => state.detailsOpen);
  const setSortKey = useFilesStore((state) => state.setSortKey);
  const setSortDirection = useFilesStore((state) => state.setSortDirection);
  const toggleSelect = useFilesStore((state) => state.toggleSelect);
  const selectOnly = useFilesStore((state) => state.selectOnly);
  const clearSelection = useFilesStore((state) => state.clearSelection);
  const setFilter = useFilesStore((state) => state.setFilter);
  const setSearchQuery = useFilesStore((state) => state.setSearchQuery);
  const setDetailsOpen = useFilesStore((state) => state.setDetailsOpen);

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
  const [previewTarget, setPreviewTarget] = useState<FileItem | null>(null);
  const [versionTarget, setVersionTarget] = useState<FileItem | null>(null);
  const [isDraggingOver, setIsDraggingOver] = useState(false);
  const dragDepth = useRef(0);

  // Freshly uploaded files — scrolled into view and pulsed for a few seconds.
  const [highlightIds, setHighlightIds] = useState<number[]>([]);

  const ownerName = user?.displayName ?? user?.username ?? 'You';

  const visibleFiles = useMemo(
    () => sortFiles(filterFiles(files, searchQuery, filter, sharedFileIds), sortKey, sortDirection),
    [files, searchQuery, filter, sharedFileIds, sortKey, sortDirection],
  );

  // Folders at the current location, narrowed by the active search term.
  const visibleFolders = useMemo(() => {
    const term = searchQuery.trim().toLowerCase();
    return term
      ? subfolders.filter((folder) => folder.name.toLowerCase().includes(term))
      : subfolders;
  }, [subfolders, searchQuery]);

  // Anchor of the last clicked file — enables Shift-click range selection.
  const lastSelectedRef = useRef<number | null>(null);

  // The anchor only makes sense within the current (filtered/sorted) list.
  useEffect(() => {
    lastSelectedRef.current = null;
  }, [files, searchQuery, filter, sortKey, sortDirection]);

  // The details panel shows the file when exactly one is selected.
  const detailsFile = useMemo(() => {
    if (!detailsOpen || selectedIds.length !== 1) {
      return null;
    }
    return visibleFiles.find((file) => file.id === selectedIds[0]) ?? null;
  }, [detailsOpen, selectedIds, visibleFiles]);

  // Scroll to the first freshly uploaded file and pulse it until the timer runs out.
  useEffect(() => {
    if (highlightIds.length === 0) {
      return;
    }
    const firstId = highlightIds[0];
    const element = document.querySelector<HTMLElement>(`[data-file-id="${firstId}"]`);
    element?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    const timer = window.setTimeout(() => setHighlightIds([]), 2800);
    return () => window.clearTimeout(timer);
  }, [highlightIds, files]);

  // ── Handlers ──────────────────────────────────────────────────────────────

  const handleOpenMenu = (file: FileItem, x: number, y: number) => {
    if (!selectedIds.includes(file.id)) {
      selectOnly([file.id]);
    }
    setContextMenu({ file, position: { x, y } });
  };

  /**
   * Dropbox-style selection: plain click selects one, Ctrl/Cmd-click toggles,
   * Shift-click selects the range from the last-clicked file to this one
   * (Ctrl/Cmd+Shift unions with the existing selection).
   */
  const handleFileSelect = (file: FileItem, event?: React.MouseEvent) => {
    const id = file.id;
    const anchorId = lastSelectedRef.current;
    const multi = event?.ctrlKey || event?.metaKey;

    if (event?.shiftKey && anchorId !== null && anchorId !== id) {
      const start = visibleFiles.findIndex((item) => item.id === anchorId);
      const end = visibleFiles.findIndex((item) => item.id === id);
      if (start !== -1 && end !== -1) {
        const [lo, hi] = start < end ? [start, end] : [end, start];
        const rangeIds = visibleFiles.slice(lo, hi + 1).map((item) => item.id);
        if (multi) {
          selectOnly(Array.from(new Set([...selectedIds, ...rangeIds])));
        } else {
          selectOnly(rangeIds);
        }
        lastSelectedRef.current = id;
        return;
      }
    }

    if (multi) {
      toggleSelect(id);
    } else {
      selectOnly([id]);
      // Single click also opens the details panel (Drive style).
      setDetailsOpen(true);
    }
    lastSelectedRef.current = id;
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

  // ── Folder navigation ──────────────────────────────────────────────────────

  /** Single click on a folder card → navigate into it. */
  const handleOpenFolder = (folder: Folder) => {
    clearSelection();
    setDetailsOpen(false);
    openFolder(folder);
  };

  /** Breadcrumb / back-navigation target. */
  const handleGoToCrumb = (crumb: ExplorerCrumb) => {
    clearSelection();
    setDetailsOpen(false);
    goToCrumb(crumb);
  };

  const handleGoUp = () => {
    clearSelection();
    setDetailsOpen(false);
    goUp();
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

  /** Freshly uploaded files → scroll to + pulse them, then refresh the list. */
  const handleUploadComplete = (ids: number[]) => {
    if (ids.length > 0) {
      setHighlightIds(ids);
    }
  };

  /** Downloads the selected files as a single ZIP archive (hierarchy kept). */
  const handleBulkDownload = async () => {
    const ids = Array.from(selectedIds);
    if (ids.length === 0) {
      return;
    }
    try {
      await fileService.downloadZip({ fileIds: ids });
      toast.success(`Downloading ${ids.length} file${ids.length === 1 ? '' : 's'} as ZIP…`);
    } catch (error) {
      toast.error(getErrorMessage(error, 'Failed to prepare the ZIP download'));
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
        key: 'preview',
        label: 'Preview',
        icon: <Eye className="h-4 w-4" />,
        onClick: () => {
          setContextMenu(null);
          setPreviewTarget(file);
        },
      },
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
      {
        key: 'versions',
        label: 'Version history',
        icon: <History className="h-4 w-4" />,
        onClick: () => {
          setContextMenu(null);
          setVersionTarget(file);
        },
      },
      {
        key: 'details',
        label: 'Details',
        icon: <Info className="h-4 w-4" />,
        onClick: () => {
          setContextMenu(null);
          selectOnly([file.id]);
          setDetailsOpen(true);
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

  // ── Keyboard shortcuts (arrows / Enter / Delete / F2 / Ctrl+A / Esc) ───────

  useExplorerKeyboard({
    enabled: !(
      uploadOpen ||
      renameTarget !== null ||
      moveTarget !== null ||
      shareTarget !== null ||
      deleteTarget !== null ||
      previewTarget !== null ||
      versionTarget !== null ||
      contextMenu !== null
    ),
    files: visibleFiles,
    selectedIds,
    onSelectFile: (file) => selectOnly([file.id]),
    onOpenPreview: setPreviewTarget,
    onDelete: setDeleteTarget,
    onRename: setRenameTarget,
    onSelectAll: () => selectOnly(visibleFiles.map((file) => file.id)),
    onEscape: () => {
      if (selectedIds.length > 0) {
        clearSelection();
      } else {
        setDetailsOpen(false);
      }
    },
    onGoUp: handleGoUp,
  });

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

      <div className="flex items-start gap-4">
        {/* Main explorer column */}
        <div className="min-w-0 flex-1 space-y-6">
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

          <FileToolbar
            files={files}
            sharedFileIds={sharedFileIds}
            resultCount={visibleFiles.length}
            detailsOpen={detailsOpen}
            onToggleDetails={() => setDetailsOpen(!detailsOpen)}
            onUpload={() => setUploadOpen(true)}
          />

          {/* Content: smooth cross-fade when the folder changes */}
          <AnimatePresence mode="wait" initial={false}>
            <motion.div
              key={currentFolderId ?? 'root'}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              transition={{ duration: 0.18, ease: 'easeOut' }}
              className="space-y-6"
            >
              {isLoading || isResolving ? (
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
              ) : visibleFiles.length === 0 && visibleFolders.length === 0 ? (
                currentFolderId && !searchQuery.trim() && filter === 'all' ? (
                  <div className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-gray-300 bg-white/60 px-6 py-14 text-center dark:border-gray-700 dark:bg-gray-900/40">
                    <div className="bg-brand-500/10 text-brand-500 grid h-14 w-14 place-items-center rounded-2xl">
                      <FolderOpen className="h-7 w-7" />
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-900 dark:text-white">
                        This folder is empty
                      </p>
                      <p className="mt-1 text-sm text-gray-400 dark:text-gray-500">
                        Upload files or create a sub-folder to get started.
                      </p>
                    </div>
                    <Button variant="primary" size="sm" onClick={() => setUploadOpen(true)}>
                      Upload files
                    </Button>
                  </div>
                ) : (
                  <FilesEmptyState
                    variant={emptyVariant}
                    searchQuery={searchQuery}
                    filterLabel={FILTER_LABELS[filter]}
                    onUpload={() => setUploadOpen(true)}
                    onClearFilters={handleClearFilters}
                  />
                )
              ) : (
                <>
                  {/* Sub-folders of the current location (hidden while filtering) */}
                  {filter === 'all' && visibleFolders.length > 0 && (
                    <section aria-label="Folders" className="space-y-2">
                      <h2 className="text-xs font-semibold tracking-wide text-gray-400 uppercase dark:text-gray-500">
                        Folders
                      </h2>
                      <FolderGrid
                        folders={visibleFolders}
                        selectedIds={[]}
                        ownerName={ownerName}
                        onOpen={handleOpenFolder}
                        onSelect={() => {}}
                        selectable={false}
                      />
                    </section>
                  )}

                  {visibleFiles.length === 0 ? (
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
                      searchQuery={searchQuery}
                      highlightedIds={highlightIds}
                      onSelect={handleFileSelect}
                      onToggleFavorite={(file) =>
                        mutations.toggleFavorite.mutate({ id: file.id, favorite: !file.isFavorite })
                      }
                      onDownload={(file) => void mutations.downloadFile(file)}
                      onOpenMenu={handleOpenMenu}
                      onPreview={setPreviewTarget}
                    />
                  ) : (
                    <FileTable
                      files={visibleFiles}
                      selectedIds={selectedIds}
                      sortKey={sortKey}
                      sortDirection={sortDirection}
                      onSortChange={handleSortChange}
                      ownerName={ownerName}
                      searchQuery={searchQuery}
                      highlightedIds={highlightIds}
                      compact={viewMode === 'compact'}
                      onSelect={handleFileSelect}
                      onToggleFavorite={(file) =>
                        mutations.toggleFavorite.mutate({ id: file.id, favorite: !file.isFavorite })
                      }
                      onOpenMenu={handleOpenMenu}
                      onPreview={setPreviewTarget}
                    />
                  )}
                </>
              )}
            </motion.div>
          </AnimatePresence>

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
                <div className="flex items-center gap-2">
                  <p className="text-brand-700 dark:text-brand-300 text-sm font-medium">
                    {selectedIds.length} file{selectedIds.length === 1 ? '' : 's'} selected
                  </p>
                  <button
                    type="button"
                    onClick={() => void handleBulkDownload()}
                    className="border-brand-200 dark:border-brand-500/30 hover:border-brand-300 flex items-center gap-1.5 rounded-lg border bg-white/60 px-2.5 py-1 text-xs font-medium text-gray-600 transition-colors hover:text-gray-900 dark:bg-gray-900/40 dark:text-gray-300 dark:hover:text-white"
                  >
                    <Archive className="h-3.5 w-3.5" /> Download ZIP
                  </button>
                  <button
                    type="button"
                    onClick={clearSelection}
                    className="flex items-center gap-1 text-sm text-gray-500 transition-colors hover:text-gray-900 dark:text-gray-400 dark:hover:text-white"
                  >
                    <X className="h-4 w-4" /> Clear
                  </button>
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Status bar (Windows Explorer / Finder style) */}
          <StatusBar
            itemCount={files.length}
            folderCount={subfolders.length}
            selectedCount={selectedIds.length}
          />
        </div>

        {/* Right-hand details panel */}
        <DetailsPanel
          file={detailsFile}
          open={detailsFile !== null}
          onClose={() => setDetailsOpen(false)}
          ownerName={ownerName}
          onDownload={(file) => void mutations.downloadFile(file)}
          onPreview={setPreviewTarget}
          onShare={setShareTarget}
          onRename={setRenameTarget}
          onDelete={setDeleteTarget}
          onToggleFavorite={(file) =>
            mutations.toggleFavorite.mutate({ id: file.id, favorite: !file.isFavorite })
          }
        />
      </div>

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
        onUploadComplete={handleUploadComplete}
        folderId={currentFolderId}
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
      <VersionHistoryDialog
        file={versionTarget}
        open={versionTarget !== null}
        onClose={() => setVersionTarget(null)}
      />
      <DeleteDialog
        file={deleteTarget}
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        isLoading={mutations.deleteFile.isPending}
      />
      <PreviewModal
        file={previewTarget}
        open={previewTarget !== null}
        onClose={() => setPreviewTarget(null)}
        onDownload={(file) => void mutations.downloadFile(file)}
      />
    </div>
  );
}
