import { User } from 'lucide-react';

import { PagePlaceholder } from '@/components/common/PagePlaceholder';

export function ProfilePage() {
  return (
    <PagePlaceholder
      icon={<User className="h-6 w-6" />}
      title="Profile"
      description="View and edit your personal information and preferences."
    />
  );
}
