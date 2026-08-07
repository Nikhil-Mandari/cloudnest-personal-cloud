import { AnimatePresence, motion } from 'framer-motion';

import type { Folder } from '@/types';
import { FolderCard } from './FolderCard';

export interface FolderGridProps {
  folders: Folder[];
  selectedIds: string[];
  ownerName: string;
  /** Opens the folder (navigates into it). */
  onOpen: (folder: Folder) => void;
  /** Toggles selection (hover checkbox). */
  onSelect: (id: string) => void;
  /** When false the selection checkbox is hidden (e.g. Files explorer). */
  selectable?: boolean;
  /** Context-menu trigger; when omitted the "More actions" button is hidden. */
  onOpenMenu?: (folder: Folder, x: number, y: number) => void;
}

/** Responsive animated grid of folder cards. */
export function FolderGrid({
  folders,
  selectedIds,
  ownerName,
  onOpen,
  onSelect,
  selectable = true,
  onOpenMenu,
}: FolderGridProps) {
  return (
    <motion.div
      layout
      className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:gap-4 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6"
    >
      <AnimatePresence mode="popLayout">
        {folders.map((folder) => (
          <FolderCard
            key={folder.id}
            folder={folder}
            selected={selectedIds.includes(folder.id)}
            ownerName={ownerName}
            onOpen={onOpen}
            onSelect={onSelect}
            selectable={selectable}
            onOpenMenu={onOpenMenu}
          />
        ))}
      </AnimatePresence>
    </motion.div>
  );
}
