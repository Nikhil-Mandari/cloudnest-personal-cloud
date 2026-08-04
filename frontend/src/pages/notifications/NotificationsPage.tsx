import { Bell } from 'lucide-react';

import { PagePlaceholder } from '@/components/common/PagePlaceholder';

export function NotificationsPage() {
  return (
    <PagePlaceholder
      icon={<Bell className="h-6 w-6" />}
      title="Notifications"
      description="Stay up to date with shares, alerts and account activity."
    />
  );
}
