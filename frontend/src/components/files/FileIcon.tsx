import type { LucideIcon } from 'lucide-react';
import {
  File,
  FileArchive,
  FileAudio,
  FileCode,
  FileImage,
  FileText,
  FileVideo,
} from 'lucide-react';

import type { FileItem, FileTypeCategory } from '@/types';
import { cn } from '@/utils/cn';
import { getFileExtension, getFileTypeCategory } from '@/utils/file';

const CATEGORY_META: Record<FileTypeCategory, { icon: LucideIcon; className: string }> = {
  image: { icon: FileImage, className: 'bg-sky-500/10 text-sky-600 dark:text-sky-400' },
  video: { icon: FileVideo, className: 'bg-rose-500/10 text-rose-600 dark:text-rose-400' },
  audio: { icon: FileAudio, className: 'bg-violet-500/10 text-violet-600 dark:text-violet-400' },
  document: { icon: FileText, className: 'bg-blue-500/10 text-blue-600 dark:text-blue-400' },
  archive: { icon: FileArchive, className: 'bg-amber-500/10 text-amber-600 dark:text-amber-400' },
  code: { icon: FileCode, className: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400' },
  other: { icon: File, className: 'bg-gray-500/10 text-gray-600 dark:text-gray-400' },
};

export interface FileIconProps {
  file: Pick<FileItem, 'fileType' | 'originalFileName'>;
  size?: 'sm' | 'md' | 'lg';
  /** Renders the file extension (e.g. "PDF") inside the tile instead of an icon. */
  showExtension?: boolean;
  className?: string;
}

const tileClasses = {
  sm: 'h-8 w-8 rounded-lg',
  md: 'h-10 w-10 rounded-xl',
  lg: 'h-14 w-14 rounded-2xl',
};

const iconClasses = {
  sm: 'h-4 w-4',
  md: 'h-5 w-5',
  lg: 'h-7 w-7',
};

const extensionClasses = {
  sm: 'text-[8px]',
  md: 'text-[9px]',
  lg: 'text-[11px]',
};

/** A color-coded, type-aware file tile used across cards, rows and uploads. */
export function FileIcon({ file, size = 'md', showExtension = false, className }: FileIconProps) {
  const category = getFileTypeCategory(file);
  const { icon: Icon, className: categoryClassName } = CATEGORY_META[category];
  const ext = getFileExtension(file.originalFileName);

  return (
    <span
      aria-hidden="true"
      className={cn(
        'grid shrink-0 place-items-center overflow-hidden',
        categoryClassName,
        tileClasses[size],
        className,
      )}
    >
      {showExtension && ext ? (
        <span className={cn('font-bold tracking-wider uppercase', extensionClasses[size])}>
          {ext.slice(0, 4)}
        </span>
      ) : (
        <Icon className={iconClasses[size]} />
      )}
    </span>
  );
}
