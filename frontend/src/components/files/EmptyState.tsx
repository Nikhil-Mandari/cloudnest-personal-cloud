import { motion } from 'framer-motion';
import { FilePlus2, FolderSearch, SearchX, Star } from 'lucide-react';
import type { ReactNode } from 'react';

import { EmptyState as BaseEmptyState } from '@/components/common/EmptyState';
import { Button } from '@/components/ui/Button';

export type FilesEmptyStateVariant = 'no-files' | 'no-search' | 'no-filter' | 'no-favorites';

export interface FilesEmptyStateProps {
  variant: FilesEmptyStateVariant;
  searchQuery?: string;
  filterLabel?: string;
  onUpload?: () => void;
  onClearFilters?: () => void;
}

const CONTENT: Record<
  FilesEmptyStateVariant,
  { icon: ReactNode; title: string; description: string }
> = {
  'no-files': {
    icon: <FilePlus2 className="h-6 w-6" />,
    title: 'No files yet',
    description: 'Upload your first file — drag & drop it here or use the button above.',
  },
  'no-search': {
    icon: <SearchX className="h-6 w-6" />,
    title: 'No matches found',
    description: 'Nothing matches your search. Try a different keyword.',
  },
  'no-filter': {
    icon: <FolderSearch className="h-6 w-6" />,
    title: 'Nothing here',
    description: 'No files match the current filter.',
  },
  'no-favorites': {
    icon: <Star className="h-6 w-6" />,
    title: 'No favorites yet',
    description: 'Tap the star on any file to keep it close at hand.',
  },
};

/** Empty / no-result state for the explorer, tailored per situation. */
export function FilesEmptyState({
  variant,
  searchQuery,
  filterLabel,
  onUpload,
  onClearFilters,
}: FilesEmptyStateProps) {
  const content = CONTENT[variant];

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25, ease: 'easeOut' }}
    >
      <BaseEmptyState
        icon={content.icon}
        title={variant === 'no-filter' ? `No ${filterLabel ?? 'matching'} files` : content.title}
        description={
          variant === 'no-search'
            ? `Nothing matches “${searchQuery?.trim() ?? ''}”. Try a different keyword.`
            : content.description
        }
        action={
          (variant === 'no-files' && onUpload) || (variant === 'no-search' && onClearFilters) ? (
            <Button
              variant="primary"
              size="sm"
              onClick={variant === 'no-files' ? onUpload : onClearFilters}
              leftIcon={variant === 'no-files' ? <FilePlus2 className="h-3.5 w-3.5" /> : undefined}
            >
              {variant === 'no-files' ? 'Upload a file' : 'Clear search'}
            </Button>
          ) : variant === 'no-filter' && onClearFilters ? (
            <Button variant="outline" size="sm" onClick={onClearFilters}>
              Show all files
            </Button>
          ) : undefined
        }
      />
    </motion.div>
  );
}
