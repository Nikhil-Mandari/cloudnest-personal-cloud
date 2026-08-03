import { useEffect, useRef } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { Upload } from 'lucide-react';

import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { useFileUpload } from '@/hooks/useFileUpload';
import { UploadDropzone } from './UploadDropzone';
import { UploadProgress } from './UploadProgress';

export interface UploadModalProps {
  open: boolean;
  onClose: () => void;
  /** Files dropped directly onto the page — queued automatically on open. */
  initialFiles?: File[];
  onInitialFilesConsumed?: () => void;
  folderId?: string | null;
}

/** Upload dialog: dropzone + live queue with progress, cancel and retry. */
export function UploadModal({
  open,
  onClose,
  initialFiles = [],
  onInitialFilesConsumed,
  folderId,
}: UploadModalProps) {
  const { tasks, addFiles, start, removeTask, cancelTask, retryTask, clearFinished } =
    useFileUpload({ folderId });

  // Queue files dropped on the page the first time the modal opens with them.
  const consumedRef = useRef(false);
  useEffect(() => {
    if (!open) {
      consumedRef.current = false;
      return;
    }
    if (!consumedRef.current && initialFiles.length > 0) {
      consumedRef.current = true;
      addFiles(initialFiles);
      onInitialFilesConsumed?.();
    }
  }, [open, initialFiles, addFiles, onInitialFilesConsumed]);

  const pendingCount = tasks.filter(
    (task) => task.status === 'queued' || task.status === 'uploading',
  ).length;

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Upload files"
      description="Drop files or pick them from your device. Uploads are private to your account."
      size="lg"
    >
      <div className="space-y-4">
        <UploadDropzone onFilesSelected={addFiles} />

        {tasks.length > 0 && (
          <>
            <div className="flex items-center justify-between">
              <p className="text-sm font-medium text-gray-700 dark:text-gray-200">
                {pendingCount > 0
                  ? `${pendingCount} file${pendingCount === 1 ? '' : 's'} in queue`
                  : 'Uploads finished'}
              </p>
              <button
                type="button"
                onClick={clearFinished}
                className="text-xs font-medium text-gray-400 transition-colors hover:text-gray-600 dark:hover:text-gray-200"
              >
                Clear completed
              </button>
            </div>

            <motion.ul layout className="max-h-72 space-y-2 overflow-y-auto pr-1">
              <AnimatePresence initial={false}>
                {tasks.map((task) => (
                  <UploadProgress
                    key={task.id}
                    task={task}
                    onCancel={cancelTask}
                    onRemove={removeTask}
                    onRetry={retryTask}
                  />
                ))}
              </AnimatePresence>
            </motion.ul>
          </>
        )}

        <div className="flex justify-end gap-3 border-t border-gray-100 pt-4 dark:border-gray-800">
          <Button variant="ghost" onClick={onClose}>
            Close
          </Button>
          <Button
            variant="primary"
            onClick={start}
            disabled={pendingCount === 0}
            leftIcon={<Upload className="h-4 w-4" />}
          >
            {pendingCount > 0 ? `Upload ${pendingCount}` : 'Upload'}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
