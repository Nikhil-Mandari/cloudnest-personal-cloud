import { Share2 } from 'lucide-react';

import { PagePlaceholder } from '@/components/common/PagePlaceholder';

export function SharedPage() {
  return (
    <PagePlaceholder
      icon={<Share2 className="h-6 w-6" />}
      title="Shared with you"
      description="Files and folders others have shared with you, in one place."
    />
  );
}
