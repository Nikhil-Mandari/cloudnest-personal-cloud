import { AnimatePresence, motion } from 'framer-motion';

import type { FileItem } from '@/types';
import { FileCard } from './FileCard';

export interface FileGridProps {
  files: FileItem[];
  selectedIds: number[];
  ownerName: string;
  onSelect: (id: number) => void;
  onToggleFavorite: (file: FileItem) => void;
  onDownload: (file: FileItem) => void;
  onOpenMenu: (file: FileItem, x: number, y: number) => void;
}

/** Responsive animated grid of file cards. */
export function FileGrid({
  files,
  selectedIds,
  ownerName,
  onSelect,
  onToggleFavorite,
  onDownload,
  onOpenMenu,
}: FileGridProps) {
  return (
    <motion.div
      layout
      className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:gap-4 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6"
    >
      <AnimatePresence mode="popLayout">
        {files.map((file) => (
          <FileCard
            key={file.id}
            file={file}
            selected={selectedIds.includes(file.id)}
            ownerName={ownerName}
            onSelect={onSelect}
            onToggleFavorite={onToggleFavorite}
            onDownload={onDownload}
            onOpenMenu={onOpenMenu}
          />
        ))}
      </AnimatePresence>
    </motion.div>
  );
}
