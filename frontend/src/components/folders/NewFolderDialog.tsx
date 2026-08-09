import { useEffect, useRef, useState } from 'react';
import { FolderPlus } from 'lucide-react';

import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { validateFolderName } from '@/utils/folder';

export interface NewFolderDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: (name: string) => void;
  isLoading?: boolean;
}

/** Modal that collects the name of a new (root-level) folder. */
export function NewFolderDialog({ open, onClose, onConfirm, isLoading }: NewFolderDialogProps) {
  const [name, setName] = useState('');
  const [error, setError] = useState<string | undefined>();
  const inputRef = useRef<HTMLInputElement>(null);

  // Reset the form each time the dialog opens — adjust state during render
  // instead of in an effect (React recommended pattern).
  const [prevOpen, setPrevOpen] = useState(false);
  if (open && !prevOpen) {
    setPrevOpen(true);
    setName('');
    setError(undefined);
  } else if (!open && prevOpen) {
    setPrevOpen(false);
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
    onConfirm(name.trim());
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="New folder"
      description="Create a folder to organise your files."
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
          placeholder="e.g. Vacation photos"
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
            leftIcon={<FolderPlus className="h-4 w-4" />}
          >
            Create folder
          </Button>
        </div>
      </div>
    </Modal>
  );
}
