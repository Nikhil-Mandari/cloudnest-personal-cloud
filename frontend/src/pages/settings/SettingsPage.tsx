import { Settings } from 'lucide-react';

import { PagePlaceholder } from '@/components/common/PagePlaceholder';

export function SettingsPage() {
  return (
    <PagePlaceholder
      icon={<Settings className="h-6 w-6" />}
      title="Settings"
      description="Customise CloudNest to suit the way you work."
    />
  );
}
