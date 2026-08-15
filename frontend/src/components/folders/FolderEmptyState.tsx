import { motion } from 'framer-motion';
import { FolderPlus, SearchX } from 'lucide-react';
import type { ReactNode } from 'react';

import { EmptyState as BaseEmptyState } from '@/components/common/EmptyState';
import { Button } from '@/components/ui/Button';

export type FoldersEmptyStateVariant = 'no-folders' | 'no-search';

export interface FoldersEmptyStateProps {
  variant: FoldersEmptyStateVariant;
  searchQuery?: string;
  onCreateFolder?: () => void;
  onClearSearch?: () => void;
}

const CONTENT: Record<
  FoldersEmptyStateVariant,
  { icon: ReactNode; title: string; description: string }
> = {
  'no-folders': {
    icon: <FolderPlus className="h-6 w-6" />,
    title: 'No folders yet',
    description: 'Create your first folder to keep your files organised.',
  },
  'no-search': {
    icon: <SearchX className="h-6 w-6" />,
    title: 'No matches found',
    description: 'Nothing matches your search. Try a different keyword.',
  },
};

/** Empty / no-result state for the folder explorer, tailored per situation. */
export function FoldersEmptyState({
  variant,
  searchQuery,
  onCreateFolder,
  onClearSearch,
}: FoldersEmptyStateProps) {
  const content = CONTENT[variant];

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25, ease: 'easeOut' }}
    >
      <BaseEmptyState
        icon={content.icon}
        title={content.title}
        description={
          variant === 'no-search'
            ? `Nothing matches “${searchQuery?.trim() ?? ''}”. Try a different keyword.`
            : content.description
        }
        action={
          variant === 'no-folders' && onCreateFolder ? (
            <Button
              variant="primary"
              size="sm"
              onClick={onCreateFolder}
              leftIcon={<FolderPlus className="h-3.5 w-3.5" />}
            >
              Create a folder
            </Button>
          ) : variant === 'no-search' && onClearSearch ? (
            <Button variant="outline" size="sm" onClick={onClearSearch}>
              Clear search
            </Button>
          ) : undefined
        }
      />
    </motion.div>
  );
}
