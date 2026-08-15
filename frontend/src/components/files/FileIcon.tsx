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

/**
 * Extension → branded tile. The label is the brand mark shown on the tile
 * (office files use the classic single letters, everything else its
 * extension). Colors mirror the well-known format brands so users recognise
 * file types at a glance.
 */
interface BrandMeta {
  label: string;
  className: string;
}

const EXTENSION_BRANDS: Record<string, BrandMeta> = {
  // PDF
  pdf: { label: 'PDF', className: 'bg-rose-50 text-rose-600 dark:bg-rose-500/15 dark:text-rose-400' },
  // Microsoft Office
  doc: { label: 'W', className: 'bg-blue-50 text-blue-600 dark:bg-blue-500/15 dark:text-blue-400' },
  docx: { label: 'W', className: 'bg-blue-50 text-blue-600 dark:bg-blue-500/15 dark:text-blue-400' },
  rtf: { label: 'W', className: 'bg-blue-50 text-blue-600 dark:bg-blue-500/15 dark:text-blue-400' },
  xls: { label: 'X', className: 'bg-emerald-50 text-emerald-600 dark:bg-emerald-500/15 dark:text-emerald-400' },
  xlsx: { label: 'X', className: 'bg-emerald-50 text-emerald-600 dark:bg-emerald-500/15 dark:text-emerald-400' },
  csv: { label: 'X', className: 'bg-emerald-50 text-emerald-600 dark:bg-emerald-500/15 dark:text-emerald-400' },
  ppt: { label: 'P', className: 'bg-orange-50 text-orange-600 dark:bg-orange-500/15 dark:text-orange-400' },
  pptx: { label: 'P', className: 'bg-orange-50 text-orange-600 dark:bg-orange-500/15 dark:text-orange-400' },
  // Archives
  zip: { label: 'ZIP', className: 'bg-amber-50 text-amber-600 dark:bg-amber-500/15 dark:text-amber-400' },
  tar: { label: 'TAR', className: 'bg-amber-50 text-amber-600 dark:bg-amber-500/15 dark:text-amber-400' },
  gz: { label: 'GZ', className: 'bg-amber-50 text-amber-600 dark:bg-amber-500/15 dark:text-amber-400' },
  tgz: { label: 'TGZ', className: 'bg-amber-50 text-amber-600 dark:bg-amber-500/15 dark:text-amber-400' },
  rar: { label: 'RAR', className: 'bg-orange-100 text-orange-700 dark:bg-orange-500/20 dark:text-orange-300' },
  '7z': { label: '7Z', className: 'bg-gray-100 text-gray-600 dark:bg-gray-500/20 dark:text-gray-300' },
  bz2: { label: 'BZ2', className: 'bg-amber-50 text-amber-600 dark:bg-amber-500/15 dark:text-amber-400' },
  // Plain text / docs
  txt: { label: 'TXT', className: 'bg-slate-100 text-slate-600 dark:bg-slate-500/20 dark:text-slate-300' },
  md: { label: 'MD', className: 'bg-slate-100 text-slate-600 dark:bg-slate-500/20 dark:text-slate-300' },
  // Code languages
  js: { label: 'JS', className: 'bg-yellow-50 text-yellow-600 dark:bg-yellow-500/15 dark:text-yellow-400' },
  jsx: { label: 'JSX', className: 'bg-yellow-50 text-yellow-600 dark:bg-yellow-500/15 dark:text-yellow-400' },
  mjs: { label: 'JS', className: 'bg-yellow-50 text-yellow-600 dark:bg-yellow-500/15 dark:text-yellow-400' },
  ts: { label: 'TS', className: 'bg-sky-50 text-sky-600 dark:bg-sky-500/15 dark:text-sky-400' },
  tsx: { label: 'TSX', className: 'bg-sky-50 text-sky-600 dark:bg-sky-500/15 dark:text-sky-400' },
  html: { label: 'HTML', className: 'bg-orange-100 text-orange-600 dark:bg-orange-500/20 dark:text-orange-300' },
  css: { label: 'CSS', className: 'bg-blue-100 text-blue-600 dark:bg-blue-500/20 dark:text-blue-300' },
  scss: { label: 'SCSS', className: 'bg-blue-100 text-blue-600 dark:bg-blue-500/20 dark:text-blue-300' },
  json: { label: 'JSON', className: 'bg-lime-50 text-lime-600 dark:bg-lime-500/15 dark:text-lime-400' },
  xml: { label: 'XML', className: 'bg-teal-50 text-teal-600 dark:bg-teal-500/15 dark:text-teal-400' },
  yml: { label: 'YML', className: 'bg-teal-50 text-teal-600 dark:bg-teal-500/15 dark:text-teal-400' },
  yaml: { label: 'YAML', className: 'bg-teal-50 text-teal-600 dark:bg-teal-500/15 dark:text-teal-400' },
  sql: { label: 'SQL', className: 'bg-indigo-50 text-indigo-600 dark:bg-indigo-500/15 dark:text-indigo-400' },
  java: { label: 'JAVA', className: 'bg-red-100 text-red-600 dark:bg-red-500/20 dark:text-red-300' },
  py: { label: 'PY', className: 'bg-blue-50 text-blue-700 dark:bg-blue-500/15 dark:text-blue-400' },
  sh: { label: 'SH', className: 'bg-emerald-50 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-400' },
  bash: { label: 'SH', className: 'bg-emerald-50 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-400' },
  php: { label: 'PHP', className: 'bg-indigo-100 text-indigo-700 dark:bg-indigo-500/20 dark:text-indigo-300' },
  go: { label: 'GO', className: 'bg-cyan-50 text-cyan-600 dark:bg-cyan-500/15 dark:text-cyan-400' },
  rs: { label: 'RS', className: 'bg-orange-50 text-orange-700 dark:bg-orange-500/15 dark:text-orange-400' },
  rb: { label: 'RB', className: 'bg-rose-100 text-rose-600 dark:bg-rose-500/20 dark:text-rose-300' },
  c: { label: 'C', className: 'bg-gray-100 text-gray-700 dark:bg-gray-500/20 dark:text-gray-300' },
  cpp: { label: 'CPP', className: 'bg-gray-100 text-gray-700 dark:bg-gray-500/20 dark:text-gray-300' },
  cs: { label: 'CS', className: 'bg-violet-50 text-violet-600 dark:bg-violet-500/15 dark:text-violet-400' },
  kt: { label: 'KT', className: 'bg-orange-100 text-orange-700 dark:bg-orange-500/20 dark:text-orange-300' },
  swift: { label: 'SWIFT', className: 'bg-orange-50 text-orange-600 dark:bg-orange-500/15 dark:text-orange-400' },
  // Media
  mp3: { label: 'MP3', className: 'bg-violet-50 text-violet-600 dark:bg-violet-500/15 dark:text-violet-400' },
  wav: { label: 'WAV', className: 'bg-violet-50 text-violet-600 dark:bg-violet-500/15 dark:text-violet-400' },
  flac: { label: 'FLAC', className: 'bg-violet-50 text-violet-600 dark:bg-violet-500/15 dark:text-violet-400' },
  m4a: { label: 'M4A', className: 'bg-violet-50 text-violet-600 dark:bg-violet-500/15 dark:text-violet-400' },
  ogg: { label: 'OGG', className: 'bg-violet-50 text-violet-600 dark:bg-violet-500/15 dark:text-violet-400' },
  mp4: { label: 'MP4', className: 'bg-rose-50 text-rose-600 dark:bg-rose-500/15 dark:text-rose-400' },
  mov: { label: 'MOV', className: 'bg-rose-50 text-rose-600 dark:bg-rose-500/15 dark:text-rose-400' },
  webm: { label: 'WEBM', className: 'bg-rose-50 text-rose-600 dark:bg-rose-500/15 dark:text-rose-400' },
  mkv: { label: 'MKV', className: 'bg-rose-50 text-rose-600 dark:bg-rose-500/15 dark:text-rose-400' },
  avi: { label: 'AVI', className: 'bg-rose-50 text-rose-600 dark:bg-rose-500/15 dark:text-rose-400' },
  // Images (fallback tiles when no thumbnail is available)
  png: { label: 'PNG', className: 'bg-sky-50 text-sky-600 dark:bg-sky-500/15 dark:text-sky-400' },
  jpg: { label: 'JPG', className: 'bg-sky-50 text-sky-600 dark:bg-sky-500/15 dark:text-sky-400' },
  jpeg: { label: 'JPEG', className: 'bg-sky-50 text-sky-600 dark:bg-sky-500/15 dark:text-sky-400' },
  gif: { label: 'GIF', className: 'bg-sky-50 text-sky-600 dark:bg-sky-500/15 dark:text-sky-400' },
  webp: { label: 'WEBP', className: 'bg-sky-50 text-sky-600 dark:bg-sky-500/15 dark:text-sky-400' },
  svg: { label: 'SVG', className: 'bg-sky-50 text-sky-600 dark:bg-sky-500/15 dark:text-sky-400' },
  bmp: { label: 'BMP', className: 'bg-sky-50 text-sky-600 dark:bg-sky-500/15 dark:text-sky-400' },
};

export interface FileIconProps {
  file: Pick<FileItem, 'fileType' | 'originalFileName'>;
  size?: 'sm' | 'md' | 'lg';
  /** Renders the branded extension tile instead of a category icon. */
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
  const brand = ext ? EXTENSION_BRANDS[ext] : undefined;

  // Branded tiles are used whenever an extension brand exists — this covers
  // the `showExtension` mode and gives every special file type its colour.
  if (showExtension && brand) {
    return (
      <span
        aria-hidden="true"
        className={cn(
          'grid shrink-0 place-items-center overflow-hidden font-bold tracking-wider uppercase',
          brand.className,
          tileClasses[size],
          extensionClasses[size],
          className,
        )}
      >
        {brand.label.slice(0, 4)}
      </span>
    );
  }

  if (showExtension && ext) {
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
        <span className={cn('font-bold tracking-wider uppercase', extensionClasses[size])}>
          {ext.slice(0, 4)}
        </span>
      </span>
    );
  }

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
      <Icon className={iconClasses[size]} />
    </span>
  );
}
