import { SearchX, Trash2 } from 'lucide-react';

import { EmptyState } from '@/components/common/EmptyState';

export interface TrashEmptyStateProps {
  /** True when the trash has items but none match the search query. */
  variant: 'empty' | 'no-search';
}

/** Empty states for the trash page. */
export function TrashEmptyState({ variant }: TrashEmptyStateProps) {
  return variant === 'no-search' ? (
    <EmptyState
      icon={<SearchX className="h-6 w-6" />}
      title="No matching items"
      description="Nothing in your trash matches that search. Try a different term."
    />
  ) : (
    <EmptyState
      icon={<Trash2 className="h-6 w-6" />}
      title="Trash is empty"
      description="Deleted files and folders stay here until you restore them or empty the trash."
    />
  );
}
