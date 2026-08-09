import { ConfirmationDialog } from '@/components/ui/ConfirmationDialog';
import type { Folder } from '@/types';

export interface DeleteFolderDialogProps {
  folder: Folder | null;
  open: boolean;
  onClose: () => void;
  onConfirm: (folder: Folder) => void;
  isLoading?: boolean;
}

/** Confirmation dialog shown before deleting a folder. */
export function DeleteFolderDialog({
  folder,
  open,
  onClose,
  onConfirm,
  isLoading,
}: DeleteFolderDialogProps) {
  return (
    <ConfirmationDialog
      open={open}
      onClose={onClose}
      onConfirm={() => folder && onConfirm(folder)}
      title={folder ? `Delete “${folder.name}”?` : 'Delete folder?'}
      description="This deletes the folder and everything inside it, including any sub-folders. This action cannot be undone."
      confirmLabel="Delete"
      variant="danger"
      isLoading={isLoading}
    />
  );
}
