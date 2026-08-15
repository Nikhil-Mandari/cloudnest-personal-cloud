import { motion } from 'framer-motion';
import { Inbox, Link2, SearchX } from 'lucide-react';
import type { ReactNode } from 'react';

import { EmptyState as BaseEmptyState } from '@/components/common/EmptyState';
import { Button } from '@/components/ui/Button';

export type SharesEmptyStateVariant = 'no-shares' | 'no-search' | 'no-links';

export interface SharesEmptyStateProps {
  variant: SharesEmptyStateVariant;
  searchQuery?: string;
  onClearSearch?: () => void;
}

const CONTENT: Record<
  SharesEmptyStateVariant,
  { icon: ReactNode; title: string; description: string }
> = {
  'no-shares': {
    icon: <Inbox className="h-6 w-6" />,
    title: 'Nothing shared with you yet',
    description: 'When someone shares a file or folder with you, it will show up here.',
  },
  'no-links': {
    icon: <Link2 className="h-6 w-6" />,
    title: 'You have not created any links yet',
    description: 'Share a file from My Files to generate a link — then manage it here.',
  },
  'no-search': {
    icon: <SearchX className="h-6 w-6" />,
    title: 'No matches found',
    description: 'Nothing matches your search. Try a different keyword.',
  },
};

/** Empty / no-result state for the shared-with-me view. */
export function SharesEmptyState({
  variant,
  searchQuery,
  onClearSearch,
}: SharesEmptyStateProps) {
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
          variant === 'no-search' && onClearSearch ? (
            <Button variant="outline" size="sm" onClick={onClearSearch}>
              Clear search
            </Button>
          ) : undefined
        }
      />
    </motion.div>
  );
}
