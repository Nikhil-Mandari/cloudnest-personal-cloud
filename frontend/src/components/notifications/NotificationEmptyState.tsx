import { BellOff, SearchX } from 'lucide-react';

import { EmptyState } from '@/components/common/EmptyState';

export interface NotificationEmptyStateProps {
  /** True when the current filter (e.g. Unread) simply has no matches. */
  filtered?: boolean;
}

/** Empty states for the notifications list. */
export function NotificationEmptyState({ filtered = false }: NotificationEmptyStateProps) {
  return filtered ? (
    <EmptyState
      icon={<SearchX className="h-6 w-6" />}
<<<<<<< Updated upstream
      title="Nothing here"
      description="No notifications match this filter — try a different one."
=======
      title="Nothing unread"
      description="You're all caught up — no unread notifications match this filter."
>>>>>>> Stashed changes
    />
  ) : (
    <EmptyState
      icon={<BellOff className="h-6 w-6" />}
      title="No notifications yet"
      description="When someone shares a file or folder with you, it will show up here."
    />
  );
}
