import { Trash2 } from 'lucide-react';

import { PagePlaceholder } from '@/components/common/PagePlaceholder';

export function TrashPage() {
  return (
    <PagePlaceholder
      icon={<Trash2 className="h-6 w-6" />}
      title="Trash"
      description="Recover deleted files or empty your trash for good."
    />
  );
}
