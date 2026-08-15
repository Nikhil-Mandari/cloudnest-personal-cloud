import { useMemo, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { RotateCcw, Trash2, X } from 'lucide-react';
import { toast } from 'react-toastify';

import { ErrorState } from '@/components/common/ErrorState';
import { PageHeader } from '@/components/common/PageHeader';
import { TrashEmptyState } from '@/components/trash/TrashEmptyState';
import { TrashSkeletons } from '@/components/trash/TrashSkeletons';
import { TrashTable } from '@/components/trash/TrashTable';
import { TrashToolbar } from '@/components/trash/TrashToolbar';
import { Button } from '@/components/ui/Button';
import { ConfirmationDialog } from '@/components/ui/ConfirmationDialog';
import { useTrashMutations, useTrashQuery } from '@/hooks/useTrash';
import type { SortDirection } from '@/types';
import { getErrorMessage } from '@/utils/error';
import {
  buildTrashEntries,
  filterTrashEntries,
  sortTrashEntries,
  type TrashEntry,
  type TrashSortKey,
} from '@/utils/trash';

export function TrashPage() {
  const { data, isLoading, isError, error, refetch } = useTrashQuery();
  const mutations = useTrashMutations();

  // Explorer state (local — transient view).
  const [searchQuery, setSearchQuery] = useState('');
  const [sortKey, setSortKey] = useState<TrashSortKey>('date');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');
  const [selectedKeys, setSelectedKeys] = useState<ReadonlySet<string>>(new Set());

  // Dialogs.
  const [deleteTarget, setDeleteTarget] = useState<TrashEntry | null>(null);
  const [batchDeleteOpen, setBatchDeleteOpen] = useState(false);
  const [emptyTrashOpen, setEmptyTrashOpen] = useState(false);

  const entries = useMemo(() => buildTrashEntries(data ?? { files: [], folders: [] }), [data]);
  const visibleEntries = useMemo(
    () => sortTrashEntries(filterTrashEntries(entries, searchQuery), sortKey, sortDirection),
    [entries, searchQuery, sortKey, sortDirection],
  );

  const isMutating =
    mutations.restoreFile.isPending ||
    mutations.permanentDeleteFile.isPending ||
    mutations.restoreFolder.isPending ||
    mutations.permanentDeleteFolder.isPending;

  // ── Selection helpers ─────────────────────────────────────────────────────

  const toggleSelect = (key: string) => {
    setSelectedKeys((prev) => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  };

  const toggleSelectAll = () => {
    setSelectedKeys((prev) => {
      const allVisibleSelected = visibleEntries.every((entry) => prev.has(entry.key));
      if (allVisibleSelected) {
        return new Set([...prev].filter((key) => !visibleEntries.some((e) => e.key === key)));
      }
      return new Set([...prev, ...visibleEntries.map((entry) => entry.key)]);
    });
  };

  const clearSelection = () => setSelectedKeys(new Set());

  // ── Actions ───────────────────────────────────────────────────────────────

  const handleSortChange = (key: TrashSortKey) => {
    if (key === sortKey) {
      setSortDirection((dir) => (dir === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDirection(key === 'date' ? 'desc' : 'asc');
    }
  };

  const restoreEntry = (entry: TrashEntry) => {
    setSelectedKeys((prev) => {
      const next = new Set(prev);
      next.delete(entry.key);
      return next;
    });
    if (entry.kind === 'file' && entry.file) {
      mutations.restoreFile.mutate({ id: entry.file.id });
    } else if (entry.folder) {
      mutations.restoreFolder.mutate({ id: entry.folder.id });
    }
  };

  const restoreSelected = () => {
    const targets = entries.filter((entry) => selectedKeys.has(entry.key));
    targets.forEach((entry) => {
      if (entry.kind === 'file' && entry.file) {
        mutations.restoreFile.mutate({ id: entry.file.id, quiet: true });
      } else if (entry.folder) {
        mutations.restoreFolder.mutate({ id: entry.folder.id, quiet: true });
      }
    });
    if (targets.length > 0) {
      toast.success(`Restored ${targets.length} item${targets.length === 1 ? '' : 's'}`);
    }
    clearSelection();
  };

  const restoreAll = () => {
    entries.forEach((entry) => {
      if (entry.kind === 'file' && entry.file) {
        mutations.restoreFile.mutate({ id: entry.file.id, quiet: true });
      } else if (entry.folder) {
        mutations.restoreFolder.mutate({ id: entry.folder.id, quiet: true });
      }
    });
    if (entries.length > 0) {
      toast.success(`Restored all ${entries.length} item${entries.length === 1 ? '' : 's'}`);
    }
    clearSelection();
  };

  const permanentDeleteEntry = (entry: TrashEntry) => {
    if (entry.kind === 'file' && entry.file) {
      mutations.permanentDeleteFile.mutate({ id: entry.file.id });
    } else if (entry.folder) {
      mutations.permanentDeleteFolder.mutate({ id: entry.folder.id });
    }
  };

  const confirmDeleteTarget = () => {
    if (deleteTarget) {
      permanentDeleteEntry(deleteTarget);
      setSelectedKeys((prev) => {
        const next = new Set(prev);
        next.delete(deleteTarget.key);
        return next;
      });
      setDeleteTarget(null);
    }
  };

  const confirmBatchDelete = () => {
    const targets = entries.filter((entry) => selectedKeys.has(entry.key));
    targets.forEach((entry) => {
      if (entry.kind === 'file' && entry.file) {
        mutations.permanentDeleteFile.mutate({ id: entry.file.id, quiet: true });
      } else if (entry.folder) {
        mutations.permanentDeleteFolder.mutate({ id: entry.folder.id, quiet: true });
      }
    });
    if (targets.length > 0) {
      toast.success(`Permanently deleted ${targets.length} item${targets.length === 1 ? '' : 's'}`);
    }
    clearSelection();
    setBatchDeleteOpen(false);
  };

  const confirmEmptyTrash = () => {
    clearSelection();
    setEmptyTrashOpen(false);
    mutations.emptyTrash.mutate();
  };

  const selectedCount = selectedKeys.size;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Trash"
        description="Recover deleted files and folders, or empty your trash for good."
      />

      <TrashToolbar
        resultCount={visibleEntries.length}
        totalCount={entries.length}
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        sortKey={sortKey}
        sortDirection={sortDirection}
        onSortChange={handleSortChange}
        onSortDirectionChange={() =>
          setSortDirection((dir) => (dir === 'asc' ? 'desc' : 'asc'))
        }
        onRestoreAll={restoreAll}
        onEmptyTrash={() => setEmptyTrashOpen(true)}
        isRestoring={isMutating}
        isEmptying={mutations.emptyTrash.isPending}
      />

      {isLoading ? (
        <TrashSkeletons />
      ) : isError ? (
        <ErrorState
          message={getErrorMessage(error, 'Failed to load your trash.')}
          onRetry={() => void refetch()}
        />
      ) : entries.length === 0 ? (
        <TrashEmptyState variant="empty" />
      ) : visibleEntries.length === 0 ? (
        <TrashEmptyState variant="no-search" />
      ) : (
        <TrashTable
          entries={visibleEntries}
          selectedKeys={selectedKeys}
          sortKey={sortKey}
          sortDirection={sortDirection}
          onSortChange={handleSortChange}
          onToggleSelect={toggleSelect}
          onToggleSelectAll={toggleSelectAll}
          onRestore={restoreEntry}
          onDeleteForever={(entry) => setDeleteTarget(entry)}
        />
      )}

      {/* Selection bar */}
      <AnimatePresence>
        {selectedCount > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 12 }}
            transition={{ duration: 0.2, ease: 'easeOut' }}
            className="bg-brand-500/10 dark:bg-brand-500/15 border-brand-200 dark:border-brand-500/30 flex flex-wrap items-center justify-between gap-3 rounded-xl border px-4 py-2.5"
          >
            <p className="text-brand-700 dark:text-brand-300 text-sm font-medium">
              {selectedCount} item{selectedCount === 1 ? '' : 's'} selected
            </p>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                leftIcon={<RotateCcw className="h-3.5 w-3.5" />}
                disabled={isMutating}
                onClick={restoreSelected}
              >
                Restore
              </Button>
              <Button
                variant="outline"
                size="sm"
                leftIcon={<Trash2 className="h-3.5 w-3.5" />}
                disabled={isMutating}
                onClick={() => setBatchDeleteOpen(true)}
                className="text-rose-600 hover:bg-rose-50 hover:text-rose-700 dark:text-rose-400 dark:hover:bg-rose-500/10"
              >
                Delete forever
              </Button>
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

      {/* Dialogs */}
      <ConfirmationDialog
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={confirmDeleteTarget}
        title="Delete forever?"
        description={`“${deleteTarget?.name}” will be permanently deleted and cannot be recovered.`}
        confirmLabel="Delete forever"
        isLoading={isMutating}
      />
      <ConfirmationDialog
        open={batchDeleteOpen}
        onClose={() => setBatchDeleteOpen(false)}
        onConfirm={confirmBatchDelete}
        title={`Permanently delete ${selectedCount} item${selectedCount === 1 ? '' : 's'}?`}
        description="These items will be permanently deleted and cannot be recovered."
        confirmLabel="Delete forever"
        isLoading={isMutating}
      />
      <ConfirmationDialog
        open={emptyTrashOpen}
        onClose={() => setEmptyTrashOpen(false)}
        onConfirm={confirmEmptyTrash}
        title="Empty trash?"
        description={`All ${entries.length} item${entries.length === 1 ? '' : 's'} in your trash will be permanently deleted and cannot be recovered.`}
        confirmLabel="Empty trash"
        isLoading={mutations.emptyTrash.isPending}
      />
    </div>
  );
}
