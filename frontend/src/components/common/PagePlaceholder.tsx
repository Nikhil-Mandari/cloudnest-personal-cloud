import type { ReactNode } from 'react';

import { EmptyState } from './EmptyState';
import { PageHeader } from './PageHeader';

export interface PagePlaceholderProps {
  icon: ReactNode;
  title: string;
  description: string;
  action?: ReactNode;
}

/**
 * Standard "under construction" placeholder used by module pages until they are
 * wired to the backend. Keep pages thin; replace this with real UI per module.
 */
export function PagePlaceholder({ icon, title, description, action }: PagePlaceholderProps) {
  return (
    <div className="space-y-6">
      <PageHeader title={title} description={description} actions={action} />
      <EmptyState
        icon={icon}
        title={`${title} is coming soon`}
        description={`The ${title.toLowerCase()} module is under construction and will be connected to the CloudNest API shortly.`}
      />
    </div>
  );
}
