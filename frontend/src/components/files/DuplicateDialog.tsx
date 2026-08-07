import { FileWarning, GitCompareArrows, Layers, ShieldCheck } from 'lucide-react';

import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import type { DuplicateAction, DuplicateFileInfo } from '@/types';
import { formatBytes } from '@/utils/format';

export interface DuplicateDialogProps {
  /** Name of the file being uploaded. */
  fileName: string;
  /** The existing file with identical content (SHA-256). */
  duplicateOf: DuplicateFileInfo;
  open: boolean;
  onResolve: (action: Exclude<DuplicateAction, 'ASK'>) => void;
  onDismiss: () => void;
}

const CHOICES: ReadonlyArray<{
  action: Exclude<DuplicateAction, 'ASK'>;
  label: string;
  description: string;
  icon: typeof Layers;
}> = [
  {
    action: 'KEEP_BOTH',
    label: 'Keep both',
    description: 'Upload anyway — you will have two copies of the same content.',
    icon: Layers,
  },
  {
    action: 'REPLACE',
    label: 'Replace',
    description: 'The existing file keeps its name and ID but gets this new content.',
    icon: GitCompareArrows,
  },
  {
    action: 'SKIP',
    label: 'Skip upload',
    description: 'Do nothing — the existing file stays untouched.',
    icon: ShieldCheck,
  },
] as const;

/**
 * Asks the user what to do when an upload has identical content (SHA-256) to
 * an existing file — the backend never stores duplicate metadata silently.
 */
export function DuplicateDialog({
  fileName,
  duplicateOf,
  open,
  onResolve,
  onDismiss,
}: DuplicateDialogProps) {
  return (
    <Modal
      open={open}
      onClose={onDismiss}
      title="Duplicate file detected"
      description={`“${fileName}” already exists with identical content.`}
      size="sm"
    >
      <div className="space-y-4">
        <div className="flex items-start gap-3 rounded-xl bg-amber-500/10 p-4">
          <FileWarning className="mt-0.5 h-5 w-5 shrink-0 text-amber-500" />
          <div className="text-sm">
            <p className="font-medium text-amber-800 dark:text-amber-200">
              Identical SHA-256 checksum
            </p>
            <p className="mt-0.5 text-xs text-amber-700/80 dark:text-amber-300/70">
              “{duplicateOf.originalFileName}” · {formatBytes(duplicateOf.fileSize)} · uploaded{' '}
              {duplicateOf.uploadedAt ? new Date(duplicateOf.uploadedAt).toLocaleDateString() : 'earlier'}
            </p>
          </div>
        </div>

        <div className="space-y-2">
          {CHOICES.map((choice) => {
            const Icon = choice.icon;
            return (
              <button
                key={choice.action}
                type="button"
                onClick={() => onResolve(choice.action)}
                className="group flex w-full items-start gap-3 rounded-xl border border-gray-200 bg-white p-3 text-left transition-all hover:border-brand-400 hover:bg-brand-500/5 dark:border-gray-700 dark:bg-gray-900 dark:hover:border-brand-500/60"
              >
                <span className="bg-brand-500/10 text-brand-600 dark:text-brand-400 grid h-9 w-9 shrink-0 place-items-center rounded-lg transition-colors group-hover:bg-brand-500/20">
                  <Icon className="h-5 w-5" />
                </span>
                <span>
                  <span className="block text-sm font-medium text-gray-900 dark:text-white">
                    {choice.label}
                  </span>
                  <span className="mt-0.5 block text-xs text-gray-500 dark:text-gray-400">
                    {choice.description}
                  </span>
                </span>
              </button>
            );
          })}
        </div>

        <div className="flex justify-end gap-3 border-t border-gray-100 pt-4 dark:border-gray-800">
          <Button variant="ghost" onClick={onDismiss}>
            Keep in queue
          </Button>
        </div>
      </div>
    </Modal>
  );
}
