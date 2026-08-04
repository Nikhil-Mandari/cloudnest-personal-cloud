import { FolderOpen } from 'lucide-react';

import { PagePlaceholder } from '@/components/common/PagePlaceholder';

export function FoldersPage() {
  return (
    <PagePlaceholder
      icon={<FolderOpen className="h-6 w-6" />}
      title="Folders"
      description="Keep your files tidy with folders and sub-folders."
    />
  );
}
