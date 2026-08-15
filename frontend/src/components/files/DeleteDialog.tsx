import { ConfirmationDialog } from '@/components/ui/ConfirmationDialog';
import type { FileItem } from '@/types';

export interface DeleteDialogProps {
  file: FileItem | null;
  open: boolean;
  onClose: () => void;
  onConfirm: (file: FileItem) => void;
  isLoading?: boolean;
}

/** Confirmation dialog shown before moving a file to the trash. */
export function DeleteDialog({ file, open, onClose, onConfirm, isLoading }: DeleteDialogProps) {
  return (
    <ConfirmationDialog
      open={open}
      onClose={onClose}
      onConfirm={() => file && onConfirm(file)}
      title={file ? `Move “${file.originalFileName}” to trash?` : 'Move file to trash?'}
      description="The file stays in your trash, where you can restore it or delete it permanently."
      confirmLabel="Move to trash"
      variant="danger"
      isLoading={isLoading}
    />
  );
}
