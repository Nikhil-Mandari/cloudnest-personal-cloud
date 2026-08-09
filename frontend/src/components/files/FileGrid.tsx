import type { MouseEvent as ReactMouseEvent } from 'react';
import { AnimatePresence, motion } from 'framer-motion';

import type { FileItem } from '@/types';
import { FileCard } from './FileCard';

export interface FileGridProps {
  files: FileItem[];
  selectedIds: number[];
  ownerName: string;
  /** Active search term — matched text is highlighted in file names. */
  searchQuery?: string;
  /** Ids of freshly uploaded files — their tiles pulse. */
  highlightedIds?: number[];
  onSelect: (file: FileItem, event?: ReactMouseEvent) => void;
  onToggleFavorite: (file: FileItem) => void;
  onDownload: (file: FileItem) => void;
  onOpenMenu: (file: FileItem, x: number, y: number) => void;
  onPreview: (file: FileItem) => void;
}

/** Responsive animated grid of file cards. */
export function FileGrid({
  files,
  selectedIds,
  ownerName,
  searchQuery,
  highlightedIds = [],
  onSelect,
  onToggleFavorite,
  onDownload,
  onOpenMenu,
  onPreview,
}: FileGridProps) {
  return (
    <motion.div
      layout
      className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 md:gap-3.5 lg:grid-cols-5 xl:grid-cols-6 2xl:grid-cols-7"
    >
      <AnimatePresence mode="popLayout">
        {files.map((file) => (
          <FileCard
            key={file.id}
            file={file}
            selected={selectedIds.includes(file.id)}
            ownerName={ownerName}
            searchQuery={searchQuery}
            highlighted={highlightedIds.includes(file.id)}
            onSelect={onSelect}
            onToggleFavorite={onToggleFavorite}
            onDownload={onDownload}
            onOpenMenu={onOpenMenu}
            onPreview={onPreview}
          />
        ))}
      </AnimatePresence>
    </motion.div>
  );
}
