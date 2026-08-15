import { useMemo, useState } from 'react';
import { ArrowDown, ArrowUp, Clock, Eye, FileText, MousePointerClick } from 'lucide-react';
import { toast } from 'react-toastify';

import { ErrorState } from '@/components/common/ErrorState';
import { PageHeader } from '@/components/common/PageHeader';
import { DropdownMenu, type DropdownOption } from '@/components/files/DropdownMenu';
import { FilesSearchBar } from '@/components/files/SearchBar';
import { MySharesTable } from '@/components/shares/MySharesTable';
import { SharesEmptyState } from '@/components/shares/ShareEmptyState';
import { ShareSettingsDialog } from '@/components/shares/ShareSettingsDialog';
import { ShareTableSkeleton } from '@/components/shares/ShareSkeletons';
import { ConfirmationDialog } from '@/components/ui/ConfirmationDialog';
import { useMySharesQuery, useRevokeShareMutation } from '@/hooks/useShare';
import type { MySharesSortKey, ShareRecord, SortDirection } from '@/types';
import { cn } from '@/utils/cn';
import { copyToClipboard } from '@/utils/download';
import { getErrorMessage } from '@/utils/error';
import { buildShareUrl } from '@/utils/file';
import { filterMyShares, sortMyShares } from '@/utils/share';

const SORT_OPTIONS: ReadonlyArray<DropdownOption<MySharesSortKey>> = [
  { value: 'date', label: 'Date created', icon: <Clock className="h-4 w-4" /> },
  { value: 'name', label: 'Resource name', icon: <FileText className="h-4 w-4" /> },
  { value: 'views', label: 'Views', icon: <Eye className="h-4 w-4" /> },
  { value: 'downloads', label: 'Downloads', icon: <MousePointerClick className="h-4 w-4" /> },
];

/** Management page for the user's own share links (analytics + settings). */
export function MySharesPage() {
  const { data: shares = [], isLoading, isError, error, refetch } = useMySharesQuery();
  const revokeMutation = useRevokeShareMutation();

  const [searchQuery, setSearchQuery] = useState('');
  const [sortKey, setSortKey] = useState<MySharesSortKey>('date');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');

  // Dialogs.
  const [settingsShare, setSettingsShare] = useState<ShareRecord | null>(null);
  const [revokeTarget, setRevokeTarget] = useState<ShareRecord | null>(null);

  const visibleShares = useMemo(
    () => sortMyShares(filterMyShares(shares, searchQuery), sortKey, sortDirection),
    [shares, searchQuery, sortKey, sortDirection],
  );

  const handleCopyLink = async (share: ShareRecord) => {
    const ok = await copyToClipboard(buildShareUrl(share.shareToken));
    if (ok) {
      toast.success('Link copied to clipboard');
    } else {
      toast.error('Could not copy the link');
    }
  };

  const confirmRevoke = () => {
    if (revokeTarget) {
      revokeMutation.mutate(revokeTarget.id);
      setRevokeTarget(null);
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="My Shares"
        description="Every link you created, with its access analytics and settings."
      />

      {/* Toolbar */}
      <div className="space-y-3">
        <div className="flex flex-wrap items-center gap-2">
          <FilesSearchBar
            value={searchQuery}
            onChange={setSearchQuery}
            placeholder="Search your links…"
            className="min-w-40 flex-1"
          />

          <div className="ml-auto flex flex-wrap items-center gap-2">
            <DropdownMenu<MySharesSortKey>
              value={sortKey}
              options={SORT_OPTIONS}
              onChange={setSortKey}
              label="Sort links"
            />

            <button
              type="button"
              onClick={() =>
                setSortDirection((direction) => (direction === 'asc' ? 'desc' : 'asc'))
              }
              aria-label={`Sort ${sortDirection === 'asc' ? 'descending' : 'ascending'}`}
              title={`Sort ${sortDirection === 'asc' ? 'descending' : 'ascending'}`}
              className={cn(
                'grid h-10 w-10 place-items-center rounded-lg border border-gray-300 bg-white text-gray-600 shadow-sm transition-colors',
                'focus-visible:ring-brand-500/50 hover:bg-gray-50 focus-visible:ring-2 focus-visible:outline-none',
                'dark:border-gray-700 dark:bg-gray-900 dark:text-gray-300 dark:hover:bg-gray-800/70',
              )}
            >
              {sortDirection === 'asc' ? (
                <ArrowUp className="h-4 w-4" />
              ) : (
                <ArrowDown className="h-4 w-4" />
              )}
            </button>
          </div>
        </div>

        <p className="text-xs text-gray-400 dark:text-gray-500">
          {visibleShares.length} link{visibleShares.length === 1 ? '' : 's'}
          {searchQuery.trim() && ` · matching “${searchQuery.trim()}”`}
        </p>
      </div>

      {/* Content */}
      {isLoading ? (
        <ShareTableSkeleton />
      ) : isError ? (
        <ErrorState
          message={getErrorMessage(error, 'Failed to load your share links.')}
          onRetry={() => void refetch()}
        />
      ) : shares.length === 0 ? (
        <SharesEmptyState variant="no-links" />
      ) : visibleShares.length === 0 ? (
        <SharesEmptyState variant="no-search" searchQuery={searchQuery} />
      ) : (
        <MySharesTable
          shares={visibleShares}
          onCopyLink={(share) => void handleCopyLink(share)}
          onOpenSettings={setSettingsShare}
          onRevoke={setRevokeTarget}
        />
      )}

      {/* Dialogs */}
      <ShareSettingsDialog
        share={settingsShare}
        open={settingsShare !== null}
        onClose={() => setSettingsShare(null)}
      />
      <ConfirmationDialog
        open={revokeTarget !== null}
        onClose={() => setRevokeTarget(null)}
        onConfirm={confirmRevoke}
        title="Revoke this link?"
        description={`“${revokeTarget?.resourceName ?? 'This share'}” will stop being accessible through the link. Existing access in CloudNest is unaffected.`}
        confirmLabel="Revoke link"
        isLoading={revokeMutation.isPending}
      />
    </div>
  );
}
