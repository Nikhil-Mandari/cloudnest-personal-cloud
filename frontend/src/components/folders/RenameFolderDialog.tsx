import { useEffect, useRef, useState } from 'react';
import { Pencil } from 'lucide-react';

import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import type { Folder } from '@/types';
import { validateFolderName } from '@/utils/folder';

export interface RenameFolderDialogProps {
  folder: Folder | null;
  open: boolean;
  onClose: () => void;
  onConfirm: (folder: Folder, name: string) => void;
  isLoading?: boolean;
}

/** Rename dialog for a folder. */
export function RenameFolderDialog({
  folder,
  open,
  onClose,
  onConfirm,
  isLoading,
}: RenameFolderDialogProps) {
  const [name, setName] = useState('');
  const [error, setError] = useState<string | undefined>();
  const inputRef = useRef<HTMLInputElement>(null);

  // Reset the form when the dialog opens for a (new) target folder — adjust
  // state during render instead of in an effect (React recommended pattern).
  const [prevState, setPrevState] = useState<{ open: boolean; folderId: string | null }>({
    open: false,
    folderId: null,
  });
  const shouldReset = open && (prevState.open !== true || folder?.id !== prevState.folderId);
  if (shouldReset) {
    setPrevState({ open: true, folderId: folder?.id ?? null });
    setName(folder?.name ?? '');
    setError(undefined);
  }

  // Focus after the modal has mounted so the panel doesn't steal focus.
  useEffect(() => {
    if (open) {
      window.setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [open]);

  const handleSubmit = () => {
    const validationError = validateFolderName(name);
    if (validationError) {
      setError(validationError);
      return;
    }
    if (folder) {
      onConfirm(folder, name.trim());
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Rename folder"
      description={folder ? `Rename "${folder.name}"` : undefined}
      size="sm"
    >
      <div className="space-y-4">
        <Input
          ref={inputRef}
          label="Folder name"
          value={name}
          onChange={(event) => {
            setName(event.target.value);
            setError(undefined);
          }}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              handleSubmit();
            }
          }}
          error={error}
          maxLength={255}
        />
        <div className="flex justify-end gap-3">
          <Button variant="outline" onClick={onClose} disabled={isLoading}>
            Cancel
          </Button>
          <Button
            variant="primary"
            onClick={handleSubmit}
            isLoading={isLoading}
            leftIcon={<Pencil className="h-4 w-4" />}
          >
            Rename
          </Button>
        </div>
      </div>
    </Modal>
  );
}
