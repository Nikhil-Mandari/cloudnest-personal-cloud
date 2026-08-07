import { useEffect, useRef, useState } from 'react';
import { Pencil } from 'lucide-react';

import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import type { FileItem } from '@/types';
import { splitFileName } from '@/utils/file';

const INVALID_NAME_CHARS = '<>:"/\\|?*';
const MAX_NAME_LENGTH = 240;

/** Rejects forbidden characters and control characters. */
function hasInvalidNameChars(name: string): boolean {
  for (const char of name) {
    const code = char.codePointAt(0) ?? 0;
    if (code < 32 || INVALID_NAME_CHARS.includes(char)) {
      return true;
    }
  }
  return false;
}

export interface RenameDialogProps {
  file: FileItem | null;
  open: boolean;
  onClose: () => void;
  onConfirm: (file: FileItem, originalFileName: string) => void;
  isLoading?: boolean;
}

/** Rename dialog — the extension stays locked while the base name is edited. */
export function RenameDialog({ file, open, onClose, onConfirm, isLoading }: RenameDialogProps) {
  const [baseName, setBaseName] = useState('');
  const [error, setError] = useState<string | undefined>();
  const inputRef = useRef<HTMLInputElement>(null);

  // Reset the form when the dialog opens for a (new) target file — adjust
  // state during render instead of in an effect (React recommended pattern).
  const [prevState, setPrevState] = useState<{ open: boolean; fileId: number | null }>({
    open: false,
    fileId: null,
  });
  const shouldReset = open && (prevState.open !== true || file?.id !== prevState.fileId);
  if (shouldReset) {
    setPrevState({ open: true, fileId: file?.id ?? null });
    setBaseName(file ? splitFileName(file.originalFileName).base : '');
    setError(undefined);
  }

  // Focus after the modal has mounted so the panel doesn't steal focus.
  useEffect(() => {
    if (open) {
      window.setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [open]);

  const ext = file ? splitFileName(file.originalFileName).ext : '';

  const handleSubmit = () => {
    const trimmed = baseName.trim();
    if (!trimmed) {
      setError('File name cannot be empty.');
      return;
    }
    if (trimmed.length > MAX_NAME_LENGTH) {
      setError(`File name is too long (max ${MAX_NAME_LENGTH} characters).`);
      return;
    }
    if (hasInvalidNameChars(trimmed)) {
      setError('Name contains invalid characters.');
      return;
    }
    if (file) {
      onConfirm(file, ext ? `${trimmed}.${ext}` : trimmed);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Rename file"
      description={file ? `Rename "${file.originalFileName}"` : undefined}
      size="sm"
    >
      <div className="space-y-4">
        <Input
          ref={inputRef}
          label="File name"
          value={baseName}
          onChange={(event) => {
            setBaseName(event.target.value);
            setError(undefined);
          }}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              handleSubmit();
            }
          }}
          error={error}
          hint={ext ? `Extension “.${ext}” is kept automatically.` : undefined}
          maxLength={MAX_NAME_LENGTH}
          rightIcon={ext ? <span className="text-sm text-gray-400">.{ext}</span> : undefined}
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
