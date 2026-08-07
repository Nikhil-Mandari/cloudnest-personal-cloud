import { useState } from 'react';
import { CircleCheck, FolderInput, FolderOpen, Home } from 'lucide-react';

import { ErrorState } from '@/components/common/ErrorState';
import { Loader } from '@/components/common/Loader';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { useFoldersQuery } from '@/hooks/useFolders';
import type { FileItem } from '@/types';
import { cn } from '@/utils/cn';

export interface MoveDialogProps {
  file: FileItem | null;
  open: boolean;
  onClose: () => void;
  onConfirm: (file: FileItem, folderId: string | null) => void;
  isLoading?: boolean;
}

/** Move dialog — pick a destination folder (or the root) for the file. */
export function MoveDialog({ file, open, onClose, onConfirm, isLoading }: MoveDialogProps) {
  const { data: folders = [], isLoading: foldersLoading, isError, refetch } = useFoldersQuery();
  const [selectedFolderId, setSelectedFolderId] = useState<string | null>(null);

  // Reset the selection when the dialog opens for a (new) target file — adjust
  // state during render instead of in an effect (React recommended pattern).
  const [prevState, setPrevState] = useState<{ open: boolean; fileId: number | null }>({
    open: false,
    fileId: null,
  });
  const shouldReset = open && (prevState.open !== true || file?.id !== prevState.fileId);
  if (shouldReset) {
    setPrevState({ open: true, fileId: file?.id ?? null });
    setSelectedFolderId(file?.folderId ?? null);
  }

  const unchanged = file != null && selectedFolderId === file.folderId;

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Move file"
      description={file ? `Choose a destination for "${file.originalFileName}"` : undefined}
      size="sm"
    >
      {foldersLoading ? (
        <Loader label="Loading folders…" />
      ) : isError ? (
        <ErrorState message="Couldn't load your folders." onRetry={() => void refetch()} />
      ) : (
        <div className="max-h-80 space-y-1 overflow-y-auto">
          <button
            type="button"
            onClick={() => setSelectedFolderId(null)}
            aria-pressed={selectedFolderId === null}
            className={cn(
              'flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm transition-colors',
              selectedFolderId === null
                ? 'bg-brand-500/10 text-brand-700 dark:text-brand-300 font-medium'
                : 'text-gray-700 hover:bg-gray-100 dark:text-gray-200 dark:hover:bg-gray-800/70',
            )}
          >
            <Home className="h-4 w-4 shrink-0 text-gray-400" />
            <span className="flex-1 truncate">My Files (root)</span>
            {selectedFolderId === null && (
              <CircleCheck className="text-brand-600 h-4 w-4 shrink-0" />
            )}
          </button>

          {folders.map((folder) => {
            const isSelected = selectedFolderId === folder.id;
            return (
              <button
                key={folder.id}
                type="button"
                onClick={() => setSelectedFolderId(folder.id)}
                aria-pressed={isSelected}
                className={cn(
                  'flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm transition-colors',
                  isSelected
                    ? 'bg-brand-500/10 text-brand-700 dark:text-brand-300 font-medium'
                    : 'text-gray-700 hover:bg-gray-100 dark:text-gray-200 dark:hover:bg-gray-800/70',
                )}
              >
                <FolderOpen className="h-4 w-4 shrink-0 text-gray-400" />
                <span className="flex-1 truncate">{folder.name}</span>
                {isSelected && <CircleCheck className="text-brand-600 h-4 w-4 shrink-0" />}
              </button>
            );
          })}

          {folders.length === 0 && (
            <p className="px-3 py-6 text-center text-sm text-gray-400 dark:text-gray-500">
              You don't have any folders yet — files will be moved to the root.
            </p>
          )}
        </div>
      )}

      <div className="mt-4 flex justify-end gap-3 border-t border-gray-100 pt-4 dark:border-gray-800">
        <Button variant="outline" onClick={onClose} disabled={isLoading}>
          Cancel
        </Button>
        <Button
          variant="primary"
          onClick={() => file && onConfirm(file, selectedFolderId)}
          isLoading={isLoading}
          disabled={unchanged}
          leftIcon={<FolderInput className="h-4 w-4" />}
        >
          Move here
        </Button>
      </div>
    </Modal>
  );
}
