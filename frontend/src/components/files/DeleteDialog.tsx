import { ConfirmationDialog } from '@/components/ui/ConfirmationDialog';
import type { FileItem } from '@/types';

export interface DeleteDialogProps {
  file: FileItem | null;
  open: boolean;
  onClose: () => void;
  onConfirm: (file: FileItem) => void;
  isLoading?: boolean;
}

/** Confirmation dialog shown before permanently deleting a file. */
export function DeleteDialog({ file, open, onClose, onConfirm, isLoading }: DeleteDialogProps) {
  return (
    <ConfirmationDialog
      open={open}
      onClose={onClose}
      onConfirm={() => file && onConfirm(file)}
      title={file ? `Delete “${file.originalFileName}”?` : 'Delete file?'}
      description="This permanently removes the file and its content from storage. This action cannot be undone."
      confirmLabel="Delete"
      variant="danger"
      isLoading={isLoading}
    />
  );
}
