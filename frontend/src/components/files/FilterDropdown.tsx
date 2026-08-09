import { useMemo } from 'react';
import {
  Clock,
  File,
  FileArchive,
  FileAudio,
  FileCode,
  FileImage,
  FileText,
  FileType,
  FileVideo,
  Files,
  Share2,
  Star,
  type LucideIcon,
} from 'lucide-react';

import type { FileItem, FileTypeCategory, FileTypeFilter } from '@/types';
import { cn } from '@/utils/cn';
import { getFileTypeCategory, isPdfFile, isRecentFile } from '@/utils/file';
import { DropdownMenu, type DropdownOption } from './DropdownMenu';

const CATEGORY_ICONS: Record<FileTypeCategory, LucideIcon> = {
  image: FileImage,
  video: FileVideo,
  audio: FileAudio,
  document: FileText,
  archive: FileArchive,
  code: FileCode,
  other: File,
};

export interface FilterDropdownProps {
  value: FileTypeFilter;
  onChange: (filter: FileTypeFilter) => void;
  /** Full (unfiltered) file list — used to compute live counts. */
  files: FileItem[];
  /** Ids of files the user has created a share link for (shared filter). */
  sharedFileIds?: ReadonlySet<number>;
}

const CATEGORY_LABELS: Record<FileTypeCategory, string> = {
  image: 'Images',
  video: 'Videos',
  audio: 'Audio',
  document: 'Documents',
  archive: 'Archives',
  code: 'Code',
  other: 'Other',
};

/** Filters the explorer by file type (or favorites / pdf / recent / shared). */
export function FilterDropdown({ value, onChange, files, sharedFileIds }: FilterDropdownProps) {
  const counts = useMemo(() => {
    const map = new Map<FileTypeFilter, number>();
    map.set('all', files.length);
    map.set('favorites', files.filter((file) => file.isFavorite).length);
    map.set('pdf', files.filter((file) => isPdfFile(file)).length);
    map.set('recent', files.filter((file) => isRecentFile(file.createdAt)).length);
    map.set(
      'shared',
      sharedFileIds ? files.filter((file) => sharedFileIds.has(file.id)).length : 0,
    );
    for (const file of files) {
      const category = getFileTypeCategory(file);
      map.set(category, (map.get(category) ?? 0) + 1);
    }
    return map;
  }, [files, sharedFileIds]);

  const options = useMemo<ReadonlyArray<DropdownOption<FileTypeFilter>>>(
    () => [
      { value: 'all', label: 'All files', icon: <Files className="h-4 w-4" /> },
      { value: 'favorites', label: 'Favorites', icon: <Star className="h-4 w-4" /> },
      { value: 'pdf', label: 'PDFs', icon: <FileType className="h-4 w-4" /> },
      { value: 'recent', label: 'Recent', icon: <Clock className="h-4 w-4" /> },
      { value: 'shared', label: 'Shared', icon: <Share2 className="h-4 w-4" /> },
      ...(Object.keys(CATEGORY_ICONS) as FileTypeCategory[]).map((category) => {
        const Icon = CATEGORY_ICONS[category];
        return {
          value: category,
          label: CATEGORY_LABELS[category],
          icon: <Icon className="h-4 w-4" />,
        };
      }),
    ],
    [],
  );

  return (
    <DropdownMenu<FileTypeFilter>
      value={value}
      options={options}
      onChange={onChange}
      label="Filter files"
      optionMeta={(filter) => (
        <span
          className={cn(
            'rounded-md px-1.5 py-0.5 text-[10px] font-semibold tabular-nums',
            filter === value
              ? 'bg-brand-500/15 text-brand-600 dark:text-brand-300'
              : 'bg-gray-100 text-gray-500 dark:bg-gray-800 dark:text-gray-400',
          )}
        >
          {counts.get(filter) ?? 0}
        </span>
      )}
    />
  );
}
