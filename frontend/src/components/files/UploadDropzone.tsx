import { useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { CloudUpload } from 'lucide-react';

import { MAX_FILE_SIZE_MB } from '@/constants/files';
import { cn } from '@/utils/cn';

export interface UploadDropzoneProps {
  onFilesSelected: (files: File[]) => void;
  disabled?: boolean;
  compact?: boolean;
}

/** Click-to-browse / drag-and-drop zone used by the upload modal. */
export function UploadDropzone({
  onFilesSelected,
  disabled = false,
  compact = false,
}: UploadDropzoneProps) {
  const [isDragging, setIsDragging] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const dragDepth = useRef(0);

  const handleFiles = (list: FileList | null) => {
    if (!list || list.length === 0) {
      return;
    }
    onFilesSelected(Array.from(list));
  };

  return (
    <div
      role="button"
      tabIndex={disabled ? -1 : 0}
      aria-label="Upload files"
      onClick={() => inputRef.current?.click()}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          inputRef.current?.click();
        }
      }}
      onDragEnter={(event) => {
        event.preventDefault();
        if (!disabled) {
          dragDepth.current += 1;
          setIsDragging(true);
        }
      }}
      onDragOver={(event) => {
        event.preventDefault();
        if (!disabled) {
          setIsDragging(true);
        }
      }}
      onDragLeave={(event) => {
        event.preventDefault();
        dragDepth.current = Math.max(0, dragDepth.current - 1);
        if (dragDepth.current === 0) {
          setIsDragging(false);
        }
      }}
      onDrop={(event) => {
        event.preventDefault();
        dragDepth.current = 0;
        setIsDragging(false);
        if (!disabled) {
          handleFiles(event.dataTransfer.files);
        }
      }}
      className={cn(
        'relative grid cursor-pointer place-items-center rounded-2xl border-2 border-dashed transition-all duration-200',
        compact ? 'gap-2 px-4 py-6' : 'gap-3 px-6 py-12',
        isDragging
          ? 'border-brand-500 bg-brand-500/5'
          : 'hover:border-brand-400 hover:bg-brand-500/[0.03] border-gray-300 bg-gray-50/50 dark:border-gray-700 dark:bg-gray-950/50',
        disabled && 'pointer-events-none opacity-50',
      )}
    >
      <input
        ref={inputRef}
        type="file"
        multiple
        className="sr-only"
        onChange={(event) => {
          handleFiles(event.target.files);
          event.target.value = '';
        }}
      />

      <motion.div
        animate={isDragging ? { y: -4, scale: 1.08 } : { y: 0, scale: 1 }}
        transition={{ type: 'spring', stiffness: 400, damping: 22 }}
        className={cn(
          'bg-brand-500/10 text-brand-500 dark:text-brand-400 grid place-items-center',
          compact ? 'h-10 w-10 rounded-xl' : 'h-14 w-14 rounded-2xl',
        )}
      >
        <CloudUpload className={compact ? 'h-5 w-5' : 'h-7 w-7'} />
      </motion.div>

      <div className="text-center">
        <p
          className={cn(
            'font-medium text-gray-900 dark:text-white',
            compact ? 'text-sm' : 'text-base',
          )}
        >
          {isDragging ? 'Drop files to upload' : 'Drag & drop files here'}
        </p>
        <p className={cn('mt-1 text-gray-400 dark:text-gray-500', compact ? 'text-xs' : 'text-sm')}>
          or <span className="text-brand-600 dark:text-brand-400 font-medium">browse</span> from
          your device — up to {MAX_FILE_SIZE_MB} MB per file
        </p>
      </div>

      <AnimatePresence>
        {isDragging && (
          <motion.div
            className="ring-brand-500/60 pointer-events-none absolute inset-0 rounded-2xl ring-2"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
